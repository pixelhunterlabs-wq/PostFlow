package com.pixelhunter.calorietracker

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class CatalogFood(
    val name: String,
    val calories100g: Double,
    val protein100g: Double,
    val carbs100g: Double,
    val fat100g: Double
)

object TurkishFoodCatalog {
    val foods = listOf(
        CatalogFood("Tavuk göğsü, pişmiş", 165.0, 31.0, 0.0, 3.6),
        CatalogFood("Yumurta", 143.0, 12.6, 0.7, 9.5),
        CatalogFood("Pirinç pilavı, pişmiş", 130.0, 2.7, 28.0, 0.3),
        CatalogFood("Bulgur pilavı, pişmiş", 83.0, 3.1, 18.6, 0.2),
        CatalogFood("Mercimek çorbası", 68.0, 3.5, 10.0, 1.7),
        CatalogFood("Menemen", 105.0, 5.2, 4.0, 7.5),
        CatalogFood("Simit", 338.0, 10.0, 56.0, 8.5),
        CatalogFood("Beyaz ekmek", 265.0, 9.0, 49.0, 3.2),
        CatalogFood("Tam buğday ekmeği", 247.0, 13.0, 41.0, 3.4),
        CatalogFood("Poğaça", 360.0, 8.0, 44.0, 17.0),
        CatalogFood("Su böreği", 310.0, 10.0, 28.0, 18.0),
        CatalogFood("Lahmacun", 230.0, 10.0, 28.0, 8.5),
        CatalogFood("Et döner", 220.0, 22.0, 4.0, 13.0),
        CatalogFood("Tavuk döner", 190.0, 22.0, 4.0, 9.0),
        CatalogFood("Adana kebap", 239.0, 25.0, 2.0, 15.0),
        CatalogFood("Köfte", 220.0, 22.0, 5.0, 13.0),
        CatalogFood("Yoğurt", 61.0, 3.5, 4.7, 3.3),
        CatalogFood("Ayran", 37.0, 2.0, 3.0, 1.7),
        CatalogFood("Beyaz peynir", 264.0, 14.0, 4.0, 21.0),
        CatalogFood("Kaşar peyniri", 404.0, 25.0, 2.0, 33.0),
        CatalogFood("Yulaf ezmesi", 389.0, 16.9, 66.3, 6.9),
        CatalogFood("Muz", 89.0, 1.1, 22.8, 0.3),
        CatalogFood("Elma", 52.0, 0.3, 13.8, 0.2),
        CatalogFood("Portakal", 47.0, 0.9, 11.8, 0.1),
        CatalogFood("Avokado", 160.0, 2.0, 8.5, 14.7),
        CatalogFood("Patates, haşlanmış", 87.0, 1.9, 20.1, 0.1),
        CatalogFood("Makarna, pişmiş", 131.0, 5.0, 25.0, 1.1),
        CatalogFood("Nohut, pişmiş", 164.0, 8.9, 27.4, 2.6),
        CatalogFood("Kuru fasulye, pişmiş", 127.0, 8.7, 22.8, 0.5),
        CatalogFood("Cici Bebe bisküvi", 438.0, 7.0, 73.0, 13.0)
    )
}

class LocalWellnessStore(context: Context) {
    private val prefs = context.getSharedPreferences("calorie_wellness", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun onboardingDone(userKey: String): Boolean = prefs.getBoolean("onboarding_done_${userKey.lowercase()}", false)
    fun markOnboardingDone(userKey: String) = prefs.edit().putBoolean("onboarding_done_${userKey.lowercase()}", true).commit()

    fun waterMl(date: LocalDate = LocalDate.now()): Int = prefs.getInt("water_$date", 0)
    fun addWater(amount: Int, date: LocalDate = LocalDate.now()): Int {
        val next = (waterMl(date) + amount).coerceIn(0, 6000)
        prefs.edit().putInt("water_$date", next).apply()
        return next
    }
    fun removeWater(amount: Int, date: LocalDate = LocalDate.now()): Int {
        val next = (waterMl(date) - amount).coerceAtLeast(0)
        prefs.edit().putInt("water_$date", next).apply()
        return next
    }

    fun favorites(): List<CatalogFood> = runCatching {
        json.decodeFromString<List<CatalogFood>>(prefs.getString("favorites_json", "[]") ?: "[]")
    }.getOrDefault(emptyList())

    fun toggleFavorite(food: CatalogFood): List<CatalogFood> {
        val current = favorites().toMutableList()
        val index = current.indexOfFirst { it.name == food.name }
        if (index >= 0) current.removeAt(index) else current.add(food)
        prefs.edit().putString("favorites_json", json.encodeToString(current)).apply()
        return current
    }
}

fun recommendedCalories(
    gender: String,
    age: Int,
    heightCm: Double,
    weightKg: Double,
    activityFactor: Double,
    weeklyGoalKg: Double
): Int {
    val base = 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + if (gender == "Kadın") -161.0 else 5.0
    val maintenance = base * activityFactor
    val dailyAdjustment = (weeklyGoalKg * 7700.0) / 7.0
    return (maintenance - dailyAdjustment).toInt().coerceIn(1200, 4500)
}
