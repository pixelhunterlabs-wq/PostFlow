package com.pixelhunter.calorietracker

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class AiMealItem(
    val name: String,
    val grams: Double,
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    val confidence: Double = 0.0
)

@Serializable
data class AiMealTotal(
    val grams: Double = 0.0,
    val calories: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0
)

@Serializable
data class AiMealAnalysis(
    val items: List<AiMealItem> = emptyList(),
    val total: AiMealTotal = AiMealTotal(),
    val note: String = ""
)

@Serializable
private data class AiPhotoRequest(val imageBase64: String, val mimeType: String)

class PhotoMealAnalysisActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KaloriDarkTheme { PhotoMealAnalysisScreen() } }
    }
}

@Composable
private fun PhotoMealAnalysisScreen(vm: TrackerViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var analysis by remember { mutableStateOf<AiMealAnalysis?>(null) }
    var loading by remember { mutableStateOf(false) }
    var mealType by remember { mutableStateOf("Öğle") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedUri = uri
        pendingCameraUri = null
        analysis = null
    }

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        if (saved) {
            selectedUri = pendingCameraUri
            analysis = null
        } else {
            // FileProvider exposes a cache file; a failed camera result must not crash
            // the UI while its cache entry is being cleaned up by Android.
            pendingCameraUri?.let { uri -> runCatching { context.contentResolver.delete(uri, null, null) } }
            pendingCameraUri = null
        }
    }

    Scaffold(
        containerColor = KaloriBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Fotoğraftan AI Analiz", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaloriBackground, titleContentColor = KaloriText)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.PhotoCamera, null, tint = KaloriGreen, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Yemek fotoğrafını analiz et", fontWeight = FontWeight.Bold)
                                Text("Kamerayla çek veya galeriden seç. Porsiyon ve besin değerleri yaklaşık tahmindir.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        File.createTempFile("meal_", ".jpg", context.cacheDir)
                                    )
                                    pendingCameraUri = uri
                                    camera.launch(uri)
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                            ) {
                                Icon(Icons.Filled.PhotoCamera, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Kamera ile Çek", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { picker.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Image, null)
                                Spacer(Modifier.width(6.dp))
                                Text("Galeriden Seç")
                            }
                        }

                        if (selectedUri != null) {
                            Text(
                                if (pendingCameraUri == selectedUri) "✓ Kamera fotoğrafı hazır" else "✓ Galeriden fotoğraf seçildi",
                                color = KaloriGreen,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            enabled = selectedUri != null && !loading,
                            onClick = {
                                scope.launch {
                                    loading = true
                                    runCatching {
                                        selectedUri?.let { analyzePhoto(context, it) }
                                            ?: error("Önce fotoğraf çek veya seç")
                                    }
                                        .onSuccess { analysis = it }
                                        .onFailure { snackbar.showSnackbar(it.message ?: "Fotoğraf analiz edilemedi") }
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                        ) {
                            if (loading) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                                Spacer(Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Filled.AutoAwesome, null)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (loading) "Analiz ediliyor" else "AI ile analiz et")
                        }
                    }
                }
            }

            analysis?.let { result ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF0E2A1D)), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Tahmini toplam", color = KaloriMuted)
                            Text("${result.total.calories.toInt()} kcal", style = MaterialTheme.typography.headlineMedium, color = KaloriGreen, fontWeight = FontWeight.Black)
                            Text("${result.total.grams.toInt()} g • P ${result.total.proteinG.toInt()} • K ${result.total.carbsG.toInt()} • Y ${result.total.fatG.toInt()}")
                            if (result.note.isNotBlank()) Text(result.note, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item { Text("Tespit edilen yiyecekler", fontWeight = FontWeight.Bold) }
                items(result.items) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = KaloriSurfaceAlt)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.name, fontWeight = FontWeight.Bold)
                                Text("${item.calories.toInt()} kcal", color = KaloriGreen)
                            }
                            Text("${item.grams.toInt()} g • P ${item.proteinG.toInt()} • K ${item.carbsG.toInt()} • Y ${item.fatG.toInt()}", color = KaloriMuted)
                            Text("Güven ${(item.confidence * 100).toInt()}%", color = KaloriMuted, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                item {
                    Text("Günlüğe eklenecek öğün", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık").forEach { type ->
                            FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type) })
                        }
                    }
                }

                item {
                    Button(
                        enabled = result.items.isNotEmpty(),
                        onClick = {
                            result.items.forEach { food ->
                                vm.addFood(food.name, mealType, food.grams, food.calories, food.proteinG, food.carbsG, food.fatG, "ai_photo")
                            }
                            scope.launch { snackbar.showSnackbar("${result.items.size} yiyecek $mealType öğününe eklendi") }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Filled.AddTask, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Tümünü günlüğe ekle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private suspend fun analyzePhoto(context: android.content.Context, uri: Uri): AiMealAnalysis = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Fotoğraf okunamadı")
    val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
    analyzePhotoBytes(bytes, mimeType)
}

private suspend fun analyzePhotoBytes(bytes: ByteArray, mimeType: String): AiMealAnalysis = withContext(Dispatchers.IO) {
    val client = SupabaseProvider.client ?: error("Supabase bağlantısı yok")
    val token = client.auth.currentSessionOrNull()?.accessToken ?: error("Önce giriş yapmalısın")
    if (bytes.size > 5_500_000) error("Fotoğraf çok büyük. Daha küçük bir fotoğraf seç.")
    val body = Json.encodeToString(AiPhotoRequest(Base64.encodeToString(bytes, Base64.NO_WRAP), mimeType))

    val connection = (URL("${BuildConfig.SUPABASE_URL}/functions/v1/analyze-food-photo").openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 25_000
        readTimeout = 70_000
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Authorization", "Bearer $token")
        setRequestProperty("apikey", BuildConfig.SUPABASE_KEY)
    }
    try {
        connection.outputStream.use { it.write(body.toByteArray()) }
        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (responseCode == 503 && text.contains("ai_not_configured")) {
            error("AI fotoğraf analizi için sunucu anahtarı henüz yapılandırılmadı")
        }
        if (responseCode !in 200..299) error("Fotoğraf analizi başarısız ($responseCode)")
        Json { ignoreUnknownKeys = true }.decodeFromString<AiMealAnalysis>(text)
    } finally {
        connection.disconnect()
    }
}
