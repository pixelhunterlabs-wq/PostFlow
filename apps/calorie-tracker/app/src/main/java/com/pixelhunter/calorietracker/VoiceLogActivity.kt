package com.pixelhunter.calorietracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

class VoiceLogActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KaloriDarkTheme { VoiceLogScreen() } }
    }
}

@Composable
private fun VoiceLogScreen(vm: TrackerViewModel = viewModel()) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var transcript by remember { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<ParsedVoiceFood>>(emptyList()) }
    var mealType by remember { mutableStateOf("Öğün") }
    var status by remember { mutableStateOf("Mikrofona dokun ve yediğini söyle") }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
        if (text.isNotBlank()) {
            transcript = text
            parsed = parseVoiceFoods(text)
            status = if (parsed.isEmpty()) "Tanıdığım bir yiyecek bulunamadı; daha kısa ve gram belirterek tekrar dene" else "${parsed.size} yiyecek bulundu"
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            status = "Bu cihazda konuşma tanıma hizmeti kullanılamıyor"
            return
        }
        speechLauncher.launch(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.forLanguageTag("tr-TR").toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Örn: 150 gram tavuk ve 200 gram pilav")
            }
        )
    }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startListening() else status = "Mikrofon izni verilmedi"
    }

    Scaffold(
        containerColor = KaloriBackground,
        topBar = {
            TopAppBar(
                title = { Text("Sesle Yemek Ekle", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaloriBackground, titleContentColor = KaloriText)
            )
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Mic, null, tint = KaloriGreen, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("“150 gram tavuk, 200 gram pilav ve 1 bardak ayran”", color = KaloriMuted)
                        Spacer(Modifier.height(14.dp))
                        Button(onClick = {
                            if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startListening()
                            else micPermission.launch(Manifest.permission.RECORD_AUDIO)
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Mic, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Konuşmaya başla")
                        }
                        Text(status, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (transcript.isNotBlank()) item {
                Card(colors = CardDefaults.cardColors(containerColor = KaloriSurfaceAlt)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Duyulan", color = KaloriMuted, style = MaterialTheme.typography.labelMedium)
                        Text(transcript, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (parsed.isNotEmpty()) {
                item {
                    Text("Öğün", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık").forEach { option ->
                            FilterChip(selected = mealType == option, onClick = { mealType = option }, label = { Text(option) })
                        }
                    }
                }
                items(parsed, key = { it.originalSegment }) { item ->
                    val ratio = item.grams / 100.0
                    Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Restaurant, null, tint = KaloriGreen)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.food.name, fontWeight = FontWeight.Bold)
                                Text("${item.grams.toInt()} g • ${(item.food.calories100g * ratio).toInt()} kcal", color = KaloriMuted)
                                item.portionNote?.let { Text(it, color = KaloriGreen, style = MaterialTheme.typography.labelSmall) }
                                if (item.needsPortionReview) Text("Porsiyon miktarı kontrol edilmeli", color = KaloriYellow, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = {
                            parsed.forEach { item ->
                                val ratio = item.grams / 100.0
                                vm.addFood(
                                    item.food.name,
                                    mealType,
                                    item.grams,
                                    item.food.calories100g * ratio,
                                    item.food.protein100g * ratio,
                                    item.food.carbs100g * ratio,
                                    item.food.fat100g * ratio,
                                    "voice"
                                )
                            }
                            status = "${parsed.size} kayıt günlüğe eklendi"
                            parsed = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                    ) { Text("Tümünü günlüğe ekle", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}
