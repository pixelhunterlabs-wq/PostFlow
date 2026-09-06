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
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    @SerialName("daily_calorie_target") val dailyCalorieTarget: Int? = 2000,
    @SerialName("goal_weight_kg") val goalWeightKg: Double? = null
)

@Serializable
data class CalorieEntry(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("food_name") val foodName: String,
    val grams: Double,
    val calories: Double,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("meal_type") val mealType: String,
    val source: String = "manual",
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("eaten_at") val eatenAt: String = Instant.now().toString()
) {
    fun dateText(): String = eatenAt.take(10)
}

@Serializable
data class WeightEntry(
    val id: String = UUID.randomUUID().toString(),
    @SerialName("user_id") val userId: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("measured_at") val measuredAt: String = Instant.now().toString()
) {
    fun dateText(): String = measuredAt.take(10)
}

object SupabaseProvider {
    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) null else createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
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
    private val today = LocalDate.now()
    val todayEntries get() = entries.filter { it.dateText() == today.toString() }
    val caloriesToday get() = todayEntries.sumOf { it.calories }
    val proteinToday get() = todayEntries.sumOf { it.proteinG }
    val carbsToday get() = todayEntries.sumOf { it.carbsG }
    val fatToday get() = todayEntries.sumOf { it.fatG }

    fun caloriesSince(days: Long): Double {
        val start = today.minusDays(days - 1)
        return entries.filter {
            runCatching { LocalDate.parse(it.dateText()) >= start }.getOrDefault(false)
        }.sumOf { it.calories }
    }

    fun averageSince(days: Long): Int = (caloriesSince(days) / days).toInt()
}

class TrackerViewModel : ViewModel() {
    var uiState by mutableStateOf(TrackerUiState())
        private set

    private val supabase get() = SupabaseProvider.client

    init { refreshSessionAndData() }

