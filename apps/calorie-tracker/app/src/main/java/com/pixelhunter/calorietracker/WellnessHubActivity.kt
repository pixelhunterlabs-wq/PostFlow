package com.pixelhunter.calorietracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.util.UUID

class WellnessHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { KaloriDarkTheme { WellnessHubScreen() } }
    }
}

@Composable
private fun WellnessHubScreen(vm: TrackerViewModel = viewModel()) {
    val context = LocalContext.current
    val state = vm.uiState
    val health = remember(context) { HealthConnectBridge(context) }
    val store = remember(context) { AdvancedWellnessStore(context) }
    val scope = rememberCoroutineScope()

    var steps by remember { mutableLongStateOf(0L) }
    var healthGranted by remember { mutableStateOf(false) }
    var reminders by remember { mutableStateOf(store.remindersEnabled()) }
    var weeklyTarget by remember { mutableDoubleStateOf(store.targetWeeklyLossKg()) }
    var savedMeals by remember { mutableStateOf(store.savedMeals()) }
    var showMealDialog by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        healthGranted = granted.containsAll(HealthConnectBridge.stepPermissions)
        if (healthGranted) {
            scope.launch { steps = health.todaySteps() }
        } else message = "Adım izni verilmedi"
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            store.setRemindersEnabled(true)
            reminders = true
            MealReminderScheduler.enable(context)
            message = "Öğün hatırlatmaları açıldı"
        } else {
            reminders = false
            store.setRemindersEnabled(false)
            message = "Bildirim izni verilmedi"
        }
    }

    LaunchedEffect(Unit) {
        if (health.available) {
            healthGranted = health.hasStepPermission()
            if (healthGranted) steps = health.todaySteps()
        }
    }

    val suggestion = remember(state.weights, state.calorieGoal, weeklyTarget) {
        dynamicGoalSuggestion(state.calorieGoal, state.weights, weeklyTarget)
    }

    Scaffold(
        containerColor = KaloriBackground,
        topBar = {
            TopAppBar(
                title = { Text("Sağlık & Akıllı Takip", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaloriBackground, titleContentColor = KaloriText)
            )
        },
        snackbarHost = {
            val host = remember { SnackbarHostState() }
            LaunchedEffect(message) {
                message?.let { host.showSnackbar(it); message = null }
            }
            SnackbarHost(host)
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HubCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DirectionsWalk, null, tint = KaloriGreen, modifier = Modifier.size(34.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Health Connect", fontWeight = FontWeight.Bold)
                            when {
                                !health.available -> Text(
                                    if (health.sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) "Health Connect güncellemesi gerekiyor" else "Bu cihazda Health Connect kullanılamıyor",
                                    color = KaloriMuted
                                )
                                healthGranted -> Text("Bugün $steps adım", color = KaloriGreen, style = MaterialTheme.typography.titleMedium)
                                else -> Text("Adım verisini bağla", color = KaloriMuted)
                            }
                        }
                        if (health.available) {
                            Button(onClick = {
                                if (healthGranted) scope.launch { steps = health.todaySteps() }
                                else healthPermissionLauncher.launch(HealthConnectBridge.stepPermissions)
                            }) { Text(if (healthGranted) "Yenile" else "Bağla") }
                        }
                    }
                }
            }

            item {
                HubCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = KaloriYellow)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Öğün hatırlatmaları", fontWeight = FontWeight.Bold)
                            Text("08:00 • 13:00 • 19:00", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = reminders, onCheckedChange = { enabled ->
                            if (!enabled) {
                                store.setRemindersEnabled(false)
                                reminders = false
                                MealReminderScheduler.disable(context)
                            } else if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                store.setRemindersEnabled(true)
                                reminders = true
                                MealReminderScheduler.enable(context)
                            }
                        })
                    }
                }
            }

            item {
                HubCard {
                    Text("Dinamik kalori hedefi", fontWeight = FontWeight.Bold)
                    Text("Kilo trendine göre hedefi haftalık olarak ayarlamak için öneri üretir.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(0.0, 0.25, 0.5, 0.75).forEach { value ->
                            FilterChip(
                                selected = weeklyTarget == value,
                                onClick = { weeklyTarget = value; store.setTargetWeeklyLossKg(value) },
                                label = { Text(if (value == 0.0) "Koru" else "−$value kg") }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    if (suggestion == null) {
                        Text("Öneri için en az iki farklı tarihte kilo kaydı gerekli.", color = KaloriMuted)
                    } else {
                        Text("Gözlenen haftalık değişim: ${"%.2f".format(suggestion.observedWeeklyChangeKg)} kg", color = KaloriMuted)
                        Text("Mevcut: ${suggestion.currentGoal} kcal  →  Öneri: ${suggestion.suggestedGoal} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
                        if (suggestion.suggestedGoal != suggestion.currentGoal) {
                            Button(onClick = { vm.updateGoal(suggestion.suggestedGoal); message = "Yeni kalori hedefi uygulandı" }) { Text("Öneriyi uygula") }
                        } else {
                            Text("Mevcut hedef kilo trendinle uyumlu.", color = KaloriGreen)
                        }
                    }
                }
            }

            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kayıtlı öğünler", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Sık yediğin öğünleri tek dokunuşla günlüğe ekle", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    FilledTonalButton(onClick = { showMealDialog = true }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Kaydet")
                    }
                }
            }

            if (savedMeals.isEmpty()) {
                item { HubCard { Text("Henüz kayıtlı öğün yok.", color = KaloriMuted) } }
            } else {
                items(savedMeals, key = { it.id }) { meal ->
                    HubCard {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(meal.name, fontWeight = FontWeight.Bold)
                                Text("${meal.calories.toInt()} kcal • P ${meal.proteinG.toInt()} • K ${meal.carbsG.toInt()} • Y ${meal.fatG.toInt()}", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { savedMeals = store.deleteMeal(meal.id) }) {
                                Icon(Icons.Filled.DeleteOutline, "Sil", tint = KaloriDanger)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık").forEach { mealType ->
                                AssistChip(
                                    onClick = {
                                        vm.addFood(meal.name, mealType, meal.grams, meal.calories, meal.proteinG, meal.carbsG, meal.fatG, "saved_meal")
                                        message = "$mealType öğününe eklendi"
                                    },
                                    label = { Text(mealType) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.PrivacyTip, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Sağlık verileri ve gizlilik")
                }
            }
        }
    }

    if (showMealDialog) {
        SavedMealDialog(
            onDismiss = { showMealDialog = false },
            onSave = { meal ->
                savedMeals = store.saveMeal(meal)
                showMealDialog = false
                message = "Öğün kaydedildi"
            }
        )
    }
}

@Composable
private fun HubCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = KaloriSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun SavedMealDialog(onDismiss: () -> Unit, onSave: (SavedMeal) -> Unit) {
    var name by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("100") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    fun valid() = name.isNotBlank() && (grams.toDoubleOrNull() ?: 0.0) > 0 && (calories.toDoubleOrNull() ?: -1.0) >= 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Öğün kaydet") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Öğün adı") }, singleLine = true)
                NumericHubField("Toplam gram", grams) { grams = it }
                NumericHubField("Kalori", calories) { calories = it }
                NumericHubField("Protein (g)", protein) { protein = it }
                NumericHubField("Karbonhidrat (g)", carbs) { carbs = it }
                NumericHubField("Yağ (g)", fat) { fat = it }
            }
        },
        confirmButton = {
            Button(enabled = valid(), onClick = {
                onSave(
                    SavedMeal(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        grams = grams.toDoubleOrNull() ?: 100.0,
                        calories = calories.toDoubleOrNull() ?: 0.0,
                        proteinG = protein.toDoubleOrNull() ?: 0.0,
                        carbsG = carbs.toDoubleOrNull() ?: 0.0,
                        fatG = fat.toDoubleOrNull() ?: 0.0
                    )
                )
            }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun NumericHubField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true
    )
}
