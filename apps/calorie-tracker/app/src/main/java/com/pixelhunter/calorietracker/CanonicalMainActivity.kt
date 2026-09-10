package com.pixelhunter.calorietracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

/**
 * Production Compose implementation of the approved Kalori Takip reference.
 * Canonical UI reference: the approved multi-screen dark/neon reference supplied on 2026-09-10.
 * Login reference: the approved fitness-photo Google sign-in reference supplied with it.
 *
 * This file intentionally reuses TrackerViewModel and the existing feature activities so Supabase,
 * Google OAuth, AI photo analysis, voice logging, barcode lookup, water, favorites and saved meals
 * continue to use the existing production data paths.
 */
class CanonicalMainActivity : ComponentActivity() {
    private var authCallbackTick by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SupabaseProvider.client?.handleDeeplinks(intent)
        if (intent?.data?.scheme == "calorietracker" && intent.data?.host == "login") authCallbackTick++
        setContent {
            KaloriDarkTheme {
                CanonicalTrackerApp(authRefreshKey = authCallbackTick)
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

private enum class CanonicalTab(val label: String, val icon: ImageVector) {
    HOME("Ana Sayfa", Icons.Filled.Home),
    DIARY("Günlük", Icons.Filled.RestaurantMenu),
    PROGRESS("İlerleme", Icons.Filled.ShowChart),
    PROFILE("Profil", Icons.Filled.Person)
}

private enum class CanonicalPage { ROOT, FOOD_ADD, BARCODE, WEIGHT, FOOD_DETAIL }
private enum class FoodAddMode(val label: String) { SEARCH("Ara"), BARCODE("Barkod"), FAVORITES("Favoriler") }
private enum class ProgressRange(val label: String) { DAY("Gün"), WEEK("Hafta"), MONTH("Ay"), YEAR("Yıl") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CanonicalTrackerApp(authRefreshKey: Int = 0, vm: TrackerViewModel = viewModel()) {
    val state = vm.uiState
    val context = androidx.compose.ui.platform.LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(CanonicalTab.HOME) }
    var page by remember { mutableStateOf(CanonicalPage.ROOT) }
    var selectedEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var selectedCatalog by remember { mutableStateOf<CatalogFood?>(null) }
    var editEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var deleteEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var deleteWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showManualFood by remember { mutableStateOf(false) }
    var showManualBarcode by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E)
            .enableAutoZoom()
            .build()
    }
    val scanner = remember(context) { GmsBarcodeScanning.getClient(context, scannerOptions) }

    fun scanBarcode() {
        runCatching {
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    BarcodeSupport.normalize(barcode.rawValue)?.let(vm::lookupBarcode)
                        ?: vm.showMessage("Geçerli bir ürün barkodu okunamadı")
                }
                .addOnFailureListener { showManualBarcode = true }
        }.onFailure { showManualBarcode = true }
    }

    fun openPhoto() = context.startActivity(Intent(context, PhotoMealAnalysisActivity::class.java))
    fun openVoice() = context.startActivity(Intent(context, VoiceLogActivity::class.java))
    fun openWellness() = context.startActivity(Intent(context, WellnessHubActivity::class.java))
    fun openPrivacy() = context.startActivity(Intent(context, PrivacyPolicyActivity::class.java))
    fun openRecipeBuilder() = context.startActivity(Intent(context, RecipeBuilderActivity::class.java))