    fun consumeMessage() {
        uiState = uiState.copy(message = null)
    }

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
                ensureProfile(user.id, user.email.orEmpty())
                loadAll()
            }.onFailure { uiState = uiState.copy(loading = false, message = it.message ?: "Veriler yüklenemedi") }
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

    private suspend fun ensureProfile(userId: String, email: String) {
        val client = supabase ?: return
        val profiles = client.from("calorie_profiles").select().decodeList<CalorieProfile>()
        val current = profiles.firstOrNull()
        if (current == null) {
            client.from("calorie_profiles").insert(CalorieProfile(userId = userId, email = email, dailyCalorieTarget = 2000))
            uiState = uiState.copy(calorieGoal = 2000)
        } else uiState = uiState.copy(calorieGoal = current.dailyCalorieTarget ?: 2000)
    }

    private suspend fun loadAll() {
        val client = supabase ?: return
        val entries = client.from("calorie_food_entries").select().decodeList<CalorieEntry>().sortedByDescending { it.eatenAt }
        val weights = client.from("calorie_weight_entries").select().decodeList<WeightEntry>().sortedByDescending { it.measuredAt }
        uiState = uiState.copy(entries = entries, weights = weights, loading = false)
    }

    fun addFood(name: String, meal: String, grams: Double, calories: Double, protein: Double, carbs: Double, fat: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_food_entries").insert(
                    CalorieEntry(
                        userId = user.id,
                        foodName = name.trim(),
                        mealType = meal.trim().ifBlank { "Öğün" },
                        grams = grams,
                        calories = calories,
                        proteinG = protein,
                        carbsG = carbs,
                        fatG = fat
                    )
                )
                loadAll()
                uiState = uiState.copy(message = "Yemek kaydı eklendi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Yemek eklenemedi") }
        }
    }

    fun updateFood(entry: CalorieEntry, name: String, meal: String, grams: Double, calories: Double, protein: Double, carbs: Double, fat: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                client.from("calorie_food_entries").update({
                    set("food_name", name.trim())
                    set("meal_type", meal.trim().ifBlank { "Öğün" })
                    set("grams", grams)
                    set("calories", calories)
                    set("protein_g", protein)
                    set("carbs_g", carbs)
                    set("fat_g", fat)
                }) {
                    filter { eq("id", entry.id) }
                }
                loadAll()
                uiState = uiState.copy(message = "Yemek kaydı güncellendi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Yemek güncellenemedi") }
        }
    }

    fun deleteFood(entryId: String) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                client.from("calorie_food_entries").delete {
                    filter { eq("id", entryId) }
                }
                loadAll()
                uiState = uiState.copy(message = "Yemek kaydı silindi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Yemek silinemedi") }
        }
    }

    fun addWeight(weightKg: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_weight_entries").insert(WeightEntry(userId = user.id, weightKg = weightKg))
                loadAll()
                uiState = uiState.copy(message = "Kilo kaydı eklendi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Kilo kaydı eklenemedi") }
        }
    }

    fun deleteWeight(entryId: String) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                client.from("calorie_weight_entries").delete {
                    filter { eq("id", entryId) }
                }
                loadAll()
                uiState = uiState.copy(message = "Kilo kaydı silindi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Kilo kaydı silinemedi") }
        }
    }

    fun updateGoal(goal: Int) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                val existing = client.from("calorie_profiles").select().decodeList<CalorieProfile>().firstOrNull()
                client.from("calorie_profiles").upsert(
                    (existing ?: CalorieProfile(userId = user.id, email = user.email)).copy(dailyCalorieTarget = goal)
                )
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
    val snackbarHostState = remember { SnackbarHostState() }
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var pendingDeleteWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(title = { Text("Kalori Takip") }, actions = {
                if (state.signedIn) TextButton(onClick = vm::signOut) { Text("Çıkış") }
            })
        },
        floatingActionButton = {
            if (state.signedIn) FloatingActionButton(onClick = { editingEntry = null; showFoodDialog = true }) { Text("+") }
        }
    ) { padding ->
        when {
            !SupabaseProvider.configured -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Supabase bağlantısı ayarlanmamış. apps/calorie-tracker/local.properties dosyasına SUPABASE_URL ve SUPABASE_KEY ekleyin.")
            }
            !state.signedIn -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kalorini ve kilo değişimini tek yerde takip et.", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(24.dp))
                Button(onClick = vm::signInWithGoogle) { Text("Google ile devam et") }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item {
                    Text(state.email, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(8.dp))
                    SummaryCard(state) { showGoalDialog = true }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("7 gün ort.", "${state.averageSince(7)} kcal", Modifier.weight(1f))
                        StatCard("30 gün ort.", "${state.averageSince(30)} kcal", Modifier.weight(1f))
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { editingEntry = null; showFoodDialog = true }, modifier = Modifier.weight(1f)) { Text("Yemek ekle") }
                        OutlinedButton(onClick = { showWeightDialog = true }, modifier = Modifier.weight(1f)) { Text("Kilo ekle") }
                    }
                }
                item { Text("Bugünkü kayıtlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (state.todayEntries.isEmpty()) item { Text("Henüz yemek kaydı yok.") }
                else items(state.todayEntries, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onEdit = { editingEntry = entry; showFoodDialog = true },
                        onDelete = { pendingDeleteEntry = entry }
                    )
                }
                item { Text("Son 30 gün geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(state.entries.filter { runCatching { LocalDate.parse(it.dateText()) >= LocalDate.now().minusDays(29) }.getOrDefault(false) }.take(40), key = { "history-${it.id}" }) { entry ->
                    EntryRow(
                        entry = entry,
                        showDate = true,
                        onEdit = { editingEntry = entry; showFoodDialog = true },
                        onDelete = { pendingDeleteEntry = entry }
                    )
                }
                item { Text("Kilo geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (state.weights.isEmpty()) item { Text("Henüz kilo kaydı yok.") }
                else items(state.weights.take(20), key = { it.id }) { w ->
                    ListItem(
                        headlineContent = { Text("${w.weightKg} kg") },
                        supportingContent = { Text(w.dateText()) },
                        trailingContent = { TextButton(onClick = { pendingDeleteWeight = w }) { Text("Sil") } }
                    )
                }
            }
        }
    }

    if (showFoodDialog) {
        FoodDialog(
            initial = editingEntry,
            onDismiss = { showFoodDialog = false; editingEntry = null }
        ) { a, b, c, d, e, f, g ->
            val edit = editingEntry
            if (edit == null) vm.addFood(a, b, c, d, e, f, g)
            else vm.updateFood(edit, a, b, c, d, e, f, g)
            showFoodDialog = false
            editingEntry = null
        }
    }

    if (showWeightDialog) NumberDialog("Kilo ekle", "kg", { showWeightDialog = false }) { vm.addWeight(it); showWeightDialog = false }
    if (showGoalDialog) NumberDialog("Günlük kalori hedefi", "kcal", { showGoalDialog = false }) { vm.updateGoal(it.toInt()); showGoalDialog = false }

    pendingDeleteEntry?.let { entry ->
        ConfirmDeleteDialog(
            title = "Yemek kaydı silinsin mi?",
            detail = entry.foodName,
            onDismiss = { pendingDeleteEntry = null },
            onConfirm = { vm.deleteFood(entry.id); pendingDeleteEntry = null }
        )
    }

    pendingDeleteWeight?.let { weight ->
        ConfirmDeleteDialog(
            title = "Kilo kaydı silinsin mi?",
            detail = "${weight.weightKg} kg • ${weight.dateText()}",
            onDismiss = { pendingDeleteWeight = null },
            onConfirm = { vm.deleteWeight(weight.id); pendingDeleteWeight = null }
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SummaryCard(state: TrackerUiState, onGoalClick: () -> Unit) {
    val remaining = (state.calorieGoal - state.caloriesToday).coerceAtLeast(0.0)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bugün", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${state.caloriesToday.toInt()} / ${state.calorieGoal} kcal")
            LinearProgressIndicator(progress = { (state.caloriesToday / state.calorieGoal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Text("Kalan: ${remaining.toInt()} kcal")
            Text("Protein ${state.proteinToday.toInt()} g • Karb ${state.carbsToday.toInt()} g • Yağ ${state.fatToday.toInt()} g")
            TextButton(onClick = onGoalClick) { Text("Hedefi değiştir") }
        }
    }
}

@Composable
private fun EntryRow(entry: CalorieEntry, showDate: Boolean = false, onEdit: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        headlineContent = { Text(entry.foodName) },
        supportingContent = { Text((if (showDate) "${entry.dateText()} • " else "") + "${entry.mealType} • ${entry.grams.toInt()} g • P ${entry.proteinG.toInt()} / K ${entry.carbsG.toInt()} / Y ${entry.fatG.toInt()}") },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text("${entry.calories.toInt()} kcal")
                Row {
                    TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Düzenle") }
                    TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Sil") }
                }
            }
        }
    )
}

