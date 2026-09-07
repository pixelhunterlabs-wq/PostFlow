package com.pixelhunter.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class ModernMainActivity : ComponentActivity() {
    private var authCallbackTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.client?.handleDeeplinks(intent)
        // A process launched directly by the OAuth callback has no onNewIntent call.
        // Trigger the same post-callback refresh path in that case.
        if (intent?.data?.scheme == "calorietracker" && intent.data?.host == "login") {
            authCallbackTick++
        }
        setContent {
            KaloriDarkTheme {
                ModernTrackerApp(authRefreshKey = authCallbackTick)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseProvider.client?.handleDeeplinks(intent)
        authCallbackTick++
    }
}

enum class MainTab(val label: String) { HOME("Ana Sayfa"), DIARY("Günlük"), PROGRESS("İlerleme"), PROFILE("Profil") }

@Composable
fun ModernTrackerApp(authRefreshKey: Int = 0, vm: TrackerViewModel = viewModel()) {
    val state = vm.uiState
    val context = LocalContext.current
    val store = remember(context) { LocalWellnessStore(context) }
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var showFoodSearch by remember { mutableStateOf(false) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showManualBarcode by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var waterMl by remember { mutableIntStateOf(store.waterMl()) }
    var favorites by remember { mutableStateOf(store.favorites()) }
    var onboardingDone by remember { mutableStateOf(store.onboardingDone()) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember(context) { GmsBarcodeScanning.getClient(context, scannerOptions) }

    fun scanBarcode() {
        scanner.startScan()
            .addOnSuccessListener { barcode -> barcode.rawValue?.let(vm::lookupBarcode) ?: run { showManualBarcode = true } }
            .addOnFailureListener { showManualBarcode = true }
    }

    LaunchedEffect(authRefreshKey) {
        // Give supabase-kt a moment to import the session parsed by handleDeeplinks
        // before asking currentUserOrNull() for it.
        if (authRefreshKey > 0) delay(250)
        vm.refreshSessionAndData()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    if (!SupabaseProvider.configured) {
        FullCenterMessage("Bağlantı ayarları eksik", "Supabase bağlantısı yapılandırılmamış.")
        return
    }

    if (!state.signedIn) {
        LoginScreen(
            loading = state.loading,
            message = state.message,
            onGoogle = vm::signInWithGoogle
        )
        return
    }

    if (!onboardingDone) {
        OnboardingScreen(
            email = state.email,
            onComplete = { calories, weight ->
                vm.updateGoal(calories)
                if (weight > 0) vm.addWeight(weight)
                store.markOnboardingDone()
                onboardingDone = true
            }
        )
        return
    }

    Scaffold(
        containerColor = KaloriBackground,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (tab == MainTab.HOME) "Bugün Harika Gidiyorsun" else tab.label, fontWeight = FontWeight.Bold)
                        if (tab == MainTab.HOME) Text(todayTurkish(), style = MaterialTheme.typography.labelSmall, color = KaloriMuted)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KaloriBackground, titleContentColor = KaloriText)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1412)) {
                MainTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                when (item) {
                                    MainTab.HOME -> Icons.Filled.Home
                                    MainTab.DIARY -> Icons.Filled.RestaurantMenu
                                    MainTab.PROGRESS -> Icons.Filled.ShowChart
                                    MainTab.PROFILE -> Icons.Filled.Person
                                },
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = KaloriGreen,
                            selectedTextColor = KaloriGreen,
                            indicatorColor = Color(0xFF123323),
                            unselectedIconColor = KaloriMuted,
                            unselectedTextColor = KaloriMuted
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == MainTab.HOME || tab == MainTab.DIARY) {
                FloatingActionButton(
                    onClick = { showFoodSearch = true },
                    containerColor = KaloriGreen,
                    contentColor = Color.Black
                ) { Icon(Icons.Filled.Add, "Yemek ekle") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    state = state,
                    waterMl = waterMl,
                    onAddWater = { waterMl = store.addWater(250) },
                    onRemoveWater = { waterMl = store.removeWater(250) },
                    onFood = { showFoodSearch = true },
                    onBarcode = ::scanBarcode,
                    onGoal = { showGoalDialog = true }
                )
                MainTab.DIARY -> DiaryScreen(state, onFood = { showFoodSearch = true }, onRepeat = vm::addRecentFood)
                MainTab.PROGRESS -> ProgressScreen(state, onWeight = { showWeightDialog = true })
                MainTab.PROFILE -> ProfileScreen(
                    state = state,
                    onGoal = { showGoalDialog = true },
                    onWeight = { showWeightDialog = true },
                    onPrivacy = { context.startActivity(Intent(context, PrivacyPolicyActivity::class.java)) },
                    onSignOut = vm::signOut,
                    onDelete = { showDeleteAccount = true }
                )
            }
            if (state.loading || state.barcodeLoading || state.accountDeleting) {
                LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter), color = KaloriGreen)
            }
        }
    }

    if (showFoodSearch) {
        FoodSearchDialog(
            favorites = favorites,
            recent = state.recentFoods,
            onDismiss = { showFoodSearch = false },
            onToggleFavorite = {
                favorites = store.toggleFavorite(it)
            },
            onAddCatalog = { food, meal, grams ->
                val ratio = grams / 100.0
                vm.addFood(food.name, meal, grams, food.calories100g * ratio, food.protein100g * ratio, food.carbs100g * ratio, food.fat100g * ratio, "turkish_catalog")
                showFoodSearch = false
            },
            onRepeat = {
                vm.addRecentFood(it)
                showFoodSearch = false
            },
            onManual = { name, meal, grams, calories, protein, carbs, fat ->
                vm.addFood(name, meal, grams, calories, protein, carbs, fat)
                showFoodSearch = false
            },
            onBarcode = {
                showFoodSearch = false
                scanBarcode()
            }
        )
    }

    if (showWeightDialog) SimpleNumberDialog("Kilo ekle", "kg", { showWeightDialog = false }) {
        vm.addWeight(it)
        showWeightDialog = false
    }
    if (showGoalDialog) SimpleNumberDialog("Günlük kalori hedefi", "kcal", { showGoalDialog = false }) {
        vm.updateGoal(it.toInt())
        showGoalDialog = false
    }
    if (showManualBarcode) BarcodeInputDialog({ showManualBarcode = false }) {
        vm.lookupBarcode(it)
        showManualBarcode = false
    }
    state.barcodeProduct?.let { product ->
        ModernBarcodeProductDialog(product, vm::clearBarcodeProduct) { meal, grams -> vm.addBarcodeFood(product, meal, grams) }
    }
    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = { showDeleteAccount = false },
            title = { Text("Hesabı ve tüm verileri sil?") },
            text = { Text("Kalori, makro, kilo ve hedef kayıtların ile hesabın kalıcı olarak silinecek. Bu işlem geri alınamaz.") },
            confirmButton = { Button(onClick = { showDeleteAccount = false; vm.deleteAccount() }, colors = ButtonDefaults.buttonColors(containerColor = KaloriDanger)) { Text("Kalıcı olarak sil") } },
            dismissButton = { TextButton(onClick = { showDeleteAccount = false }) { Text("Vazgeç") } }
        )
    }
}

