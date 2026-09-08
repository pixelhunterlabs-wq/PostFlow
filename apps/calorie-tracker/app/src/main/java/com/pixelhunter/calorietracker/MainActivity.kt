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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.android.gms.mlkit.vision.codescanner.GmsBarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
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

@Serializable data class DailyWater(
    val id: String = UUID.randomUUID().toString(), @SerialName("user_id") val userId: String,
    @SerialName("target_date") val targetDate: String, @SerialName("water_ml") val waterMl: Int = 0
)
@Serializable data class FavoriteFood(
    val id: String = UUID.randomUUID().toString(), @SerialName("user_id") val userId: String,
    @SerialName("food_name") val foodName: String, val grams: Double = 100.0, val calories: Double = 0.0,
    @SerialName("protein_g") val proteinG: Double = 0.0, @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0, @SerialName("meal_type") val mealType: String = "Öğün",
    val source: String = "favorite"
) {
    fun catalogFood() = CatalogFood(foodName, calories, proteinG, carbsG, fatG)
}
@Serializable data class SavedMealRemote(
    val id: String = UUID.randomUUID().toString(), @SerialName("user_id") val userId: String,
    val name: String, @SerialName("total_grams") val totalGrams: Double, val calories: Double,
    @SerialName("protein_g") val proteinG: Double, @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double
)
@Serializable data class SavedMealItemRemote(
    @SerialName("saved_meal_id") val savedMealId: String, @SerialName("user_id") val userId: String,
    @SerialName("food_name") val foodName: String, val grams: Double, val calories: Double,
    @SerialName("protein_g") val proteinG: Double, @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double
)

data class BarcodeProduct(
    val barcode: String,
    val name: String,
    val brand: String,
    val calories100g: Double,
    val protein100g: Double,
    val carbs100g: Double,
    val fat100g: Double
)

data class DailyTotal(val date: LocalDate, val calories: Double)

object SupabaseProvider {
    const val oauthRedirectUrl = "calorietracker://login"

    val configured: Boolean
        get() = BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) null else createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
            install(Auth) {
                scheme = "calorietracker"
                host = "login"
            }
            install(Postgrest)
            install(Functions)
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
    val waterMl: Int = 0,
    val favorites: List<FavoriteFood> = emptyList(),
    val savedMeals: List<SavedMealRemote> = emptyList(),
    val editingSavedMealItems: List<SavedMealItemRemote> = emptyList(),
    val barcodeLoading: Boolean = false,
    val barcodeProduct: BarcodeProduct? = null,
    val onboardingCompleted: Boolean = false,
    val accountDeleting: Boolean = false,
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

    fun dailyTotals(days: Int): List<DailyTotal> = (days - 1 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        DailyTotal(date, entries.filter { it.dateText() == date.toString() }.sumOf { it.calories })
    }

    val monthCalories: Double
        get() {
            val month = YearMonth.from(today)
            return entries.filter {
                runCatching { YearMonth.from(LocalDate.parse(it.dateText())) == month }.getOrDefault(false)
            }.sumOf { it.calories }
        }

    val monthAverage: Int
        get() = (monthCalories / today.dayOfMonth.coerceAtLeast(1)).toInt()

    val recentFoods: List<CalorieEntry>
        get() = entries.distinctBy { it.foodName.trim().lowercase() }.take(6)
}

class TrackerViewModel : ViewModel() {
    var uiState by mutableStateOf(TrackerUiState())
        private set

    private val supabase get() = SupabaseProvider.client
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    init { refreshSessionAndData() }