    LaunchedEffect(authRefreshKey) {
        if (authRefreshKey > 0) delay(250)
        vm.refreshSessionAndData()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    when {
        !SupabaseProvider.configured -> CanonicalCenterState("Bağlantı ayarları eksik", "Supabase bağlantısı yapılandırılmamış.")
        state.authChecking -> CanonicalCenterState("Oturum açılıyor", "Hesabın kontrol ediliyor.")
        !state.signedIn -> CanonicalLoginScreen(
            loading = state.loading,
            onGoogle = vm::signInWithGoogle,
            onPrivacy = ::openPrivacy
        )
        !state.onboardingCompleted && state.entries.isEmpty() -> CanonicalOnboardingScreen(state.email, vm::completeOnboarding)
        else -> {
            BackHandler(enabled = page != CanonicalPage.ROOT) {
                page = CanonicalPage.ROOT
                selectedEntry = null
            }

            Scaffold(
                containerColor = KaloriBackground,
                snackbarHost = { SnackbarHost(snackbar) },
                topBar = {
                    if (page != CanonicalPage.ROOT) {
                        TopAppBar(
                            title = { Text(canonicalPageTitle(page), fontWeight = FontWeight.Bold) },
                            navigationIcon = {
                                IconButton(onClick = {
                                    page = CanonicalPage.ROOT
                                    selectedEntry = null
                                }) { Icon(Icons.Filled.ArrowBack, "Geri") }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = KaloriBackground,
                                titleContentColor = KaloriText,
                                navigationIconContentColor = KaloriText
                            )
                        )
                    }
                },
                bottomBar = {
                    if (page == CanonicalPage.ROOT) CanonicalBottomBar(tab) { tab = it }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (page) {
                        CanonicalPage.ROOT -> when (tab) {
                            CanonicalTab.HOME -> CanonicalHomeScreen(
                                state = state,
                                onFood = { page = CanonicalPage.FOOD_ADD },
                                onBarcode = { page = CanonicalPage.BARCODE },
                                onPhoto = ::openPhoto,
                                onVoice = ::openVoice,
                                onGoal = { showGoalDialog = true },
                                onWaterPlus = { vm.changeWater(250) },
                                onWaterMinus = { vm.changeWater(-250) },
                                onEntry = {
                                    selectedEntry = it
                                    page = CanonicalPage.FOOD_DETAIL
                                }
                            )
                            CanonicalTab.DIARY -> CanonicalDiaryScreen(
                                state = state,
                                onAddFood = { page = CanonicalPage.FOOD_ADD },
                                onEntry = {
                                    selectedEntry = it
                                    page = CanonicalPage.FOOD_DETAIL
                                },
                                onRepeat = vm::addRecentFood
                            )
                            CanonicalTab.PROGRESS -> CanonicalProgressScreen(
                                state = state,
                                onWeight = { page = CanonicalPage.WEIGHT },
                                onAddWeight = { showWeightDialog = true }
                            )
                            CanonicalTab.PROFILE -> CanonicalProfileScreen(
                                state = state,
                                onGoal = { showGoalDialog = true },
                                onWeight = { page = CanonicalPage.WEIGHT },
                                onWellness = ::openWellness,
                                onRecipeBuilder = ::openRecipeBuilder,
                                onPrivacy = ::openPrivacy,
                                onSignOut = vm::signOut,
                                onDelete = { showDeleteAccount = true }
                            )
                        }
                        CanonicalPage.FOOD_ADD -> CanonicalFoodAddScreen(
                            state = state,
                            onSelect = { selectedCatalog = it },
                            onFavorite = vm::toggleFavorite,
                            onBarcode = { page = CanonicalPage.BARCODE },
                            onPhoto = ::openPhoto,
                            onVoice = ::openVoice,
                            onManual = { showManualFood = true },
                            onRepeat = vm::addRecentFood,
                            onAddSavedMeal = { meal -> vm.addSavedMealToDiary(meal.id, "Öğün") }
                        )
                        CanonicalPage.BARCODE -> CanonicalBarcodeScreen(
                            state = state,
                            onScan = ::scanBarcode,
                            onManual = { showManualBarcode = true },
                            onClearProduct = vm::clearBarcodeProduct,
                            onAddProduct = vm::addBarcodeFood
                        )
                        CanonicalPage.WEIGHT -> CanonicalWeightScreen(
                            state = state,
                            onAddWeight = { showWeightDialog = true },
                            onDeleteWeight = { deleteWeight = it }
                        )
                        CanonicalPage.FOOD_DETAIL -> selectedEntry?.let { entry ->
                            CanonicalFoodDetailScreen(
                                entry = entry,
                                onEdit = { editEntry = entry },
                                onDelete = { deleteEntry = entry },
                                onRepeat = { vm.addRecentFood(entry) }
                            )
                        } ?: CanonicalCenterState("Kayıt bulunamadı", "Bu öğün artık mevcut değil.")
                    }

                    if (state.loading || state.barcodeLoading || state.accountDeleting) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = KaloriGreen,
                            trackColor = Color(0xFF173127)
                        )
                    }
                }
            }
        }
    }

    selectedCatalog?.let { food ->
        CanonicalCatalogAmountDialog(
            food = food,
            onDismiss = { selectedCatalog = null },
            onSave = { meal, grams ->
                val ratio = grams / 100.0
                vm.addFood(
                    food.name, meal, grams,
                    food.calories100g * ratio,
                    food.protein100g * ratio,
                    food.carbs100g * ratio,
                    food.fat100g * ratio,
                    "turkish_catalog"
                )
                selectedCatalog = null
            }
        )
    }
    if (showManualFood) CanonicalManualFoodDialog({ showManualFood = false }) { name, meal, grams, calories, protein, carbs, fat ->
        vm.addFood(name, meal, grams, calories, protein, carbs, fat)
        showManualFood = false
    }
    editEntry?.let { entry ->
        CanonicalEditEntryDialog(entry, { editEntry = null }) { name, meal, grams, calories, protein, carbs, fat ->
            vm.updateFood(entry, name, meal, grams, calories, protein, carbs, fat)
            selectedEntry = entry.copy(foodName = name, mealType = meal, grams = grams, calories = calories, proteinG = protein, carbsG = carbs, fatG = fat)
            editEntry = null
        }
    }
    deleteEntry?.let { entry ->
        CanonicalDeleteDialog("Öğünü sil", "${entry.foodName} kaydı kalıcı olarak silinecek.", { deleteEntry = null }) {
            vm.deleteFood(entry.id)
            deleteEntry = null
            selectedEntry = null
            page = CanonicalPage.ROOT
        }
    }
    deleteWeight?.let { entry ->
        CanonicalDeleteDialog("Kilo kaydını sil", "${entry.weightKg} kg • ${entry.dateText()}", { deleteWeight = null }) {
            vm.deleteWeight(entry.id)
            deleteWeight = null
        }
    }
    if (showWeightDialog) CanonicalNumberDialog("Kilo ekle", "kg", { showWeightDialog = false }) {
        vm.addWeight(it)
        showWeightDialog = false
    }
    if (showGoalDialog) CanonicalNumberDialog("Günlük kalori hedefi", "kcal", { showGoalDialog = false }) {
        vm.updateGoal(it.toInt())
        showGoalDialog = false
    }
    if (showManualBarcode) CanonicalBarcodeInputDialog({ showManualBarcode = false }) {
        vm.lookupBarcode(it)
        showManualBarcode = false
        page = CanonicalPage.BARCODE
    }
    if (showDeleteAccount) {
        CanonicalDeleteDialog(
            title = "Verilerimi sil",
            detail = "Kalori, makro, kilo, hedef ve hesap verilerin kalıcı olarak silinecek. Bu işlem geri alınamaz.",
            onDismiss = { showDeleteAccount = false }
        ) {
            showDeleteAccount = false
            vm.deleteAccount()
        }
    }
}

@Composable
private fun CanonicalLoginScreen(loading: Boolean, onGoogle: () -> Unit, onPrivacy: () -> Unit) {
    Box(Modifier.fillMaxSize().background(KaloriBackground)) {
        Image(
            painter = painterResource(R.drawable.login_fitness_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    .58f to Color(0x12000302),
                    1f to Color(0xD9000705)
                )
            )
        )
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
                Button(
                    enabled = !loading,
                    onClick = onGoogle,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(29.dp),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF151515),
                        disabledContainerColor = Color(0xFFE4E7E6),
                        disabledContentColor = Color(0xFF555957)
                    )
                ) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.ic_google_g),
                            contentDescription = "Google",
                            modifier = Modifier.align(Alignment.CenterStart).size(23.dp)
                        )
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = KaloriGreen)
                        } else {
                            Text("Google ile devam et", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.CenterEnd).size(22.dp)
                        )
                    }
                }
                Spacer(Modifier.height(13.dp))
                Text(
                    "Hesabınla giriş yaparak verilerin cihazlar arasında güvende kalır.",
                    color = Color(0xFFB8C0BD),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    "Gizlilik Politikası",
                    color = KaloriGreen,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onPrivacy).padding(horizontal = 10.dp, vertical = 7.dp)
                )
        }
    }
}