@Composable
private fun LoginScreen(loading: Boolean, message: String?, onGoogle: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = RoundedCornerShape(28.dp), color = Color(0xFF0E2A1D)) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = KaloriGreen, modifier = Modifier.padding(22.dp).size(52.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Kalori Takip", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text("Türkiye odaklı sade kalori ve makro takibi", color = KaloriMuted)
        Spacer(Modifier.height(28.dp))
        Button(
            enabled = !loading,
            onClick = onGoogle,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.Black)
                Spacer(Modifier.width(8.dp))
                Text("Google açılıyor…")
            } else {
                Text("Google ile devam et")
            }
        }
        if (loading) {
            Spacer(Modifier.height(12.dp))
            Text("Tarayıcıda Google hesabını seçerek devam et.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
        }
        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = KaloriDanger, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun OnboardingScreen(email: String, onComplete: (Int, Double) -> Unit) {
    var gender by remember { mutableStateOf("Kadın") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableDoubleStateOf(1.375) }
    var goal by remember { mutableDoubleStateOf(0.25) }

    val ageN = age.toIntOrNull()
    val heightN = height.toDoubleOrNull()
    val weightN = weight.toDoubleOrNull()
    val valid = ageN != null && ageN in 14..100 && heightN != null && heightN in 120.0..230.0 && weightN != null && weightN in 35.0..300.0
    val estimate = if (valid) recommendedCalories(gender, ageN!!, heightN!!, weightN!!, activity, goal) else 0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Sana uygun hedefi oluşturalım", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(email, color = KaloriMuted)
            Text("Bu hesap başlangıç tahminidir; tıbbi öneri değildir.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Kadın", "Erkek").forEach { option -> FilterChip(selected = gender == option, onClick = { gender = option }, label = { Text(option) }) }
            }
        }
        item { NumberField("Yaş", age) { age = it } }
        item { NumberField("Boy (cm)", height) { height = it } }
        item { NumberField("Kilo (kg)", weight) { weight = it } }
        item {
            Text("Aktivite", fontWeight = FontWeight.Bold)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Düşük" to 1.2, "Hafif" to 1.375, "Orta" to 1.55, "Yüksek" to 1.725).forEach { (label, value) ->
                    FilterChip(selected = activity == value, onClick = { activity = value }, label = { Text(label) })
                }
            }
        }
        item {
            Text("Haftalık hedef", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Koru" to 0.0, "−0.25 kg" to 0.25, "−0.5 kg" to 0.5).forEach { (label, value) ->
                    FilterChip(selected = goal == value, onClick = { goal = value }, label = { Text(label) })
                }
            }
        }
        if (estimate > 0) item {
            AccentCard {
                Text("Önerilen başlangıç hedefi", color = KaloriMuted)
                Text("$estimate kcal / gün", style = MaterialTheme.typography.headlineMedium, color = KaloriGreen, fontWeight = FontWeight.Black)
            }
        }
        item { Button(enabled = valid, onClick = { onComplete(estimate, weightN ?: 0.0) }, modifier = Modifier.fillMaxWidth().height(54.dp)) { Text("Hedefimi oluştur") } }
    }
}

