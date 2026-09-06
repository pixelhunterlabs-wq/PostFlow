package com.pixelhunter.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Serializable
data class CalorieProfile(
    @SerialName("user_id") val userId: String,
    @SerialName("daily_calorie_goal") val dailyCalorieGoal: Int = 2000,
    @SerialName("protein_goal_g") val proteinGoalG: Double = 100.0,
    @SerialName("carb_goal_g") val carbGoalG: Double = 250.0,
    @SerialName("fat_goal_g") val fatGoalG: Double = 65.0
)

@Serializable
data class CalorieEntry(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    val name: String,
    @SerialName("meal_type") val mealType: String,
    val grams: Double,
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carb_g") val carbG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("entry_date") val entryDate: String = LocalDate.now().toString(),
    @SerialName("created_at") val createdAt: String = Instant.now().toString()
)

@Serializable
data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("entry_date") val entryDate: String = LocalDate.now().toString(),
    @SerialName("created_at") val createdAt: String = Instant.now().toString()
)

object SupabaseProvider {
    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) null else createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            install(Auth) {
                scheme = "calorietracker"
                host = "login"
            }
            install(Postgrest)
        }
    }
}

data class TrackerUiState(
    val loading: Boolean = false,
    val signedIn: Boolean = false,
    val email: String = "",
    val calorieGoal: Int = 2000,
    val entries: List<CalorieEntry> = emptyList(),
    val weights: List<WeightEntry> = emptyList(),
    val message: String? = null
) {
    val todayEntries: List<CalorieEntry>
        get() = entries.filter { it.entryDate == LocalDate.now().toString() }
    val caloriesToday: Double get() = todayEntries.sumOf { it.calories }
    val proteinToday: Double get() = todayEntries.sumOf { it.proteinG }
    val carbsToday: Double get() = todayEntries.sumOf { it.carbG }
    val fatToday: Double get() = todayEntries.sumOf { it.fatG }
}

class TrackerViewModel : ViewModel() {
    var uiState by mutableStateOf(TrackerUiState())
        private set

    private val supabase get() = SupabaseProvider.client

    init { refreshSessionAndData() }

    fun refreshSessionAndData() {
        val client = supabase ?: run {
            uiState = uiState.copy(message = "Supabase ayarları eksik. local.properties dosyasını doldurun.")
            return
        }
        viewModelScope.launch {
            runCatching {
                val user = client.auth.currentUserOrNull()
                if (user == null) {
                    uiState = uiState.copy(signedIn = false, loading = false)
                    return@launch
                }
                uiState = uiState.copy(loading = true, signedIn = true, email = user.email.orEmpty())
                ensureProfile(user.id)
                loadAll()
            }.onFailure {
                uiState = uiState.copy(loading = false, message = it.message ?: "Veriler yüklenemedi")
            }
        }
    }

    fun signInWithGoogle() {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching { client.auth.signInWith(Google) }
                .onFailure { uiState = uiState.copy(message = it.message ?: "Google girişi başlatılamadı") }
        }
    }

    fun signOut() {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching { client.auth.signOut() }
            uiState = TrackerUiState()
        }
    }

    private suspend fun ensureProfile(userId: String) {
        val client = supabase ?: return
        val profiles = client.from("calorie_profiles").select().decodeList<CalorieProfile>()
        val current = profiles.firstOrNull()
        if (current == null) {
            client.from("calorie_profiles").insert(CalorieProfile(userId = userId))
            uiState = uiState.copy(calorieGoal = 2000)
        } else {
            uiState = uiState.copy(calorieGoal = current.dailyCalorieGoal)
        }
    }

    private suspend fun loadAll() {
        val client = supabase ?: return
        val entries = client.from("calorie_entries").select().decodeList<CalorieEntry>()
            .sortedByDescending { it.createdAt }
        val weights = client.from("calorie_weights").select().decodeList<WeightEntry>()
            .sortedByDescending { it.entryDate }
        uiState = uiState.copy(entries = entries, weights = weights, loading = false, message = null)
    }

    fun addFood(name: String, meal: String, grams: Double, calories: Double, protein: Double, carbs: Double, fat: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_entries").insert(
                    CalorieEntry(
                        userId = user.id,
                        name = name.trim(),
                        mealType = meal,
                        grams = grams,
                        calories = calories,
                        proteinG = protein,
                        carbG = carbs,
                        fatG = fat
                    )
                )
                loadAll()
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Yemek eklenemedi") }
        }
    }

    fun addWeight(weightKg: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_weights").insert(WeightEntry(userId = user.id, weightKg = weightKg))
                loadAll()
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Kilo kaydı eklenemedi") }
        }
    }

    fun updateGoal(goal: Int) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_profiles").upsert(CalorieProfile(userId = user.id, dailyCalorieGoal = goal))
                uiState = uiState.copy(calorieGoal = goal, message = "Günlük hedef güncellendi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Hedef güncellenemedi") }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.client?.handleDeeplinks(intent)
        setContent { MaterialTheme { TrackerApp() } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseProvider.client?.handleDeeplinks(intent)
    }
}