    fun consumeMessage() { uiState = uiState.copy(message = null) }
    fun showMessage(message: String) { uiState = uiState.copy(message = message) }
    fun clearBarcodeProduct() { uiState = uiState.copy(barcodeProduct = null) }

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
                // Clear any previous account before rendering/loading this account.
                uiState = TrackerUiState(loading = true, signedIn = true, email = user.email.orEmpty())
                ensureProfile(user.id, user.email.orEmpty())
                loadAccountData(user.id)
            }.onFailure { uiState = uiState.copy(loading = false, message = it.message ?: "Veriler yüklenemedi") }
        }
    }

    fun signInWithGoogle() {
        val client = supabase ?: return
        viewModelScope.launch {
            // signInWith returns as soon as the browser/custom tab is opened. Keep the
            // button disabled until the callback is handled, so duplicate OAuth flows
            // cannot be started from the login screen.
            uiState = uiState.copy(loading = true, message = null)
            runCatching {
                client.auth.signInWith(Google, redirectUrl = SupabaseProvider.oauthRedirectUrl)
            }.onFailure {
                uiState = uiState.copy(
                    loading = false,
                    message = it.message ?: "Google girişi başlatılamadı"
                )
            }
        }
    }

    fun signOut() {
        val client = supabase ?: return
        viewModelScope.launch {
            uiState = TrackerUiState()
            runCatching { client.auth.signOut() }
        }
    }

    fun deleteAccount() {
        val client = supabase ?: return
        viewModelScope.launch {
            uiState = uiState.copy(accountDeleting = true)
            runCatching {
                client.functions.invoke("delete-calorie-account")
            }.onSuccess {
                runCatching { client.auth.signOut() }
                uiState = TrackerUiState(message = "Hesabın ve Kalori Takip verilerin silindi")
            }.onFailure {
                uiState = uiState.copy(accountDeleting = false, message = it.message ?: "Hesap silinemedi")
            }
        }
    }

    private suspend fun ensureProfile(userId: String, email: String) {
        val client = supabase ?: return
        val current = client.from("calorie_profiles").select().decodeList<CalorieProfile>().firstOrNull()
        if (current == null) {
            client.from("calorie_profiles").insert(CalorieProfile(userId = userId, email = email, dailyCalorieTarget = 2000))
            uiState = uiState.copy(calorieGoal = 2000)
        } else uiState = uiState.copy(calorieGoal = current.dailyCalorieTarget ?: 2000, onboardingCompleted = current.displayName == "onboarded")
    }

    private suspend fun loadAll() {
        val client = supabase ?: return
        val entries = client.from("calorie_food_entries").select().decodeList<CalorieEntry>().sortedByDescending { it.eatenAt }
        val weights = client.from("calorie_weight_entries").select().decodeList<WeightEntry>().sortedByDescending { it.measuredAt }
        uiState = uiState.copy(entries = entries, weights = weights, loading = false)
    }

    fun lookupBarcode(barcode: String) {
        val clean = BarcodeSupport.normalize(barcode)
        if (clean == null) {
            uiState = uiState.copy(message = "Geçerli bir ürün barkodu okunamadı")
            return
        }
        viewModelScope.launch {
            uiState = uiState.copy(barcodeLoading = true, barcodeProduct = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val fields = "product_name,brands,nutriments"
                    val url = URL("https://world.openfoodfacts.org/api/v2/product/$clean.json?fields=$fields")
                    val connection = (url.openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 10_000
                        readTimeout = 10_000
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("User-Agent", "KaloriTakip-Android/1.0 barcode-scan")
                    }
                    try {
                        if (connection.responseCode !in 200..299) error("Ürün servisine ulaşılamadı")
                        BarcodeSupport.parseOpenFoodFacts(clean, connection.inputStream.bufferedReader().use { it.readText() })
                    } finally { connection.disconnect() }
                }
            }.onSuccess { product ->
                if (product == null) {
                    uiState = uiState.copy(barcodeLoading = false, message = "Ürün Open Food Facts veritabanında bulunamadı")
                    return@onSuccess
                }
                uiState = uiState.copy(
                    barcodeLoading = false,
                    barcodeProduct = product
                )
            }.onFailure { uiState = uiState.copy(barcodeLoading = false, message = it.message ?: "Barkod ürünü alınamadı") }
        }
    }

    fun addBarcodeFood(product: BarcodeProduct, meal: String, grams: Double) {
        val ratio = grams / 100.0
        addFood(product.name, meal, grams, product.calories100g * ratio, product.protein100g * ratio, product.carbs100g * ratio, product.fat100g * ratio, "open_food_facts:${product.barcode}")
        clearBarcodeProduct()
    }

    fun addRecentFood(entry: CalorieEntry) {
        addFood(entry.foodName, entry.mealType, entry.grams, entry.calories, entry.proteinG, entry.carbsG, entry.fatG, "recent")
    }

    fun addFood(name: String, meal: String, grams: Double, calories: Double, protein: Double, carbs: Double, fat: Double, source: String = "manual") {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                client.from("calorie_food_entries").insert(
                    CalorieEntry(userId = user.id, foodName = name.trim(), mealType = meal.trim().ifBlank { "Öğün" }, grams = grams, calories = calories, proteinG = protein, carbsG = carbs, fatG = fat, source = source)
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
                    set("food_name", name.trim()); set("meal_type", meal.trim().ifBlank { "Öğün" }); set("grams", grams); set("calories", calories); set("protein_g", protein); set("carbs_g", carbs); set("fat_g", fat)
                }) { filter { eq("id", entry.id) } }
                loadAll(); uiState = uiState.copy(message = "Yemek kaydı güncellendi")
            }.onFailure { uiState = uiState.copy(message = "Öğün güncellenemedi. Lütfen tekrar deneyin.") }
        }
    }

    fun deleteFood(entryId: String) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
                client.from("calorie_food_entries").delete { filter { eq("id", entryId); eq("user_id", user.id) } }
                loadAll()
                if (uiState.entries.any { it.id == entryId }) error("delete_not_confirmed")
                uiState = uiState.copy(message = "Yemek kaydı silindi")
            }
                .onFailure { uiState = uiState.copy(message = "Öğün silinemedi. Lütfen tekrar deneyin.") }
        }
    }

    private suspend fun loadAccountData(userId: String) {
        val client = supabase ?: return
        loadAll()
        val today = LocalDate.now().toString()
        val water = runCatching { client.from("calorie_daily_water").select { filter { eq("user_id", userId); eq("target_date", today) } }
            .decodeList<DailyWater>().firstOrNull()?.waterMl ?: 0 }.getOrDefault(0)
        val favorites = runCatching { client.from("calorie_favorite_foods").select { filter { eq("user_id", userId) } }.decodeList<FavoriteFood>() }.getOrDefault(emptyList())
        val meals = runCatching { client.from("calorie_saved_meals").select { filter { eq("user_id", userId) } }.decodeList<SavedMealRemote>() }.getOrDefault(emptyList())
        uiState = uiState.copy(waterMl = water, favorites = favorites, savedMeals = meals, barcodeProduct = null, loading = false)
    }

    fun changeWater(deltaMl: Int) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val next = (uiState.waterMl + deltaMl).coerceIn(0, 6000)
            client.from("calorie_daily_water").upsert(DailyWater(userId = user.id, targetDate = LocalDate.now().toString(), waterMl = next)) { onConflict = "user_id,target_date" }
            uiState = uiState.copy(waterMl = next)
        }.onFailure { uiState = uiState.copy(message = "Su bilgisi güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun toggleFavorite(food: CatalogFood) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val existing = uiState.favorites.firstOrNull { it.foodName.equals(food.name, true) }
            if (existing == null) client.from("calorie_favorite_foods").insert(FavoriteFood(userId = user.id, foodName = food.name, calories = food.calories100g, proteinG = food.protein100g, carbsG = food.carbs100g, fatG = food.fat100g))
            else client.from("calorie_favorite_foods").delete { filter { eq("id", existing.id); eq("user_id", user.id) } }
            loadAccountData(user.id)
        }.onFailure { uiState = uiState.copy(message = "Favoriler güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun saveMeal(name: String, items: List<RecipeIngredient>) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val id = UUID.randomUUID().toString()
            val mapped = items.map { ingredient ->
                val ratio = ingredient.grams / 100.0
                SavedMealItemRemote(id, user.id, ingredient.food.name, ingredient.grams,
                    ingredient.food.calories100g * ratio, ingredient.food.protein100g * ratio,
                    ingredient.food.carbs100g * ratio, ingredient.food.fat100g * ratio)
            }
            client.from("calorie_saved_meals").insert(SavedMealRemote(id, user.id, name.trim(), mapped.sumOf { it.grams }, mapped.sumOf { it.calories }, mapped.sumOf { it.proteinG }, mapped.sumOf { it.carbsG }, mapped.sumOf { it.fatG }))
            client.from("calorie_saved_meal_items").insert(mapped)
            loadAccountData(user.id)
        }.onFailure { uiState = uiState.copy(message = "Kayıtlı öğünler güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun deleteSavedMeal(id: String) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            client.from("calorie_saved_meals").delete { filter { eq("id", id); eq("user_id", user.id) } }
            loadAccountData(user.id)
        }.onFailure { uiState = uiState.copy(message = "Kayıtlı öğün güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun loadSavedMealItems(id: String) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val items = client.from("calorie_saved_meal_items").select { filter { eq("saved_meal_id", id); eq("user_id", user.id) } }.decodeList<SavedMealItemRemote>()
            uiState = uiState.copy(editingSavedMealItems = items)
        }.onFailure { uiState = uiState.copy(message = "Kayıtlı öğün güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun updateSavedMeal(meal: SavedMealRemote, name: String, items: List<SavedMealItemRemote>) {
        val client = supabase ?: return
        if (name.isBlank() || items.isEmpty() || items.any { !isValidSavedMealItem(it) }) { uiState = uiState.copy(message = "Kayıtlı öğün güncellenemedi. Lütfen tekrar deneyin."); return }
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val total = savedMealTotals(items)
            client.from("calorie_saved_meals").update({ set("name", name.trim()); set("total_grams", total.grams); set("calories", total.calories); set("protein_g", total.protein); set("carbs_g", total.carbs); set("fat_g", total.fat) }) { filter { eq("id", meal.id); eq("user_id", user.id) } }
            client.from("calorie_saved_meal_items").delete { filter { eq("saved_meal_id", meal.id); eq("user_id", user.id) } }
            client.from("calorie_saved_meal_items").insert(items.map { it.copy(savedMealId = meal.id, userId = user.id) })
            loadAccountData(user.id)
        }.onFailure { uiState = uiState.copy(message = "Kayıtlı öğün güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun addSavedMealToDiary(id: String, mealType: String) {
        val client = supabase ?: return
        viewModelScope.launch { runCatching {
            val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
            val items = client.from("calorie_saved_meal_items").select { filter { eq("saved_meal_id", id); eq("user_id", user.id) } }.decodeList<SavedMealItemRemote>()
            if (items.isEmpty()) error("empty_saved_meal")
            items.forEach { item -> client.from("calorie_food_entries").insert(CalorieEntry(userId = user.id, foodName = item.foodName, grams = item.grams, calories = item.calories, proteinG = item.proteinG, carbsG = item.carbsG, fatG = item.fatG, mealType = mealType, source = "saved_food")) }
            loadAll()
            uiState = uiState.copy(message = "Öğün günlüğe eklendi.")
        }.onFailure { uiState = uiState.copy(message = "Kayıtlı öğün güncellenemedi. Lütfen tekrar deneyin.") } }
    }

    fun completeOnboarding(calories: Int, weight: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
                val current = client.from("calorie_profiles").select().decodeList<CalorieProfile>().firstOrNull()
                client.from("calorie_profiles").upsert((current ?: CalorieProfile(userId = user.id, email = user.email)).copy(displayName = "onboarded", dailyCalorieTarget = calories))
                if (weight > 0) client.from("calorie_weight_entries").insert(WeightEntry(userId = user.id, weightKg = weight))
                loadAll(); uiState = uiState.copy(onboardingCompleted = true, message = "Profilin kaydedildi")
            }.onFailure { uiState = uiState.copy(message = "Profil kaydedilemedi. Lütfen tekrar deneyin.") }
        }
    }

    fun addWeight(weightKg: Double) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching { client.from("calorie_weight_entries").insert(WeightEntry(userId = user.id, weightKg = weightKg)); loadAll(); uiState = uiState.copy(message = "Kilo kaydı eklendi") }
                .onFailure { uiState = uiState.copy(message = it.message ?: "Kilo kaydı eklenemedi") }
        }
    }

    fun deleteWeight(entryId: String) {
        val client = supabase ?: return
        viewModelScope.launch {
            runCatching {
                val user = client.auth.currentUserOrNull() ?: error("Oturum bulunamadı")
                client.from("calorie_weight_entries").delete {
                    filter { eq("id", entryId); eq("user_id", user.id) }
                }
                loadAll()
                if (uiState.weights.any { it.id == entryId }) error("delete_not_confirmed")
                uiState = uiState.copy(message = "Kilo kaydı silindi")
            }.onFailure {
                uiState = uiState.copy(message = "Kilo kaydı silinemedi. Lütfen tekrar deneyin.")
            }
        }
    }

    fun updateGoal(goal: Int) {
        val client = supabase ?: return
        viewModelScope.launch {
            val user = client.auth.currentUserOrNull() ?: return@launch
            runCatching {
                val existing = client.from("calorie_profiles").select().decodeList<CalorieProfile>().firstOrNull()
                client.from("calorie_profiles").upsert((existing ?: CalorieProfile(userId = user.id, email = user.email)).copy(dailyCalorieTarget = goal))
                uiState = uiState.copy(calorieGoal = goal, message = "Günlük hedef güncellendi")
            }.onFailure { uiState = uiState.copy(message = it.message ?: "Hedef güncellenemedi") }
        }
    }
}

class MainActivity : ComponentActivity() {
    private var authCallbackTick by mutableIntStateOf(0)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(); SupabaseProvider.client?.handleDeeplinks(intent)
        setContent { MaterialTheme { TrackerApp(authRefreshKey = authCallbackTick) } }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); SupabaseProvider.client?.handleDeeplinks(intent); authCallbackTick++
    }
}