@Composable
private fun CanonicalOnboardingScreen(email: String, onComplete: (Int, Double) -> Unit) {
    var gender by remember { mutableStateOf("Kadın") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var activity by remember { mutableDoubleStateOf(1.375) }
    var weeklyGoal by remember { mutableDoubleStateOf(-0.25) }

    val ageN = age.toIntOrNull()
    val heightN = height.toDoubleOrNull()
    val weightN = weight.toDoubleOrNull()
    val valid = ageN != null && ageN in 14..100 && heightN != null && heightN in 120.0..230.0 && weightN != null && weightN in 35.0..300.0
    val goals = if (valid) GoalCalculator.goals(PersonalGoalInput(gender, ageN!!, heightN!!, weightN!!, activity, weeklyGoal)) else null

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 22.dp, 20.dp, 36.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Hedefini oluşturalım", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = KaloriText)
            Text(email, color = KaloriMuted)
            Text("Başlangıç hedefini verilerine göre hesaplıyoruz.", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
        }
        item { CanonicalSegmentedChoice(listOf("Kadın", "Erkek"), gender) { gender = it } }
        item { CanonicalNumberField("Yaş", age) { age = it } }
        item { CanonicalNumberField("Boy (cm)", height) { height = it } }
        item { CanonicalNumberField("Kilo (kg)", weight) { weight = it } }
        item {
            Text("Aktivite", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Düşük" to 1.2, "Hafif" to 1.375, "Orta" to 1.55, "Yüksek" to 1.725)) { item ->
                    FilterChip(selected = activity == item.second, onClick = { activity = item.second }, label = { Text(item.first) })
                }
            }
        }
        item {
            Text("Haftalık hedef", fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("Koru" to 0.0, "−0.25 kg" to -0.25, "−0.5 kg" to -0.5, "+0.25 kg" to 0.25)) { item ->
                    FilterChip(selected = weeklyGoal == item.second, onClick = { weeklyGoal = item.second }, label = { Text(item.first) })
                }
            }
        }
        goals?.let { calculated ->
            item {
                CanonicalNeonCard {
                    Text("Önerilen günlük hedef", color = KaloriMuted)
                    Text("${calculated.calories} kcal", color = KaloriGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Protein ${calculated.proteinG} g  •  Karbonhidrat ${calculated.carbsG} g  •  Yağ ${calculated.fatG} g", color = KaloriText)
                }
            }
        }
        item {
            Button(
                enabled = goals != null,
                onClick = { if (goals != null && weightN != null) onComplete(goals.calories, weightN) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color(0xFF00140B))
            ) { Text("Bu hedefle başla", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun CanonicalHomeScreen(
    state: TrackerUiState,
    onFood: () -> Unit,
    onBarcode: () -> Unit,
    onPhoto: () -> Unit,
    onVoice: () -> Unit,
    onGoal: () -> Unit,
    onWaterPlus: () -> Unit,
    onWaterMinus: () -> Unit,
    onEntry: (CalorieEntry) -> Unit
) {
    val goal = state.calorieGoal.coerceAtLeast(1)
    val remaining = (goal - state.caloriesToday).coerceAtLeast(0.0).toInt()
    val proteinGoal = (goal * .30 / 4).toInt().coerceAtLeast(1)
    val carbsGoal = (goal * .45 / 4).toInt().coerceAtLeast(1)
    val fatGoal = (goal * .25 / 9).toInt().coerceAtLeast(1)
    val userName = canonicalDisplayName(state.email)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column {
                Text("Merhaba 👋", color = KaloriMuted, style = MaterialTheme.typography.bodyMedium)
                Text("Bugün Harika Gidiyorsun", color = KaloriText, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                Text("$userName  •  ${canonicalToday()}", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(154.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { (state.caloriesToday / goal).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 13.dp,
                            color = KaloriGreen,
                            trackColor = Color(0xFF164331)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = KaloriYellow, modifier = Modifier.size(24.dp))
                            Text(canonicalNumber(state.caloriesToday.toInt()), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                            Text("/ ${canonicalNumber(goal)} kcal", color = KaloriMuted, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Spacer(Modifier.width(18.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Kalan", color = KaloriMuted)
                        Text(canonicalNumber(remaining), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = KaloriText)
                        Text("kcal", color = KaloriMuted)
                        TextButton(onClick = onGoal, contentPadding = PaddingValues(0.dp)) { Text("Hedefi düzenle", color = KaloriGreen) }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CanonicalMacroTile("Protein", state.proteinToday.toInt(), proteinGoal, KaloriGreen, Modifier.weight(1f))
                CanonicalMacroTile("Karbonhidrat", state.carbsToday.toInt(), carbsGoal, KaloriBlue, Modifier.weight(1f))
                CanonicalMacroTile("Yağ", state.fatToday.toInt(), fatGoal, KaloriYellow, Modifier.weight(1f))
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFF0B2A20), border = BorderStroke(1.dp, Color(0xFF154D39))) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Eco, null, tint = KaloriGreen)
                    Spacer(Modifier.width(10.dp))
                    Text("Küçük adımlar büyük sonuçlar getirir.", color = Color(0xFFD7E6DF), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Button(
                onClick = onFood,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color(0xFF00140B)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Yemek Ekle", fontWeight = FontWeight.Bold)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CanonicalQuickButton("Fotoğraf", Icons.Filled.CameraAlt, onPhoto, Modifier.weight(1f))
                CanonicalQuickButton("Barkod", Icons.Filled.QrCodeScanner, onBarcode, Modifier.weight(1f))
                CanonicalQuickButton("Ses", Icons.Filled.Mic, onVoice, Modifier.weight(1f))
            }
        }
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WaterDrop, null, tint = KaloriWater, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Su", fontWeight = FontWeight.Bold)
                        Text("${state.waterMl} ml / 2500 ml", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(5.dp))
                        LinearProgressIndicator(
                            progress = { (state.waterMl / 2500f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = KaloriWater,
                            trackColor = Color(0xFF25332F)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    IconButton(onClick = onWaterMinus, enabled = state.waterMl > 0) { Text("−", style = MaterialTheme.typography.headlineSmall) }
                    IconButton(onClick = onWaterPlus) { Icon(Icons.Filled.AddCircle, "250 ml ekle", tint = KaloriGreen) }
                }
            }
        }
        if (state.todayEntries.isNotEmpty()) {
            item { CanonicalSectionHeader("Bugünkü Öğünler") }
            items(state.todayEntries.take(5), key = { "home-${it.id}" }) { entry ->
                CanonicalDiaryRow(entry = entry, onClick = { onEntry(entry) })
            }
        }
    }
}

@Composable
private fun CanonicalDiaryScreen(
    state: TrackerUiState,
    onAddFood: () -> Unit,
    onEntry: (CalorieEntry) -> Unit,
    onRepeat: (CalorieEntry) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val entries = state.entries.filter { it.dateText() == selectedDate.toString() }
    val total = entries.sumOf { it.calories }.toInt()
    val meals = listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık", "Öğün")

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Günlük", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("${canonicalNumber(total)} / ${canonicalNumber(state.calorieGoal)} kcal", color = KaloriMuted)
                }
                Button(onClick = onAddFood, colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("+ Yemek") }
            }
        }
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { selectedDate = selectedDate.minusDays(1) }) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
                    Text(selectedDate.format(DateTimeFormatter.ofPattern("d MMMM EEEE", Locale.forLanguageTag("tr-TR"))), fontWeight = FontWeight.Bold)
                    TextButton(onClick = { if (selectedDate < LocalDate.now()) selectedDate = selectedDate.plusDays(1) }, enabled = selectedDate < LocalDate.now()) { Text("›", style = MaterialTheme.typography.headlineMedium) }
                }
            }
        }
        if (entries.isEmpty()) {
            item {
                CanonicalEmptyCard("Bu gün için öğün kaydı yok", "Yemek ekleyerek günlüğünü oluşturmaya başla.", onAddFood)
            }
        } else {
            meals.forEach { meal ->
                val mealEntries = entries.filter { canonicalNormalizeMeal(it.mealType) == meal }
                if (mealEntries.isNotEmpty()) {
                    item { CanonicalSectionHeader(meal) }
                    items(mealEntries, key = { "diary-${it.id}" }) { entry -> CanonicalDiaryRow(entry, { onEntry(entry) }) }
                }
            }
        }
        if (state.recentFoods.isNotEmpty()) {
            item { CanonicalSectionHeader("Hızlı tekrar ekle") }
            items(state.recentFoods.take(5), key = { "repeat-${it.id}" }) { entry ->
                CanonicalNeonCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.foodName, fontWeight = FontWeight.SemiBold)
                            Text("${entry.grams.toInt()} g • ${entry.calories.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onRepeat(entry) }) { Text("+ Ekle", color = KaloriGreen) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanonicalFoodAddScreen(
    state: TrackerUiState,
    onSelect: (CatalogFood) -> Unit,
    onFavorite: (CatalogFood) -> Unit,
    onBarcode: () -> Unit,
    onPhoto: () -> Unit,
    onVoice: () -> Unit,
    onManual: () -> Unit,
    onRepeat: (CalorieEntry) -> Unit,
    onAddSavedMeal: (SavedMealRemote) -> Unit
) {
    var mode by remember { mutableStateOf(FoodAddMode.SEARCH) }
    var query by remember { mutableStateOf("") }
    val favorites = state.favorites.map { it.catalogFood() }
    val results = TurkishFoodCatalog.foods.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }.take(18)

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FoodAddMode.entries.forEach { item ->
                    val selected = mode == item
                    Surface(
                        modifier = Modifier.weight(1f).clickable { mode = item; if (item == FoodAddMode.BARCODE) onBarcode() },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) KaloriGreen else KaloriSurfaceAlt,
                        border = BorderStroke(1.dp, if (selected) KaloriGreen else KaloriBorder)
                    ) {
                        Text(item.label, modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center, color = if (selected) Color(0xFF00150A) else KaloriText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = KaloriMuted) },
                placeholder = { Text("Yiyecek ara (ör: tavuk, elma, süt...)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = canonicalTextFieldColors()
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CanonicalQuickButton("Fotoğraf", Icons.Filled.CameraAlt, onPhoto, Modifier.weight(1f))
                CanonicalQuickButton("Ses", Icons.Filled.Mic, onVoice, Modifier.weight(1f))
                CanonicalQuickButton("Manuel", Icons.Filled.Add, onManual, Modifier.weight(1f))
            }
        }
        if (mode == FoodAddMode.FAVORITES) {
            item { CanonicalSectionHeader("Favoriler") }
            if (favorites.isEmpty()) item { Text("Henüz favori yiyecek yok.", color = KaloriMuted) }
            else items(favorites, key = { "fav-${it.name}" }) { food ->
                CanonicalFoodResultRow(food, true, { onFavorite(food) }) { onSelect(food) }
            }
        } else {
            if (query.isBlank()) item { CanonicalSectionHeader("Popüler Yiyecekler") }
            items(results, key = { "food-${it.name}" }) { food ->
                CanonicalFoodResultRow(food, favorites.any { it.name.equals(food.name, true) }, { onFavorite(food) }) { onSelect(food) }
            }
        }
        if (query.isBlank() && state.recentFoods.isNotEmpty()) {
            item { CanonicalSectionHeader("Son Kullanılanlar") }
            items(state.recentFoods.take(5), key = { "recent-add-${it.id}" }) { entry ->
                CanonicalNeonCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.foodName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${entry.grams.toInt()} g • ${entry.calories.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onRepeat(entry) }) { Text("+ Ekle", color = KaloriGreen) }
                    }
                }
            }
        }
        if (query.isBlank() && state.savedMeals.isNotEmpty()) {
            item { CanonicalSectionHeader("Kayıtlı Öğünler") }
            items(state.savedMeals.take(8), key = { "saved-${it.id}" }) { meal ->
                CanonicalNeonCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.name, fontWeight = FontWeight.SemiBold)
                            Text("${meal.totalGrams.toInt()} g • ${meal.calories.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onAddSavedMeal(meal) }) { Text("+ Ekle", color = KaloriGreen) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanonicalFoodResultRow(food: CatalogFood, favorite: Boolean, onFavorite: () -> Unit, onSelect: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(58.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = Color(0xFF1B2925), modifier = Modifier.size(42.dp)) {
            Box(contentAlignment = Alignment.Center) { Text(canonicalFoodEmoji(food.name), style = MaterialTheme.typography.titleLarge) }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(food.name, color = KaloriText, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("100 g • ${food.calories100g.toInt()} kcal", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onFavorite) { Icon(if (favorite) Icons.Filled.Star else Icons.Filled.StarBorder, "Favori", tint = if (favorite) KaloriYellow else KaloriMuted) }
        IconButton(onClick = onSelect) { Icon(Icons.Filled.AddCircle, "Ekle", tint = KaloriGreen, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun CanonicalBarcodeScreen(
    state: TrackerUiState,
    onScan: () -> Unit,
    onManual: () -> Unit,
    onClearProduct: () -> Unit,
    onAddProduct: (BarcodeProduct, String, Double) -> Unit
) {
    val product = state.barcodeProduct
    var meal by remember(product?.barcode) { mutableStateOf("Öğün") }
    var grams by remember(product?.barcode) { mutableStateOf("100") }
    val gramsN = grams.toDoubleOrNull() ?: 0.0

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                Modifier.fillMaxWidth().aspectRatio(.86f).clip(RoundedCornerShape(24.dp)).background(Color(0xFF121D19)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(245.dp).background(Color(0xFF17211E)), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize().padding(14.dp)) {
                            val c = KaloriGreen
                            val l = 38.dp.toPx()
                            val w = 4.dp.toPx()
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(l, 0f), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, 0f), end = androidx.compose.ui.geometry.Offset(0f, l), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width - l, 0f), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width, 0f), end = androidx.compose.ui.geometry.Offset(size.width, l), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(l, size.height), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(0f, size.height), end = androidx.compose.ui.geometry.Offset(0f, size.height - l), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width, size.height), end = androidx.compose.ui.geometry.Offset(size.width - l, size.height), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(c, start = androidx.compose.ui.geometry.Offset(size.width, size.height), end = androidx.compose.ui.geometry.Offset(size.width, size.height - l), strokeWidth = w, cap = StrokeCap.Round)
                            drawLine(Color(0xFF79FFD0), start = androidx.compose.ui.geometry.Offset(0f, size.height * .5f), end = androidx.compose.ui.geometry.Offset(size.width, size.height * .5f), strokeWidth = 2.dp.toPx())
                        }
                        Icon(Icons.Filled.QrCodeScanner, null, tint = Color(0xFF789188), modifier = Modifier.size(92.dp))
                    }
                    Spacer(Modifier.height(20.dp))
                    Text("Barkodu kutu içine hizalayın", color = KaloriText, fontWeight = FontWeight.Bold)
                    Text("Google kod tarayıcı ile ürün bilgisi aranacak", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Button(
                onClick = onScan,
                enabled = !state.barcodeLoading,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
            ) {
                Icon(Icons.Filled.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.barcodeLoading) "Ürün aranıyor..." else "Barkodu Tara", fontWeight = FontWeight.Bold)
            }
        }
        item { OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) { Text("Barkod numarasını elle gir") } }

        product?.let { p ->
            item {
                CanonicalNeonCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                            if (p.brand.isNotBlank()) Text(p.brand, color = KaloriMuted)
                            Text("100 g • ${p.calories100g.toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
                            Text("P ${p.protein100g.toInt()} • K ${p.carbs100g.toInt()} • Y ${p.fat100g.toInt()}", color = KaloriMuted)
                        }
                        IconButton(onClick = onClearProduct) { Text("×", style = MaterialTheme.typography.headlineMedium) }
                    }
                    Spacer(Modifier.height(8.dp))
                    CanonicalMealSelector(meal) { meal = it }
                    Spacer(Modifier.height(8.dp))
                    CanonicalNumberField("Gram", grams) { grams = it }
                    if (gramsN > 0) {
                        Text("Eklenecek: ${(p.calories100g * gramsN / 100).toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        enabled = gramsN > 0,
                        onClick = { onAddProduct(p, meal, gramsN) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
                    ) { Text("Günlüğe ekle") }
                }
            }
        }
    }
}