@Composable
private fun HomeScreen(
    state: TrackerUiState,
    waterMl: Int,
    onAddWater: () -> Unit,
    onRemoveWater: () -> Unit,
    onFood: () -> Unit,
    onBarcode: () -> Unit,
    onGoal: () -> Unit
) {
    val goal = state.calorieGoal.coerceAtLeast(1)
    val remaining = (goal - state.caloriesToday).coerceAtLeast(0.0).toInt()
    val proteinGoal = (goal * 0.30 / 4).toInt().coerceAtLeast(1)
    val carbsGoal = (goal * 0.45 / 4).toInt().coerceAtLeast(1)
    val fatGoal = (goal * 0.25 / 9).toInt().coerceAtLeast(1)

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            AccentCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (state.caloriesToday / goal).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 11.dp,
                            color = KaloriGreen,
                            trackColor = Color(0xFF1B3029)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = KaloriYellow)
                            Text(state.caloriesToday.toInt().toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("/ $goal kcal", style = MaterialTheme.typography.labelMedium, color = KaloriMuted)
                        }
                    }
                    Spacer(Modifier.width(22.dp))
                    Column {
                        Text("Kalan", color = KaloriMuted)
                        Text("$remaining", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                        Text("kcal", color = KaloriMuted)
                        TextButton(onClick = onGoal) { Text("Hedefi düzenle") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroCard("Protein", state.proteinToday.toInt(), proteinGoal, KaloriGreen, Modifier.weight(1f))
                MacroCard("Karbonhidrat", state.carbsToday.toInt(), carbsGoal, KaloriBlue, Modifier.weight(1f))
                MacroCard("Yağ", state.fatToday.toInt(), fatGoal, KaloriYellow, Modifier.weight(1f))
            }
        }
        item {
            AccentCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, null, tint = KaloriBlue, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Su", fontWeight = FontWeight.Bold)
                        Text("${waterMl / 1000.0} / 2.5 L", color = KaloriMuted)
                        LinearProgressIndicator(progress = { (waterMl / 2500f).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = KaloriBlue)
                    }
                    IconButton(onClick = onRemoveWater, enabled = waterMl > 0) { Icon(Icons.Filled.Remove, "250 ml çıkar") }
                    FilledTonalButton(onClick = onAddWater) { Text("+250 ml") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onFood, modifier = Modifier.weight(1f).height(52.dp)) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Yemek Ekle") }
                OutlinedButton(onClick = onBarcode, modifier = Modifier.weight(1f).height(52.dp)) { Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(6.dp)); Text("Barkod") }
            }
        }
        if (state.recentFoods.isNotEmpty()) item {
            SectionTitle("Son kullanılanlar")
            AccentCard {
                state.recentFoods.take(4).forEach { food ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                        Text(food.foodName, Modifier.weight(1f))
                        Text("${food.calories.toInt()} kcal", color = KaloriMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiaryScreen(state: TrackerUiState, onFood: () -> Unit, onRepeat: (CalorieEntry) -> Unit) {
    val meals = listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık", "Öğün")
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("${state.caloriesToday.toInt()} / ${state.calorieGoal} kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Button(onClick = onFood) { Text("+ Yemek") }
            }
        }
        meals.forEach { meal ->
            val entries = state.todayEntries.filter { normalizeMeal(it.mealType) == meal }
            if (entries.isNotEmpty() || meal != "Öğün") {
                item { SectionTitle(meal) }
                item {
                    AccentCard {
                        if (entries.isEmpty()) Text("Henüz kayıt yok", color = KaloriMuted)
                        entries.forEach { entry ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(entry.foodName, fontWeight = FontWeight.SemiBold)
                                    Text("${entry.grams.toInt()} g • P ${entry.proteinG.toInt()} • K ${entry.carbsG.toInt()} • Y ${entry.fatG.toInt()}", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                                }
                                Text("${entry.calories.toInt()} kcal")
                            }
                        }
                    }
                }
            }
        }
        if (state.recentFoods.isNotEmpty()) {
            item { SectionTitle("Hızlı tekrar ekle") }
            items(state.recentFoods.take(6), key = { "recent-diary-${it.id}" }) { food ->
                Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) { Text(food.foodName); Text("${food.calories.toInt()} kcal", color = KaloriMuted) }
                        TextButton(onClick = { onRepeat(food) }) { Text("+ Ekle") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressScreen(state: TrackerUiState, onWeight: () -> Unit) {
    val days = state.dailyTotals(7)
    val max = maxOf(state.calorieGoal.toDouble(), days.maxOfOrNull { it.calories } ?: 1.0)
    val adherence = days.count { it.calories > 0 && abs(it.calories - state.calorieGoal) <= state.calorieGoal * 0.10 }
    val proteinGoal = state.calorieGoal * 0.30 / 4
    val proteinDays = days.count { day -> state.entries.filter { it.dateText() == day.date.toString() }.sumOf { it.proteinG } >= proteinGoal }
    val newest = state.weights.firstOrNull()?.weightKg
    val oldest = state.weights.lastOrNull()?.weightKg
    val delta = if (newest != null && oldest != null) newest - oldest else null

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Son 7 gün") }
        item {
            AccentCard {
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    days.forEach { day ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                            Text(day.calories.toInt().toString(), style = MaterialTheme.typography.labelSmall, color = KaloriMuted)
                            Spacer(Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth().height(((day.calories / max) * 115).toFloat().coerceAtLeast(4f).dp),
                                shape = RoundedCornerShape(5.dp),
                                color = if (day.calories <= state.calorieGoal * 1.1) KaloriGreen else KaloriYellow
                            ) {}
                            Spacer(Modifier.height(4.dp))
                            Text(day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.forLanguageTag("tr-TR")), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("Ortalama", "${state.averageSince(7)} kcal", Modifier.weight(1f))
                MetricCard("Hedef uyumu", "%${(adherence / 7.0 * 100).toInt()}", Modifier.weight(1f))
            }
        }
        item {
            SectionTitle("Haftalık rapor")
            AccentCard {
                Text("Kalori hedef aralığında: $adherence / 7 gün")
                Text("Protein hedefine ulaşılan: $proteinDays / 7 gün")
                Text("Aylık ortalama: ${state.monthAverage} kcal")
                if (delta != null) Text("Kayıtlı kilo değişimi: ${if (delta > 0) "+" else ""}${"%.1f".format(Locale.US, delta)} kg")
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Kilo takibi", Modifier.weight(1f))
                TextButton(onClick = onWeight) { Text("+ Ekle") }
            }
        }
        if (state.weights.isEmpty()) item { Text("Henüz kilo kaydı yok.", color = KaloriMuted) }
        else items(state.weights.take(12), key = { "progress-weight-${it.id}" }) { weight ->
            Card(colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
                Row(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(weight.dateText(), Modifier.weight(1f), color = KaloriMuted)
                    Text("${weight.weightKg} kg", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
    state: TrackerUiState,
    onGoal: () -> Unit,
    onWeight: () -> Unit,
    onPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 100.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            AccentCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(50), color = Color(0xFF163629)) { Icon(Icons.Filled.Person, null, modifier = Modifier.padding(14.dp).size(28.dp), tint = KaloriGreen) }
                    Spacer(Modifier.width(12.dp))
                    Column { Text("Hesabım", fontWeight = FontWeight.Bold); Text(state.email, color = KaloriMuted) }
                }
            }
        }
        item { SettingsRow(Icons.Filled.TrackChanges, "Hedeflerim", "Günlük ${state.calorieGoal} kcal", onGoal) }
        item { SettingsRow(Icons.Filled.MonitorWeight, "Kilo kaydı", state.weights.firstOrNull()?.let { "Güncel ${it.weightKg} kg" } ?: "Kilo ekle", onWeight) }
        item { SettingsRow(Icons.Filled.Security, "Gizlilik Politikası", "Verileriniz ve hesap silme bilgileri", onPrivacy) }
        item { SettingsRow(Icons.Filled.Logout, "Çıkış Yap", "Google hesabından çık", onSignOut) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF251314))) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.DeleteForever, null, tint = KaloriDanger)
                    Spacer(Modifier.width(8.dp))
                    Text("Hesabımı ve verilerimi sil", color = KaloriDanger)
                }
            }
        }
    }
}