@Composable
fun TrackerApp(authRefreshKey: Int = 0, vm: TrackerViewModel = viewModel()) {
    val state = vm.uiState
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var pendingDeleteEntry by remember { mutableStateOf<CalorieEntry?>(null) }
    var showWeightDialog by remember { mutableStateOf(false) }
    var pendingDeleteWeight by remember { mutableStateOf<WeightEntry?>(null) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showManualBarcode by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }

    val scannerOptions = remember {
        GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8, Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E).enableAutoZoom().build()
    }
    val barcodeScanner = remember(context) { GmsBarcodeScanning.getClient(context, scannerOptions) }

    LaunchedEffect(authRefreshKey) { vm.refreshSessionAndData() }
    LaunchedEffect(state.message) { state.message?.let { snackbarHostState.showSnackbar(it); vm.consumeMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Kalori Takip") }, actions = { if (state.signedIn) TextButton(onClick = vm::signOut) { Text("Çıkış") } }) },
        floatingActionButton = { if (state.signedIn && !state.accountDeleting) FloatingActionButton(onClick = { editingEntry = null; showFoodDialog = true }) { Text("+") } }
    ) { padding ->
        when {
            !SupabaseProvider.configured -> Box(Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) { Text("Supabase bağlantısı ayarlanmamış.") }
            !state.signedIn -> Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Kalorini ve kilo değişimini tek yerde takip et.", style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(24.dp)); Button(onClick = vm::signInWithGoogle) { Text("Google ile devam et") }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.loading || state.barcodeLoading || state.accountDeleting) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                item { Text(state.email, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(8.dp)); SummaryCard(state) { showGoalDialog = true } }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { StatCard("7 gün ort.", "${state.averageSince(7)} kcal", Modifier.weight(1f)); StatCard("30 gün ort.", "${state.averageSince(30)} kcal", Modifier.weight(1f)) } }
                item { MonthSummaryCard(state) }
                item { SevenDayCard(state) }
                if (state.recentFoods.isNotEmpty()) item { RecentFoodsCard(state.recentFoods, vm::addRecentFood) }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { editingEntry = null; showFoodDialog = true }, modifier = Modifier.weight(1f)) { Text("Yemek ekle") }; OutlinedButton(onClick = { showWeightDialog = true }, modifier = Modifier.weight(1f)) { Text("Kilo ekle") } } }
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilledTonalButton(enabled = !state.barcodeLoading, onClick = { barcodeScanner.startScan().addOnSuccessListener { it.rawValue?.let(vm::lookupBarcode) }.addOnFailureListener { showManualBarcode = true } }, modifier = Modifier.weight(1f)) { Text("Barkod tara") }; OutlinedButton(onClick = { showManualBarcode = true }, modifier = Modifier.weight(1f)) { Text("Barkod yaz") } } }
                item { Text("Bugünkü kayıtlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (state.todayEntries.isEmpty()) item { Text("Henüz yemek kaydı yok.") } else items(state.todayEntries, key = { it.id }) { entry -> EntryRow(entry, onEdit = { editingEntry = entry; showFoodDialog = true }, onDelete = { pendingDeleteEntry = entry }) }
                item { Text("Son 30 gün geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                items(state.entries.filter { runCatching { LocalDate.parse(it.dateText()) >= LocalDate.now().minusDays(29) }.getOrDefault(false) }.take(40), key = { "history-${it.id}" }) { entry -> EntryRow(entry, true, { editingEntry = entry; showFoodDialog = true }, { pendingDeleteEntry = entry }) }
                item { Text("Kilo geçmişi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (state.weights.isEmpty()) item { Text("Henüz kilo kaydı yok.") } else items(state.weights.take(20), key = { it.id }) { w -> ListItem(headlineContent = { Text("${w.weightKg} kg") }, supportingContent = { Text(w.dateText()) }, trailingContent = { TextButton(onClick = { pendingDeleteWeight = w }) { Text("Sil") } }) }
                item { HorizontalDivider(); TextButton(enabled = !state.accountDeleting, onClick = { showDeleteAccount = true }, modifier = Modifier.fillMaxWidth()) { Text("Hesabımı ve verilerimi sil") } }
            }
        }
    }

    if (showFoodDialog) FoodDialog(editingEntry, { showFoodDialog = false; editingEntry = null }) { a,b,c,d,e,f,g -> val edit = editingEntry; if (edit == null) vm.addFood(a,b,c,d,e,f,g) else vm.updateFood(edit,a,b,c,d,e,f,g); showFoodDialog = false; editingEntry = null }
    if (showManualBarcode) BarcodeNumberDialog({ showManualBarcode = false }) { vm.lookupBarcode(it); showManualBarcode = false }
    state.barcodeProduct?.let { product -> BarcodeProductDialog(product, vm::clearBarcodeProduct) { meal, grams -> vm.addBarcodeFood(product, meal, grams) } }
    if (showWeightDialog) NumberDialog("Kilo ekle", "kg", { showWeightDialog = false }) { vm.addWeight(it); showWeightDialog = false }
    if (showGoalDialog) NumberDialog("Günlük kalori hedefi", "kcal", { showGoalDialog = false }) { vm.updateGoal(it.toInt()); showGoalDialog = false }
    pendingDeleteEntry?.let { entry -> ConfirmDeleteDialog("Yemek kaydı silinsin mi?", entry.foodName, { pendingDeleteEntry = null }) { vm.deleteFood(entry.id); pendingDeleteEntry = null } }
    pendingDeleteWeight?.let { weight -> ConfirmDeleteDialog("Kilo kaydı silinsin mi?", "${weight.weightKg} kg • ${weight.dateText()}", { pendingDeleteWeight = null }) { vm.deleteWeight(weight.id); pendingDeleteWeight = null } }
    if (showDeleteAccount) AlertDialog(onDismissRequest = { showDeleteAccount = false }, title = { Text("Hesabı kalıcı olarak sil?") }, text = { Text("Kalori, makro, kilo ve hedef kayıtların ile Kalori Takip hesabın kalıcı olarak silinecek. Bu işlem geri alınamaz.") }, confirmButton = { Button(onClick = { showDeleteAccount = false; vm.deleteAccount() }) { Text("Kalıcı olarak sil") } }, dismissButton = { TextButton(onClick = { showDeleteAccount = false }) { Text("Vazgeç") } })
}

@Composable private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) { Card(modifier) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, fontWeight = FontWeight.Bold) } } }