@Composable
fun TrackerApp(vm: TrackerViewModel = viewModel()) {
    val state = vm.uiState
    var showFoodDialog by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kalori Takip") },
                actions = {
                    if (state.signedIn) TextButton(onClick = vm::signOut) { Text("Çıkış") }
                }
            )
        },
        floatingActionButton = {
            if (state.signedIn) FloatingActionButton(onClick = { showFoodDialog = true }) { Text("+") }
        }
    ) { padding ->
        if (!SupabaseProvider.configured) {
            Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Supabase bağlantısı ayarlanmamış. apps/calorie-tracker/local.properties dosyasına SUPABASE_URL ve SUPABASE_KEY ekleyin.")
            }
            return@Scaffold
        }

        if (!state.signedIn) {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Kalorini ve kilo değişimini tek yerde takip et.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
                Button(onClick = vm::signInWithGoogle) { Text("Google ile devam et") }
            }
            return@Scaffold
        }

        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(state.email, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                SummaryCard(state, onGoalClick = { showGoalDialog = true })
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showFoodDialog = true }, modifier = Modifier.weight(1f)) { Text("Yemek ekle") }
                    OutlinedButton(onClick = { showWeightDialog = true }, modifier = Modifier.weight(1f)) { Text("Kilo ekle") }
                }
            }
            item { Text("Bugünkü kayıtlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (state.todayEntries.isEmpty()) {
                item { Text("Henüz yemek kaydı yok.") }
            } else {
                items(state.todayEntries, key = { it.id }) { EntryRow(it) }
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text("Kilo geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (state.weights.isEmpty()) item { Text("Henüz kilo kaydı yok.") }
            else items(state.weights.take(10), key = { it.id }) { w ->
                ListItem(headlineContent = { Text("${w.weightKg} kg") }, supportingContent = { Text(w.entryDate) })
            }
        }
    }

    state.message?.let { message ->
        LaunchedEffect(message) { /* message remains visible through dialogs/cards in V1 */ }
    }

    if (showFoodDialog) FoodDialog(
        onDismiss = { showFoodDialog = false },
        onSave = { name, meal, grams, calories, protein, carbs, fat ->
            vm.addFood(name, meal, grams, calories, protein, carbs, fat)
            showFoodDialog = false
        }
    )
    if (showWeightDialog) NumberDialog("Kilo ekle", "kg", onDismiss = { showWeightDialog = false }) {
        vm.addWeight(it)
        showWeightDialog = false
    }
    if (showGoalDialog) NumberDialog("Günlük kalori hedefi", "kcal", onDismiss = { showGoalDialog = false }) {
        vm.updateGoal(it.toInt())
        showGoalDialog = false
    }
}

@Composable
private fun SummaryCard(state: TrackerUiState, onGoalClick: () -> Unit) {
    val remaining = (state.calorieGoal - state.caloriesToday).coerceAtLeast(0.0)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bugün", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${state.caloriesToday.toInt()} / ${state.calorieGoal} kcal")
            LinearProgressIndicator(
                progress = { (state.caloriesToday / state.calorieGoal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Kalan: ${remaining.toInt()} kcal")
            Text("Protein ${state.proteinToday.toInt()} g  •  Karb ${state.carbsToday.toInt()} g  •  Yağ ${state.fatToday.toInt()} g")
            TextButton(onClick = onGoalClick) { Text("Hedefi değiştir") }
        }
    }
}

@Composable
private fun EntryRow(entry: CalorieEntry) {
    ListItem(
        headlineContent = { Text(entry.name) },
        supportingContent = { Text("${entry.mealType} • ${entry.grams.toInt()} g • P ${entry.proteinG.toInt()} / K ${entry.carbG.toInt()} / Y ${entry.fatG.toInt()}") },
        trailingContent = { Text("${entry.calories.toInt()} kcal") }
    )
}

@Composable
private fun FoodDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf("Öğün") }
    var grams by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yemek ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Yemek") }, singleLine = true)
                OutlinedTextField(meal, { meal = it }, label = { Text("Öğün") }, singleLine = true)
                OutlinedTextField(grams, { grams = it }, label = { Text("Gram") }, singleLine = true)
                OutlinedTextField(calories, { calories = it }, label = { Text("Kalori") }, singleLine = true)
                OutlinedTextField(protein, { protein = it }, label = { Text("Protein g") }, singleLine = true)
                OutlinedTextField(carbs, { carbs = it }, label = { Text("Karbonhidrat g") }, singleLine = true)
                OutlinedTextField(fat, { fat = it }, label = { Text("Yağ g") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && grams.toDoubleOrNull() != null && calories.toDoubleOrNull() != null,
                onClick = {
                    onSave(
                        name, meal,
                        grams.toDoubleOrNull() ?: 0.0,
                        calories.toDoubleOrNull() ?: 0.0,
                        protein.toDoubleOrNull() ?: 0.0,
                        carbs.toDoubleOrNull() ?: 0.0,
                        fat.toDoubleOrNull() ?: 0.0
                    )
                }
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun NumberDialog(title: String, suffix: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(suffix) }, singleLine = true) },
        confirmButton = {
            Button(enabled = value.toDoubleOrNull() != null, onClick = { value.toDoubleOrNull()?.let(onSave) }) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