@Composable
private fun CanonicalProgressScreen(state: TrackerUiState, onWeight: () -> Unit, onAddWeight: () -> Unit) {
    var range by remember { mutableStateOf(ProgressRange.WEEK) }
    val chart = canonicalChartData(state, range)
    val average = if (chart.isEmpty()) 0 else chart.map { it.second }.average().toInt()
    val adherence = if (chart.isEmpty()) 0 else (chart.count { it.second > 0 && abs(it.second - state.calorieGoal) <= state.calorieGoal * .10 } * 100 / chart.size.coerceAtLeast(1))
    val macroTotal = state.proteinToday + state.carbsToday + state.fatToday

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("İlerleme", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        item { CanonicalProgressSelector(range) { range = it } }
        item {
            CanonicalNeonCard {
                Text("Kalori Alımı", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                CanonicalBarChart(chart, state.calorieGoal)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CanonicalStatCard("Ortalama", "${canonicalNumber(average)} kcal", Icons.Filled.BarChart, Modifier.weight(1f))
                CanonicalStatCard("Hedefe Uyum", "%$adherence", Icons.Filled.TrackChanges, Modifier.weight(1f))
            }
        }
        item {
            CanonicalNeonCard {
                Text("Makro Dağılımı", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                val p = if (macroTotal > 0) (state.proteinToday / macroTotal).toFloat() else 0f
                val c = if (macroTotal > 0) (state.carbsToday / macroTotal).toFloat() else 0f
                val f = if (macroTotal > 0) (state.fatToday / macroTotal).toFloat() else 0f
                Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(8.dp))) {
                    if (p > 0f) Box(Modifier.weight(p).fillMaxHeight().background(KaloriGreen))
                    if (c > 0f) Box(Modifier.weight(c).fillMaxHeight().background(KaloriBlue))
                    if (f > 0f) Box(Modifier.weight(f).fillMaxHeight().background(KaloriYellow))
                    if (macroTotal <= 0) Box(Modifier.fillMaxSize().background(Color(0xFF24312D)))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Protein ${state.proteinToday.toInt()} g", color = KaloriGreen, style = MaterialTheme.typography.bodySmall)
                    Text("Karb ${state.carbsToday.toInt()} g", color = KaloriBlue, style = MaterialTheme.typography.bodySmall)
                    Text("Yağ ${state.fatToday.toInt()} g", color = KaloriYellow, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Kilo Takibi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(state.weights.firstOrNull()?.let { "Güncel ${canonicalWeight(it.weightKg)} kg" } ?: "Henüz kilo kaydı yok", color = KaloriMuted)
                    }
                    TextButton(onClick = onAddWeight) { Text("Ekle", color = KaloriGreen) }
                    IconButton(onClick = onWeight) { Icon(Icons.Filled.ChevronRight, null, tint = KaloriGreen) }
                }
            }
        }
    }
}