@Composable private fun SummaryCard(state: TrackerUiState, onGoalClick: () -> Unit) {
    val remaining = (state.calorieGoal - state.caloriesToday).coerceAtLeast(0.0)
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Bugün", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${state.caloriesToday.toInt()} / ${state.calorieGoal} kcal"); LinearProgressIndicator(progress = { (state.caloriesToday / state.calorieGoal.coerceAtLeast(1)).toFloat().coerceIn(0f,1f) }, modifier = Modifier.fillMaxWidth()); Text("Kalan: ${remaining.toInt()} kcal"); Text("Protein ${state.proteinToday.toInt()} g • Karb ${state.carbsToday.toInt()} g • Yağ ${state.fatToday.toInt()} g"); TextButton(onClick = onGoalClick) { Text("Hedefi değiştir") } } }
}

@Composable private fun MonthSummaryCard(state: TrackerUiState) { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Bu ay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text("Toplam ${state.monthCalories.toInt()} kcal"); Text("Günlük ortalama ${state.monthAverage} kcal") } } }

@Composable private fun SevenDayCard(state: TrackerUiState) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("tr","TR")) }
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Son 7 gün", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); state.dailyTotals(7).forEach { day -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(day.date.format(formatter), modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodySmall); LinearProgressIndicator(progress = { (day.calories / state.calorieGoal.coerceAtLeast(1)).toFloat().coerceIn(0f,1f) }, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Text("${day.calories.toInt()}", style = MaterialTheme.typography.bodySmall) } } } }
}