@Composable
private fun FoodSearchDialog(
    favorites: List<CatalogFood>,
    recent: List<CalorieEntry>,
    onDismiss: () -> Unit,
    onToggleFavorite: (CatalogFood) -> Unit,
    onAddCatalog: (CatalogFood, String, Double) -> Unit,
    onRepeat: (CalorieEntry) -> Unit,
    onManual: (String, String, Double, Double, Double, Double, Double) -> Unit,
    onBarcode: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<CatalogFood?>(null) }
    var showManual by remember { mutableStateOf(false) }
    val results = remember(query) { TurkishFoodCatalog.foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }.take(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Yemek Ekle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(query, { query = it }, label = { Text("Yiyecek ara") }, leadingIcon = { Icon(Icons.Filled.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onBarcode, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.QrCodeScanner, null); Spacer(Modifier.width(5.dp)); Text("Barkod") }
                    OutlinedButton(onClick = { showManual = true }, modifier = Modifier.weight(1f)) { Text("Manuel") }
                }
                if (favorites.isNotEmpty()) {
                    Text("Favoriler", fontWeight = FontWeight.Bold)
                    favorites.take(4).forEach { food -> FoodResultRow(food, true, { onToggleFavorite(food) }) { selected = food } }
                }
                if (query.isBlank() && recent.isNotEmpty()) {
                    Text("Son kullanılanlar", fontWeight = FontWeight.Bold)
                    recent.take(3).forEach { entry ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(entry.foodName); Text("${entry.calories.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { onRepeat(entry) }) { Text("+ Ekle") }
                        }
                    }
                }
                Text("Türkiye kataloğu", fontWeight = FontWeight.Bold)
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(results, key = { it.name }) { food -> FoodResultRow(food, favorites.any { it.name == food.name }, { onToggleFavorite(food) }) { selected = food } }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Kapat") } }
    )

    selected?.let { food -> CatalogAmountDialog(food, { selected = null }) { meal, grams -> onAddCatalog(food, meal, grams); selected = null } }
    if (showManual) ManualFoodDialog({ showManual = false }, onManual)
}