@Composable
private fun CanonicalProgressSelector(selected: ProgressRange, onSelected: (ProgressRange) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = KaloriSurfaceAlt, border = BorderStroke(1.dp, KaloriBorder)) {
        Row(Modifier.fillMaxWidth().padding(3.dp)) {
            ProgressRange.entries.forEach { range ->
                Surface(
                    modifier = Modifier.weight(1f).clickable { onSelected(range) },
                    shape = RoundedCornerShape(9.dp),
                    color = if (selected == range) KaloriGreen else Color.Transparent
                ) {
                    Text(range.label, modifier = Modifier.padding(vertical = 9.dp), textAlign = TextAlign.Center, color = if (selected == range) Color.Black else KaloriMuted, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CanonicalBarChart(data: List<Pair<String, Double>>, goal: Int) {
    if (data.isEmpty()) {
        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { Text("Henüz veri yok", color = KaloriMuted) }
        return
    }
    val max = maxOf(goal.toDouble(), data.maxOf { it.second }, 1.0)
    val display = if (data.size <= 8) data else data.filterIndexed { index, _ -> index % (data.size / 7).coerceAtLeast(1) == 0 }.takeLast(8)
    Row(Modifier.fillMaxWidth().height(185.dp), horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.Bottom) {
        display.forEach { (label, value) ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(value.toInt().toString(), color = KaloriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Box(
                    Modifier.fillMaxWidth().height(((value / max) * 125).toFloat().coerceAtLeast(5f).dp)
                        .clip(RoundedCornerShape(6.dp, 6.dp, 2.dp, 2.dp))
                        .background(if (value <= goal * 1.1) KaloriGreen else KaloriYellow)
                )
                Spacer(Modifier.height(5.dp))
                Text(label, color = KaloriMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CanonicalWeightScreen(state: TrackerUiState, onAddWeight: () -> Unit, onDeleteWeight: (WeightEntry) -> Unit) {
    val points = state.weights.take(18).reversed()
    val start = points.firstOrNull()?.weightKg
    val current = points.lastOrNull()?.weightKg
    val change = if (start != null && current != null) current - start else null

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Kilo değişimi", color = KaloriMuted, modifier = Modifier.weight(1f))
                TextButton(onClick = onAddWeight) { Text("Ekle", color = KaloriGreen, fontWeight = FontWeight.Bold) }
            }
        }
        item {
            CanonicalNeonCard {
                if (points.size < 2) {
                    Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                        Text("Grafik için en az iki kilo kaydı ekle", color = KaloriMuted)
                    }
                } else {
                    CanonicalWeightChart(points)
                    val latest = points.last()
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF0A3A24), border = BorderStroke(1.dp, KaloriGreen.copy(alpha = .5f))) {
                        Text("${canonicalWeight(latest.weightKg)} kg  •  ${latest.dateText()}", modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = KaloriText, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CanonicalValueCard("Başlangıç", start?.let { "${canonicalWeight(it)} kg" } ?: "—", Modifier.weight(1f))
                CanonicalValueCard("Güncel", current?.let { "${canonicalWeight(it)} kg" } ?: "—", Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CanonicalValueCard("Toplam Değişim", change?.let { "${if (it > 0) "+" else ""}${canonicalWeight(it)} kg" } ?: "—", Modifier.weight(1f), if (change != null && change <= 0) KaloriGreen else KaloriText)
                CanonicalValueCard("Hedef", "— kg", Modifier.weight(1f), KaloriYellow)
            }
        }
        if (state.weights.isNotEmpty()) {
            item { CanonicalSectionHeader("Kayıtlar") }
            items(state.weights.take(14), key = { "weight-${it.id}" }) { weight ->
                CanonicalNeonCard {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${canonicalWeight(weight.weightKg)} kg", fontWeight = FontWeight.Bold)
                            Text(weight.dateText(), color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { onDeleteWeight(weight) }) { Text("Sil", color = KaloriDanger) }
                    }
                }
            }
        }
    }
}

@Composable
private fun CanonicalWeightChart(points: List<WeightEntry>) {
    val min = points.minOf { it.weightKg }
    val max = points.maxOf { it.weightKg }
    val range = (max - min).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(220.dp).padding(8.dp)) {
        val stepX = if (points.size <= 1) size.width else size.width / (points.size - 1)
        val coordinates = points.mapIndexed { index, item ->
            val x = index * stepX
            val y = size.height - (((item.weightKg - min) / range).toFloat() * (size.height * .78f) + size.height * .1f)
            androidx.compose.ui.geometry.Offset(x, y)
        }
        val path = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            coordinates.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(path, color = KaloriGreen, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        coordinates.forEach { drawCircle(KaloriGreen, radius = 5.dp.toPx(), center = it) }
    }
}

@Composable
private fun CanonicalProfileScreen(
    state: TrackerUiState,
    onGoal: () -> Unit,
    onWeight: () -> Unit,
    onWellness: () -> Unit,
    onRecipeBuilder: () -> Unit,
    onPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    onDelete: () -> Unit
) {
    val name = canonicalDisplayName(state.email)
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 18.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Ayarlar", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF17372B), modifier = Modifier.size(58.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), color = KaloriGreen, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text(state.email, color = KaloriMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = KaloriMuted)
                }
            }
        }
        item { CanonicalSettingsRow(Icons.Filled.TrackChanges, "Hedeflerim", "Günlük ${canonicalNumber(state.calorieGoal)} kcal", onGoal) }
        item { CanonicalSettingsRow(Icons.Filled.MonitorWeight, "Kilo Takibi", state.weights.firstOrNull()?.let { "Güncel ${canonicalWeight(it.weightKg)} kg" } ?: "Kilo kaydı ekle", onWeight) }
        item { CanonicalSettingsRow(Icons.Filled.Eco, "Sağlık ve Hatırlatmalar", "Su, Health Connect ve öğün bildirimleri", onWellness) }
        item { CanonicalSettingsRow(Icons.Filled.RestaurantMenu, "Kayıtlı Öğünler", "${state.savedMeals.size} kayıtlı öğün • tarif oluştur", onRecipeBuilder) }
        item { CanonicalSettingsRow(Icons.Filled.Security, "Gizlilik Politikası", "Verilerin bizim için önemli", onPrivacy) }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF251314)), border = BorderStroke(1.dp, Color(0xFF4B2225)), shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().clickable(onClick = onDelete).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.DeleteForever, null, tint = KaloriDanger)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Verilerimi Sil", color = KaloriDanger, fontWeight = FontWeight.Bold)
                        Text("Tüm verilerin kalıcı olarak silinir", color = Color(0xFFFF8791), style = MaterialTheme.typography.bodySmall)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = KaloriDanger)
                }
            }
        }
        item { CanonicalSettingsRow(Icons.Filled.Logout, "Çıkış Yap", "Google hesabından çık", onSignOut) }
    }
}

