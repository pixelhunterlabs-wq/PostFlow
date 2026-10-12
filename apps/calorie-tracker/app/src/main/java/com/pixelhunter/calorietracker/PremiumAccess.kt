package com.pixelhunter.calorietracker

import android.content.Intent
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal object PremiumAccess {
    suspend fun request(endpoint: String = "premium-access", body: JSONObject = JSONObject()): JSONObject = withContext(Dispatchers.IO) {
        val token = SupabaseProvider.client?.auth?.currentSessionOrNull()?.accessToken ?: error("Önce giriş yapmalısın.")
        val connection = URL(BuildConfig.SUPABASE_URL + "/functions/v1/" + endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $token")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            val result = JSONObject(text.ifBlank { "{}" })
            if (status !in 200..299) error(when (result.optString("error")) {
                "premium_required" -> "Bu özellik Premium üyelik gerektirir."
                "billing_not_configured" -> "Abonelik doğrulaması henüz yapılandırılmadı."
                "authentication_required" -> "Oturumunu yenileyip tekrar dene."
                else -> "Üyelik doğrulanamadı. İnternet bağlantını kontrol edip tekrar dene."
            })
            result
        } finally { connection.disconnect() }
    }
    suspend fun requireFeature(feature: String) = request(body = JSONObject().put("feature", feature))
    suspend fun report(range: String = "MONTH", target: Double = 0.25): PremiumReport =
        Json { ignoreUnknownKeys = true }.decodeFromString(request("premium-reports", JSONObject().put("range",range).put("targetWeeklyLoss",target)).toString())
}

@Serializable internal data class PremiumChartPoint(val date: String, val calories: Double)
@Serializable internal data class PremiumGoal(val currentGoal: Int, val suggestedGoal: Int, val observedWeeklyChangeKg: Double, val targetWeeklyLossKg: Double)
@Serializable internal data class PremiumReport(val chart: List<PremiumChartPoint>, val average: Int, val adherence: Int, val protein: Double, val carbs: Double, val fat: Double, val recordedDays: Int, val weights: List<WeightEntry>, val suggestion: PremiumGoal? = null)

@Composable
internal fun PremiumGate(feature: String, content: @Composable () -> Unit) {
    var allowed by remember(feature) { mutableStateOf(false) }
    var message by remember(feature) { mutableStateOf("Üyelik kontrol ediliyor…") }
    var retry by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    LaunchedEffect(feature, retry) {
        allowed = false
        runCatching { PremiumAccess.requireFeature(feature) }.onSuccess { allowed = true }.onFailure { message = it.message ?: "Üyelik doğrulanamadı." }
    }
    if (allowed) content() else Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = KaloriMuted)
        Button(onClick = { context.startActivity(Intent(context, PremiumPlansActivity::class.java)) }) { Text("Ücretsiz ve Premium") }
        OutlinedButton(onClick = { retry++ }) { Text("Yeniden kontrol et") }
    }
}