@Composable
private fun FoodDialog(
    initial: CalorieEntry? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double, Double, Double, Double, Double) -> Unit
) {
    var name by remember(initial?.id) { mutableStateOf(initial?.foodName.orEmpty()) }
    var meal by remember(initial?.id) { mutableStateOf(initial?.mealType ?: "Öğün") }
    var grams by remember(initial?.id) { mutableStateOf(initial?.grams?.toString().orEmpty()) }
    var calories by remember(initial?.id) { mutableStateOf(initial?.calories?.toString().orEmpty()) }
    var protein by remember(initial?.id) { mutableStateOf(initial?.proteinG?.toString().orEmpty()) }
    var carbs by remember(initial?.id) { mutableStateOf(initial?.carbsG?.toString().orEmpty()) }
    var fat by remember(initial?.id) { mutableStateOf(initial?.fatG?.toString().orEmpty()) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Yemek ekle" else "Yemek düzenle") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text("Yemek") }, singleLine = true)
            OutlinedTextField(meal, { meal = it }, label = { Text("Öğün") }, singleLine = true)
            OutlinedTextField(grams, { grams = it }, label = { Text("Gram") }, singleLine = true)
            OutlinedTextField(calories, { calories = it }, label = { Text("Kalori") }, singleLine = true)
            OutlinedTextField(protein, { protein = it }, label = { Text("Protein g") }, singleLine = true)
            OutlinedTextField(carbs, { carbs = it }, label = { Text("Karbonhidrat g") }, singleLine = true)
            OutlinedTextField(fat, { fat = it }, label = { Text("Yağ g") }, singleLine = true)
        }
    }, confirmButton = {
        Button(enabled = name.isNotBlank() && grams.toDoubleOrNull() != null && calories.toDoubleOrNull() != null, onClick = {
            onSave(name, meal, grams.toDoubleOrNull() ?: 0.0, calories.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0)
        }) { Text(if (initial == null) "Kaydet" else "Güncelle") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } })
}

@Composable
private fun ConfirmDeleteDialog(title: String, detail: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(detail) },
        confirmButton = { Button(onClick = onConfirm) { Text("Sil") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun NumberDialog(title: String, suffix: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value, { value = it }, label = { Text(suffix) }, singleLine = true) },
        confirmButton = { Button(enabled = value.toDoubleOrNull() != null, onClick = { value.toDoubleOrNull()?.let(onSave) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