@Composable
private fun CanonicalFoodDetailScreen(entry: CalorieEntry, onEdit: () -> Unit, onDelete: () -> Unit, onRepeat: () -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 6.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CanonicalNeonCard {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = Color(0xFF18352B), modifier = Modifier.size(62.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(canonicalFoodEmoji(entry.foodName), style = MaterialTheme.typography.headlineMedium) }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(entry.foodName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                        Text("${entry.mealType} • ${entry.dateText()}", color = KaloriMuted)
                    }
                    Text("${entry.calories.toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Black)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CanonicalMacroDetail("Protein", "${entry.proteinG.toInt()} g", KaloriGreen, Modifier.weight(1f))
                CanonicalMacroDetail("Karb.", "${entry.carbsG.toInt()} g", KaloriBlue, Modifier.weight(1f))
                CanonicalMacroDetail("Yağ", "${entry.fatG.toInt()} g", KaloriYellow, Modifier.weight(1f))
            }
        }
        item {
            CanonicalNeonCard {
                CanonicalDetailLine("Miktar", "${canonicalWeight(entry.grams)} g")
                HorizontalDivider(color = KaloriBorder)
                CanonicalDetailLine("Öğün", entry.mealType)
                HorizontalDivider(color = KaloriBorder)
                CanonicalDetailLine("Kaynak", canonicalSourceName(entry.source))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onRepeat, modifier = Modifier.weight(1f)) { Text("Tekrar Ekle") }
                Button(onClick = onEdit, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("Düzenle") }
            }
        }
        item { TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text("Bu kaydı sil", color = KaloriDanger) } }
    }
}