@Composable private fun RecentFoodsCard(foods: List<CalorieEntry>, onAdd: (CalorieEntry) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Son kullanılanlar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); foods.forEach { food -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(food.foodName); Text("${food.grams.toInt()} g • ${food.calories.toInt()} kcal", style = MaterialTheme.typography.bodySmall) }; TextButton(onClick = { onAdd(food) }) { Text("Tekrar ekle") } } } } }
}

@Composable private fun EntryRow(entry: CalorieEntry, showDate: Boolean = false, onEdit: () -> Unit, onDelete: () -> Unit) { ListItem(headlineContent = { Text(entry.foodName) }, supportingContent = { Text((if (showDate) "${entry.dateText()} • " else "") + "${entry.mealType} • ${entry.grams.toInt()} g • P ${entry.proteinG.toInt()} / K ${entry.carbsG.toInt()} / Y ${entry.fatG.toInt()}") }, trailingContent = { Column(horizontalAlignment = Alignment.End) { Text("${entry.calories.toInt()} kcal"); Row { TextButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Düzenle") }; TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 6.dp)) { Text("Sil") } } } }) }

@Composable private fun FoodDialog(initial: CalorieEntry? = null, onDismiss: () -> Unit, onSave: (String,String,Double,Double,Double,Double,Double) -> Unit) {
    var name by remember(initial?.id) { mutableStateOf(initial?.foodName.orEmpty()) }; var meal by remember(initial?.id) { mutableStateOf(initial?.mealType ?: "Öğün") }; var grams by remember(initial?.id) { mutableStateOf(initial?.grams?.toString().orEmpty()) }; var calories by remember(initial?.id) { mutableStateOf(initial?.calories?.toString().orEmpty()) }; var protein by remember(initial?.id) { mutableStateOf(initial?.proteinG?.toString().orEmpty()) }; var carbs by remember(initial?.id) { mutableStateOf(initial?.carbsG?.toString().orEmpty()) }; var fat by remember(initial?.id) { mutableStateOf(initial?.fatG?.toString().orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (initial == null) "Yemek ekle" else "Yemek düzenle") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(name,{name=it},label={Text("Yemek")},singleLine=true); OutlinedTextField(meal,{meal=it},label={Text("Öğün")},singleLine=true); OutlinedTextField(grams,{grams=it},label={Text("Gram")},singleLine=true); OutlinedTextField(calories,{calories=it},label={Text("Kalori")},singleLine=true); OutlinedTextField(protein,{protein=it},label={Text("Protein g")},singleLine=true); OutlinedTextField(carbs,{carbs=it},label={Text("Karbonhidrat g")},singleLine=true); OutlinedTextField(fat,{fat=it},label={Text("Yağ g")},singleLine=true) } }, confirmButton = { Button(enabled = name.isNotBlank() && grams.toDoubleOrNull()!=null && calories.toDoubleOrNull()!=null, onClick = { onSave(name,meal,grams.toDoubleOrNull()?:0.0,calories.toDoubleOrNull()?:0.0,protein.toDoubleOrNull()?:0.0,carbs.toDoubleOrNull()?:0.0,fat.toDoubleOrNull()?:0.0) }) { Text(if (initial == null) "Kaydet" else "Güncelle") } }, dismissButton = { TextButton(onClick=onDismiss){Text("İptal")} })
}

@Composable private fun BarcodeNumberDialog(onDismiss: () -> Unit, onSearch: (String) -> Unit) { var barcode by remember { mutableStateOf("") }; AlertDialog(onDismissRequest=onDismiss,title={Text("Barkod numarası")},text={OutlinedTextField(barcode,{barcode=it.filter(Char::isDigit)},label={Text("EAN / UPC")},singleLine=true)},confirmButton={Button(enabled=barcode.length in 8..14,onClick={onSearch(barcode)}){Text("Ürünü bul")}},dismissButton={TextButton(onClick=onDismiss){Text("İptal")}}) }

@Composable private fun BarcodeProductDialog(product: BarcodeProduct, onDismiss: () -> Unit, onSave: (String,Double) -> Unit) { var meal by remember(product.barcode){mutableStateOf("Öğün")}; var grams by remember(product.barcode){mutableStateOf("100")}; val g=grams.toDoubleOrNull()?:0.0; val ratio=g/100.0; AlertDialog(onDismissRequest=onDismiss,title={Text(product.name)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){if(product.brand.isNotBlank())Text(product.brand,style=MaterialTheme.typography.labelMedium);Text("100 g: ${product.calories100g.toInt()} kcal • P ${product.protein100g.toInt()} • K ${product.carbs100g.toInt()} • Y ${product.fat100g.toInt()}");OutlinedTextField(meal,{meal=it},label={Text("Öğün")},singleLine=true);OutlinedTextField(grams,{grams=it},label={Text("Gram")},singleLine=true);Text("Eklenecek: ${(product.calories100g*ratio).toInt()} kcal")}},confirmButton={Button(enabled=g>0,onClick={onSave(meal,g)}){Text("Günlüğe ekle")}},dismissButton={TextButton(onClick=onDismiss){Text("İptal")}}) }

@Composable private fun ConfirmDeleteDialog(title:String,detail:String,onDismiss:()->Unit,onConfirm:()->Unit){AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={Text(detail)},confirmButton={Button(onClick=onConfirm){Text("Sil")}},dismissButton={TextButton(onClick=onDismiss){Text("Vazgeç")}})}

@Composable private fun NumberDialog(title:String,suffix:String,onDismiss:()->Unit,onSave:(Double)->Unit){var value by remember{mutableStateOf("")};AlertDialog(onDismissRequest=onDismiss,title={Text(title)},text={OutlinedTextField(value,{value=it},label={Text(suffix)},singleLine=true)},confirmButton={Button(enabled=value.toDoubleOrNull()!=null,onClick={value.toDoubleOrNull()?.let(onSave)}){Text("Kaydet")}},dismissButton={TextButton(onClick=onDismiss){Text("İptal")}})}
