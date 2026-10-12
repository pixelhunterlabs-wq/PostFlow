package com.pixelhunter.calorietracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Public comparison of the approved Free and Premium plans.
 *
 * The paid checkout must only be enabled after Google Play products, server-side
 * purchase verification and entitlement enforcement are deployed and tested.
 * Displaying the plans must never grant a Premium entitlement.
 */
class PremiumPlansActivity : ComponentActivity() {
    private lateinit var billing: GooglePlaySubscriptionBilling
    private var offers by mutableStateOf<Map<String, PlayPlanOffer>>(emptyMap())
    private var message by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        billing = GooglePlaySubscriptionBilling(
            activity = this,
            scope = lifecycleScope,
            onPlans = { offers = it },
            onMessage = { message = it }
        )
        setContent {
            KaloriDarkTheme {
                PremiumPlansScreen(
                    offers = offers,
                    message = message,
                    onBuy = billing::purchase,
                    onManage = billing::openSubscriptionManagement
                )
            }
        }
        billing.start()
    }

    override fun onResume() {
        super.onResume()
        if (::billing.isInitialized) billing.restore()
    }

    override fun onDestroy() {
        if (::billing.isInitialized) billing.close()
        super.onDestroy()
    }
}

private data class PlanFeature(val title: String, val free: String, val premium: String)

private val planFeatures = listOf(
    PlanFeature("Kalori ve makro takibi", "✓", "✓"),
    PlanFeature("Manuel yemek ekleme", "✓", "✓"),
    PlanFeature("Su ve kilo takibi", "✓", "✓"),
    PlanFeature("Barkod okutma", "✓", "✓"),
    PlanFeature("Fotoğrafla AI analiz", "5 / ay", "100 / ay"),
    PlanFeature("Sesle çoklu yemek ekleme", "—", "✓"),
    PlanFeature("Gelişmiş kilo grafikleri", "—", "✓"),
    PlanFeature("Haftalık ve aylık analizler", "—", "✓"),
    PlanFeature("Akıllı kalori hedefi önerileri", "—", "✓")
)

@Composable
private fun PremiumPlansScreen(
    offers: Map<String, PlayPlanOffer>,
    message: String?,
    onBuy: (String) -> Unit,
    onManage: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(KaloriBackground),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 32.dp, bottom = 44.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Sana uygun planı seç", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = KaloriText)
            Spacer(Modifier.height(6.dp))
            Text("Ücretsiz ve Premium özellikleri karşılaştır.", color = KaloriMuted)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PlanPriceCard("ÜCRETSİZ", "0 TL", "Temel takip", false, Modifier.weight(1f))
                PlanPriceCard("PREMIUM", offers[PremiumBillingConfig.MONTHLY]?.formattedPrice ?: "99,99 TL*", "Aylık abonelik", true, Modifier.weight(1f))
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = KaloriSurfaceAlt),
                border = BorderStroke(1.dp, KaloriGreen)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("YILLIK PREMIUM", color = KaloriGreen, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(4.dp))
                    Text((offers[PremiumBillingConfig.YEARLY]?.formattedPrice ?: "699,99 TL*") + " / yıl", color = KaloriText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Türkiye için önerilen yıllık fiyat • Kesin fiyat Google Play'de gösterilir.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = KaloriSurface),
                border = BorderStroke(1.dp, KaloriBorder)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Özellik", Modifier.weight(1.8f), fontWeight = FontWeight.Bold, color = KaloriText)
                        Text("Ücretsiz", Modifier.weight(1f), textAlign = TextAlign.Center, color = KaloriMuted, fontWeight = FontWeight.Bold)
                        Text("Premium", Modifier.weight(1f), textAlign = TextAlign.Center, color = KaloriGreen, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = KaloriBorder)
                    planFeatures.forEach { feature ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(feature.title, Modifier.weight(1.8f), color = KaloriText, style = MaterialTheme.typography.bodySmall)
                            Text(feature.free, Modifier.weight(1f), textAlign = TextAlign.Center, color = KaloriMuted)
                            Text(feature.premium, Modifier.weight(1f), textAlign = TextAlign.Center, color = KaloriGreen, fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = KaloriBorder)
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onBuy(PremiumBillingConfig.MONTHLY) },
                    enabled = PremiumBillingConfig.SALES_ENABLED && offers.containsKey(PremiumBillingConfig.MONTHLY),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Aylık Premium'a Geç") }
                Button(
                    onClick = { onBuy(PremiumBillingConfig.YEARLY) },
                    enabled = PremiumBillingConfig.SALES_ENABLED && offers.containsKey(PremiumBillingConfig.YEARLY),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Yıllık Premium'a Geç") }
                OutlinedButton(onClick = onManage, modifier = Modifier.fillMaxWidth()) {
                    Text("Google Play'de Aboneliğimi Yönet")
                }
                message?.let { Text(it, color = KaloriGreen, style = MaterialTheme.typography.bodySmall) }
                if (!PremiumBillingConfig.SALES_ENABLED) {
                    Text("Abonelikler hazırlık aşamasında. Satın alma butonları ödeme doğrulaması tamamlanana kadar kapalıdır.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                }
                Text("* Önerilen Türkiye fiyatlarıdır; geçerli fiyat Google Play'den alınır. Abonelikler otomatik yenilenir; Google Play üzerinden iptal edilebilir.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlanPriceCard(title: String, price: String, subtitle: String, highlighted: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (highlighted) Color(0xFF153528) else KaloriSurface),
        border = BorderStroke(1.dp, if (highlighted) KaloriGreen else KaloriBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = if (highlighted) KaloriGreen else KaloriMuted, fontWeight = FontWeight.Bold)
            Text(price, color = KaloriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}