@Composable
private fun CanonicalBottomBar(selected: CanonicalTab, onSelect: (CanonicalTab) -> Unit) {
    NavigationBar(containerColor = Color(0xFF08110F), tonalElevation = 0.dp) {
        CanonicalTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelect(tab) },
                icon = { Icon(tab.icon, tab.label) },
                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = KaloriGreen,
                    selectedTextColor = KaloriGreen,
                    indicatorColor = Color(0xFF103626),
                    unselectedIconColor = Color(0xFFB6C0BC),
                    unselectedTextColor = Color(0xFFB6C0BC)
                )
            )
        }
    }
}

@Composable
private fun CanonicalNeonCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KaloriBorder),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0E1815))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) { content() }
    }
}

@Composable
private fun CanonicalMacroTile(label: String, current: Int, goal: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(13.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101B18)), border = BorderStroke(1.dp, KaloriBorder)) {
        Column(Modifier.padding(10.dp)) {
            Text(label, color = color, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Spacer(Modifier.height(5.dp))
            Text("$current", color = KaloriText, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            Text("/ $goal g", color = KaloriMuted, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun CanonicalQuickButton(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(76.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0F1B17),
        border = BorderStroke(1.dp, KaloriBorder)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, label, tint = KaloriGreen, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, color = KaloriText, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CanonicalDiaryRow(entry: CalorieEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF0E1815),
        border = BorderStroke(1.dp, KaloriBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Color(0xFF1A2924), modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(canonicalFoodEmoji(entry.foodName), style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.foodName, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${entry.grams.toInt()} g • P ${entry.proteinG.toInt()} • K ${entry.carbsG.toInt()} • Y ${entry.fatG.toInt()}", color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
            }
            Text("${entry.calories.toInt()} kcal", color = KaloriText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CanonicalSectionHeader(text: String) {
    Text(text, color = KaloriText, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun CanonicalStatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A17)), border = BorderStroke(1.dp, KaloriBorder)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
                Text(value, color = KaloriText, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleMedium)
            }
            Icon(icon, null, tint = KaloriGreen)
        }
    }
}

@Composable
private fun CanonicalValueCard(title: String, value: String, modifier: Modifier = Modifier, valueColor: Color = KaloriText) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1A17)), border = BorderStroke(1.dp, KaloriBorder)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(value, color = valueColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CanonicalSettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1816)),
        border = BorderStroke(1.dp, KaloriBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = KaloriText)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(subtitle, color = KaloriMuted, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = KaloriMuted)
        }
    }
}

