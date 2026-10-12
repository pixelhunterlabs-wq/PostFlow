package com.pixelhunter.calorietracker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Product and base-plan IDs must exactly match Google Play Console. */
internal object PremiumBillingConfig {
    const val PRODUCT_ID = "kalori_premium"
    const val MONTHLY = "monthly"
    const val YEARLY = "yearly"

    // Release gate: do not accept payments before the service account, Play Console
    // subscription and verified server entitlements are fully tested.
    const val SALES_ENABLED = false
}

internal data class PlayPlanOffer(
    val details: ProductDetails,
    val offerToken: String,
    val basePlanId: String,
    val formattedPrice: String
)

internal class GooglePlaySubscriptionBilling(
    private val activity: Activity,
    private val scope: CoroutineScope,
    private val onPlans: (Map<String, PlayPlanOffer>) -> Unit,
    private val onMessage: (String) -> Unit
) : PurchasesUpdatedListener {
    private var plans: Map<String, PlayPlanOffer> = emptyMap()
    private val client: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    fun start() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                onMessage("Google Play bağlantısı kesildi. Daha sonra tekrar deneyin.")
            }
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryPlans()
                    restore()
                } else {
                    onMessage("Google Play abonelik bilgileri şu anda alınamıyor.")
                }
            }
        })
    }

    private fun queryPlans() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(PremiumBillingConfig.PRODUCT_ID)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            )
        ).build()
        client.queryProductDetailsAsync(params) { result, query ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val offers = mutableMapOf<String, PlayPlanOffer>()
            query.productDetailsList.forEach { product ->
                product.subscriptionOfferDetailsList.orEmpty().forEach { offer ->
                    val planId = offer.basePlanId
                    if (planId !in setOf(PremiumBillingConfig.MONTHLY, PremiumBillingConfig.YEARLY)) return@forEach
                    // Only base plans; do not accidentally show an introductory price as renewal price.
                    if (offer.offerId != null) return@forEach
                    val recurring = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice ?: return@forEach
                    offers[planId] = PlayPlanOffer(product, offer.offerToken, planId, recurring)
                }
            }
            plans = offers
            onPlans(offers)
        }
    }

    fun purchase(basePlanId: String) {
        if (!PremiumBillingConfig.SALES_ENABLED) {
            onMessage("Abonelik ödemeleri henüz açılmadı.")
            return
        }
        val selected = plans[basePlanId] ?: run {
            onMessage("Seçilen abonelik Google Play'de henüz kullanılamıyor.")
            return
        }
        val userId = SupabaseProvider.client?.auth?.currentSessionOrNull()?.user?.id ?: run {
            onMessage("Önce Google hesabınla giriş yapmalısın.")
            return
        }
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(selected.details)
            .setOfferToken(selected.offerToken)
            .build()
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(sha256Hex(userId))
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onMessage("Satın alma ekranı açılamadı: ${result.debugMessage}")
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> purchases.orEmpty().forEach(::processPurchase)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> onMessage("Google Play işlemi tamamlanamadı: ${result.debugMessage}")
        }
    }

    fun restore() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) purchases.forEach(::processPurchase)
        }
    }

    private fun processPurchase(purchase: Purchase) {
        if (PremiumBillingConfig.PRODUCT_ID !in purchase.products) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> onMessage("Ödeme beklemede. Premium erişim henüz açılmadı.")
            Purchase.PurchaseState.PURCHASED -> scope.launch {
                val result = withContext(Dispatchers.IO) { verifyOnServer(purchase.purchaseToken) }
                onMessage(result)
            }
        }
    }

    private fun verifyOnServer(token: String): String {
        val jwt = SupabaseProvider.client?.auth?.currentSessionOrNull()?.accessToken
            ?: return "Google oturumunu yenileyip tekrar deneyin."
        return try {
            val connection = URL(BuildConfig.SUPABASE_URL + "/functions/v1/verify-google-play-subscription")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
                connection.setRequestProperty("Authorization", "Bearer $jwt")
                connection.outputStream.use { it.write(JSONObject().put("purchaseToken", token).toString().toByteArray()) }
                val status = connection.responseCode
                val response = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: "{}"
                val body = JSONObject(response)
                when {
                    status == 200 && body.optBoolean("premium") -> "Premium aboneliğin doğrulandı."
                    body.optString("error") == "billing_not_configured" -> "Ödeme doğrulama sistemi henüz etkin değil."
                    else -> "Abonelik doğrulanamadı. Satın alımı geri yüklemeyi deneyin."
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            "Abonelik doğrulanamadı. İnternet bağlantını kontrol edip tekrar dene."
        }
    }

    fun openSubscriptionManagement() {
        val uri = Uri.parse("https://play.google.com/store/account/subscriptions?sku=" +
            PremiumBillingConfig.PRODUCT_ID + "&package=" + activity.packageName)
        activity.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    fun close() = client.endConnection()

    private fun sha256Hex(input: String): String = MessageDigest.getInstance("SHA-256")
        .digest(input.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