@Composable
private fun FoodResultRow(food: CatalogFood, favorite: Boolean, onFavorite: () -> Unit, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(food.name); Text("100 g • ${food.calories100g.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall) }
        IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", tint = if (favorite) KaloriYellow else KaloriMuted) }
        IconButton(onClick = onSelect) { Icon(Icons.Filled.AddCircle, "Ekle", tint = KaloriGreen) }
    }
}

@Composable
private fun CatalogAmountDialog(food: CatalogFood, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var grams by remember { mutableStateOf("100") }
    var meal by remember { mutableStateOf("Öğle") }
    val value = grams.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MealSelector(meal) { meal = it }
            NumberField("Gram", grams) { grams = it }
            Text("Yaklaşık ${(food.calories100g * value / 100).toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
        } },
        confirmButton = { Button(enabled = value > 0, onClick = { onSave(meal, value) }) { Text("Günlüğe ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun ManualFoodDialog(onDismiss: () -> Unit, onSave: (String, String, Double, Double, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }; var meal by remember { mutableStateOf("Öğle") }; var grams by remember { mutableStateOf("") }; var calories by remember { mutableStateOf("") }; var protein by remember { mutableStateOf("") }; var carbs by remember { mutableStateOf("") }; var fat by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manuel yemek") },
        text = { LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item { OutlinedTextField(name, { name = it }, label = { Text("Yemek") }, singleLine = true) }
            item { MealSelector(meal) { meal = it } }
            item { NumberField("Gram", grams) { grams = it } }
            item { NumberField("Kalori", calories) { calories = it } }
            item { NumberField("Protein g", protein) { protein = it } }
            item { NumberField("Karbonhidrat g", carbs) { carbs = it } }
            item { NumberField("Yağ g", fat) { fat = it } }
        } },
        confirmButton = { Button(enabled = name.isNotBlank() && grams.toDoubleOrNull() != null && calories.toDoubleOrNull() != null, onClick = { onSave(name, meal, grams.toDoubleOrNull() ?: 0.0, calories.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun MealSelector(selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Öğün", style = MaterialTheme.typography.labelMedium, color = KaloriMuted)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık").forEach { meal ->
                FilterChip(selected = selected == meal, onClick = { onSelected(meal) }, label = { Text(meal, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}

@Composable
private fun ModernBarcodeProductDialog(product: BarcodeProduct, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var meal by remember(product.barcode) { mutableStateOf("Öğle") }
    var grams by remember(product.barcode) { mutableStateOf("100") }
    val g = grams.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (product.brand.isNotBlank()) Text(product.brand, color = KaloriMuted)
            Text("100 g • ${product.calories100g.toInt()} kcal • P ${product.protein100g.toInt()} • K ${product.carbs100g.toInt()} • Y ${product.fat100g.toInt()}")
            MealSelector(meal) { meal = it }
            NumberField("Gram", grams) { grams = it }
            Text("Eklenecek: ${(product.calories100g * g / 100).toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
        } },
        confirmButton = { Button(enabled = g > 0, onClick = { onSave(meal, g) }) { Text("Günlüğe ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun BarcodeInputDialog(onDismiss: () -> Unit, onSearch: (String) -> Unit) {
    var barcode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Barkod numarası") },
        text = { OutlinedTextField(barcode, { barcode = it.filter(Char::isDigit) }, label = { Text("EAN / UPC") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)) },
        confirmButton = { Button(enabled = barcode.length in 8..14, onClick = { onSearch(barcode) }) { Text("Ürünü bul") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun SimpleNumberDialog(title: String, suffix: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { NumberField(suffix, value) { value = it } },
        confirmButton = { Button(enabled = value.toDoubleOrNull() != null, onClick = { value.toDoubleOrNull()?.let(onSave) }) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun NumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValue(it.filter { c -> c.isDigit() || c == '.' || c == ',' }.replace(',', '.')) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun MacroCard(title: String, value: Int, goal: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = color)
            Text("$value / $goal g", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            LinearProgressIndicator(progress = { (value / goal.toFloat()).coerceIn(0f, 1f) }, color = color, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
        Column(Modifier.padding(14.dp)) { Text(title, color = KaloriMuted); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = KaloriSurface)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = KaloriText)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Bold); Text(subtitle, color = KaloriMuted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Filled.ChevronRight, null, tint = KaloriMuted)
        }
    }
}

@Composable
private fun AccentCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = KaloriSurface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) { Text(text, modifier = modifier, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

@Composable
private fun FullCenterMessage(title: String, detail: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp)); Text(detail, color = KaloriMuted)
    }
}

private fun todayTurkish(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")))

private fun normalizeMeal(meal: String): String {
    val text = meal.trim().lowercase(Locale.forLanguageTag("tr-TR"))
    return when {
        text.contains("kahv") -> "Kahvaltı"
        text.contains("öğle") || text.contains("ogle") -> "Öğle"
        text.contains("akşam") || text.contains("aksam") -> "Akşam"
        text.contains("atış") || text.contains("atis") -> "Atıştırmalık"
        else -> "Öğün"
    }
}