@Composable
private fun CanonicalMacroDetail(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF101A17)), border = BorderStroke(1.dp, KaloriBorder)) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = color, style = MaterialTheme.typography.labelMedium)
            Text(value, color = KaloriText, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun CanonicalDetailLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
        Text(label, color = KaloriMuted, modifier = Modifier.weight(1f))
        Text(value, color = KaloriText, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CanonicalEmptyCard(title: String, subtitle: String, onAction: () -> Unit) {
    CanonicalNeonCard {
        Icon(Icons.Filled.RestaurantMenu, null, tint = KaloriMuted, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Text(subtitle, color = KaloriMuted)
        Spacer(Modifier.height(10.dp))
        Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("Yemek Ekle") }
    }
}

@Composable
private fun CanonicalCatalogAmountDialog(food: CatalogFood, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) {
    var grams by remember(food.name) { mutableStateOf("100") }
    var meal by remember(food.name) { mutableStateOf("Öğün") }
    val g = grams.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        titleContentColor = KaloriText,
        textContentColor = KaloriText,
        title = { Text(food.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CanonicalMealSelector(meal) { meal = it }
                CanonicalNumberField("Gram", grams) { grams = it }
                Text("Yaklaşık ${(food.calories100g * g / 100).toInt()} kcal", color = KaloriGreen, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { Button(enabled = g > 0, onClick = { onSave(meal, g) }, colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("Günlüğe ekle") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal", color = KaloriMuted) } }
    )
}

@Composable
private fun CanonicalManualFoodDialog(onDismiss: () -> Unit, onSave: (String, String, Double, Double, Double, Double, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var meal by remember { mutableStateOf("Öğün") }
    var grams by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        title = { Text("Manuel Yemek") },
        text = {
            LazyColumn(Modifier.heightIn(max = 450.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Yemek adı") }, singleLine = true, colors = canonicalTextFieldColors()) }
                item { CanonicalMealSelector(meal) { meal = it } }
                item { CanonicalNumberField("Gram", grams) { grams = it } }
                item { CanonicalNumberField("Kalori", calories) { calories = it } }
                item { CanonicalNumberField("Protein g", protein) { protein = it } }
                item { CanonicalNumberField("Karbonhidrat g", carbs) { carbs = it } }
                item { CanonicalNumberField("Yağ g", fat) { fat = it } }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && grams.toDoubleOrNull() != null && calories.toDoubleOrNull() != null,
                onClick = { onSave(name, meal, grams.toDoubleOrNull() ?: 0.0, calories.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
            ) { Text("Kaydet") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun CanonicalEditEntryDialog(entry: CalorieEntry, onDismiss: () -> Unit, onSave: (String, String, Double, Double, Double, Double, Double) -> Unit) {
    var name by remember(entry.id) { mutableStateOf(entry.foodName) }
    var meal by remember(entry.id) { mutableStateOf(entry.mealType) }
    var grams by remember(entry.id) { mutableStateOf(entry.grams.toString()) }
    var calories by remember(entry.id) { mutableStateOf(entry.calories.toString()) }
    var protein by remember(entry.id) { mutableStateOf(entry.proteinG.toString()) }
    var carbs by remember(entry.id) { mutableStateOf(entry.carbsG.toString()) }
    var fat by remember(entry.id) { mutableStateOf(entry.fatG.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        title = { Text("Yemek Detayı Düzenle") },
        text = {
            LazyColumn(Modifier.heightIn(max = 450.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, label = { Text("Yemek adı") }, singleLine = true, colors = canonicalTextFieldColors()) }
                item { CanonicalMealSelector(meal) { meal = it } }
                item { CanonicalNumberField("Gram", grams) { grams = it } }
                item { CanonicalNumberField("Kalori", calories) { calories = it } }
                item { CanonicalNumberField("Protein g", protein) { protein = it } }
                item { CanonicalNumberField("Karbonhidrat g", carbs) { carbs = it } }
                item { CanonicalNumberField("Yağ g", fat) { fat = it } }
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && grams.toDoubleOrNull() != null && calories.toDoubleOrNull() != null,
                onClick = { onSave(name, meal, grams.toDoubleOrNull() ?: 0.0, calories.toDoubleOrNull() ?: 0.0, protein.toDoubleOrNull() ?: 0.0, carbs.toDoubleOrNull() ?: 0.0, fat.toDoubleOrNull() ?: 0.0) },
                colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)
            ) { Text("Güncelle") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun CanonicalMealSelector(selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Öğün", color = KaloriMuted, style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(listOf("Kahvaltı", "Öğle", "Akşam", "Atıştırmalık", "Öğün")) { meal ->
                FilterChip(selected = selected == meal, onClick = { onSelected(meal) }, label = { Text(meal, style = MaterialTheme.typography.labelSmall) })
            }
        }
    }
}

@Composable
private fun CanonicalNumberDialog(title: String, suffix: String, onDismiss: () -> Unit, onSave: (Double) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        title = { Text(title) },
        text = { CanonicalNumberField(suffix, value) { value = it } },
        confirmButton = { Button(enabled = value.toDoubleOrNull() != null, onClick = { value.toDoubleOrNull()?.let(onSave) }, colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("Kaydet") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun CanonicalBarcodeInputDialog(onDismiss: () -> Unit, onSearch: (String) -> Unit) {
    var barcode by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        title = { Text("Barkod numarası") },
        text = {
            OutlinedTextField(
                barcode,
                { barcode = it.filter(Char::isDigit) },
                label = { Text("EAN / UPC") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = canonicalTextFieldColors()
            )
        },
        confirmButton = { Button(enabled = barcode.length in 8..14, onClick = { onSearch(barcode) }, colors = ButtonDefaults.buttonColors(containerColor = KaloriGreen, contentColor = Color.Black)) { Text("Ürünü bul") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}

@Composable
private fun CanonicalDeleteDialog(title: String, detail: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KaloriDialog,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(detail, color = KaloriMuted) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = KaloriDanger, contentColor = Color.White)) { Text("Sil") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Vazgeç") } }
    )
}

@Composable
private fun CanonicalNumberField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValue(raw.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')) },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = canonicalTextFieldColors()
    )
}

@Composable
private fun CanonicalSegmentedChoice(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            Surface(
                modifier = Modifier.weight(1f).clickable { onSelect(option) },
                shape = RoundedCornerShape(12.dp),
                color = if (selected == option) KaloriGreen else KaloriSurfaceAlt,
                border = BorderStroke(1.dp, if (selected == option) KaloriGreen else KaloriBorder)
            ) {
                Text(option, modifier = Modifier.padding(vertical = 11.dp), textAlign = TextAlign.Center, color = if (selected == option) Color.Black else KaloriText, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CanonicalCenterState(title: String, subtitle: String) {
    Box(Modifier.fillMaxSize().background(KaloriBackground), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = KaloriGreen)
            Spacer(Modifier.height(14.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(subtitle, color = KaloriMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun canonicalTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Color(0xFF131C1A),
    unfocusedContainerColor = Color(0xFF131C1A),
    disabledContainerColor = Color(0xFF131C1A),
    focusedBorderColor = KaloriGreen,
    unfocusedBorderColor = KaloriBorder,
    focusedTextColor = KaloriText,
    unfocusedTextColor = KaloriText,
    focusedPlaceholderColor = KaloriMuted,
    unfocusedPlaceholderColor = KaloriMuted,
    focusedLabelColor = KaloriGreen,
    unfocusedLabelColor = KaloriMuted
)

private fun canonicalPageTitle(page: CanonicalPage): String = when (page) {
    CanonicalPage.FOOD_ADD -> "Yemek Ekle"
    CanonicalPage.BARCODE -> "Barkod Tara"
    CanonicalPage.WEIGHT -> "Kilo Takibi"
    CanonicalPage.FOOD_DETAIL -> "Yemek Detayı"
    CanonicalPage.ROOT -> "Kalori Takip"
}

private fun canonicalDisplayName(email: String): String {
    val raw = email.substringBefore('@').replace('.', ' ').replace('_', ' ').trim()
    return raw.split(' ').filter { it.isNotBlank() }.joinToString(" ") { part -> part.replaceFirstChar { c -> c.titlecase(Locale.forLanguageTag("tr-TR")) } }.ifBlank { "Kullanıcı" }
}

private fun canonicalToday(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale.forLanguageTag("tr-TR")))
private fun canonicalNumber(value: Int): String = String.format(Locale.forLanguageTag("tr-TR"), "%,d", value)
private fun canonicalWeight(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun canonicalNormalizeMeal(value: String): String {
    val v = value.trim().lowercase(Locale.forLanguageTag("tr-TR"))
    return when {
        "kahvalt" in v -> "Kahvaltı"
        "öğle" in v || "ogle" in v -> "Öğle"
        "akşam" in v || "aksam" in v -> "Akşam"
        "atıştır" in v || "atistir" in v -> "Atıştırmalık"
        else -> "Öğün"
    }
}

private fun canonicalFoodEmoji(name: String): String {
    val v = name.lowercase(Locale.forLanguageTag("tr-TR"))
    return when {
        "tavuk" in v -> "🍗"
        "yumurta" in v -> "🥚"
        "muz" in v -> "🍌"
        "elma" in v -> "🍎"
        "portakal" in v -> "🍊"
        "yulaf" in v -> "🥣"
        "ekmek" in v || "simit" in v || "poğaça" in v -> "🥖"
        "yoğurt" in v || "ayran" in v -> "🥛"
        "peynir" in v -> "🧀"
        "pilav" in v || "bulgur" in v -> "🍚"
        "çorba" in v -> "🥣"
        "balık" in v || "ton" in v -> "🐟"
        else -> "🍽️"
    }
}

private fun canonicalSourceName(source: String): String = when {
    source.startsWith("open_food_facts") -> "Barkod"
    source.contains("photo", true) || source.contains("ai", true) -> "AI Fotoğraf"
    source.contains("voice", true) -> "Ses"
    source.contains("catalog", true) -> "Yiyecek Kataloğu"
    source.contains("saved", true) -> "Kayıtlı Öğün"
    source.contains("recent", true) -> "Tekrar Ekle"
    else -> "Manuel"
}

private fun canonicalChartData(state: TrackerUiState, range: ProgressRange): List<Pair<String, Double>> {
    val tr = Locale.forLanguageTag("tr-TR")
    return when (range) {
        ProgressRange.DAY -> listOf("Bugün" to state.caloriesToday)
        ProgressRange.WEEK -> state.dailyTotals(7).map { it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, tr).take(3) to it.calories }
        ProgressRange.MONTH -> state.dailyTotals(30).map { it.date.dayOfMonth.toString() to it.calories }
        ProgressRange.YEAR -> {
            val now = YearMonth.now()
            (11 downTo 0).map { offset ->
                val month = now.minusMonths(offset.toLong())
                val calories = state.entries.filter {
                    runCatching { YearMonth.from(LocalDate.parse(it.dateText())) == month }.getOrDefault(false)
                }.sumOf { it.calories }
                month.month.getDisplayName(TextStyle.SHORT, tr).take(3) to calories
            }
        }
    }
}
