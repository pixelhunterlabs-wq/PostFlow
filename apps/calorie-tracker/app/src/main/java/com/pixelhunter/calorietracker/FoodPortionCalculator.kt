package com.pixelhunter.calorietracker

import java.util.Locale

/**
 * Everyday Turkish household measures. Volumes are reference sizes, not calibrated tools.
 * Food-specific *bulk density* yields an approximate weight; the user can correct grams.
 * The saved food record continues to use grams and the existing per-100g nutrients.
 */
enum class FoodMeasureUnit(val label: String, val volumeMl: Double? = null, val isSpoon: Boolean = false) {
    GRAM("Gram"),
    COFFEE_SPOON("Kahve kaşığı", 2.5, true),
    TEA_SPOON("Çay kaşığı", 5.0, true),
    DESSERT_SPOON("Tatlı kaşığı", 10.0, true),
    TABLE_SPOON("Yemek kaşığı", 15.0, true),
    SOUP_SPOON("Çorba kaşığı", 15.0, true),
    SERVING_SPOON("Servis kaşığı", 30.0, true),
    LADLE("Kepçe", 100.0),
    COFFEE_CUP("Kahve fincanı", 60.0),
    TEA_GLASS("Çay bardağı", 100.0),
    AJDA_GLASS("Ajda çay bardağı", 125.0),
    WATER_GLASS("Su bardağı", 200.0),
    LARGE_GLASS("Büyük bardak", 250.0),
    MUG("Kupa", 300.0),
    BOWL("Kase", 250.0)
}

enum class SpoonFill(val label: String, val factor: Double) {
    LEVEL("Silme", 1.0),
    NORMAL("Normal", 1.2),
    HEAPED("Tepeleme", 1.5)
}

object FoodPortionCalculator {
    val spoons: List<FoodMeasureUnit> = FoodMeasureUnit.entries.filter { it.isSpoon }
    val glasses: List<FoodMeasureUnit> = FoodMeasureUnit.entries.filter { it != FoodMeasureUnit.GRAM && !it.isSpoon }

    /** Liquids do not have 'heaped' spoons. */
    fun isPourable(food: CatalogFood): Boolean {
        val name = food.name.lowercase(Locale.forLanguageTag("tr-TR"))
        return food.category in setOf("İçecekler", "Çorbalar") ||
            name in setOf("Ayran", "Bal", "Pekmez", "Tahin", "Süt", "Yoğurt", "Kefir").map { it.lowercase(Locale.forLanguageTag("tr-TR")) } ||
            name.contains("çorbası") || name.startsWith("süt ") || name.startsWith("reçel")
    }

    /**
     * Approximate grams per ml of a household volume. For pieces (e.g., a steak),
     * this is only a rough proxy; the UI always allows overriding the gram weight.
     */
    fun bulkDensity(food: CatalogFood): Double {
        val name = food.name.lowercase(Locale.forLanguageTag("tr-TR"))
        return when {
            name == "bal" -> 1.42
            name == "pekmez" -> 1.38
            name.startsWith("reçel") -> 1.30
            name == "tahin" -> 1.10
            name.contains("sıvı yağ") || name.contains("zeytinyağı") || name == "tereyağı" -> 0.92
            name.startsWith("yoğurt") || name.startsWith("sütlaç") -> 1.05
            name.startsWith("süt ") || name == "ayran" || name == "kefir" || name == "latte" -> 1.03
            food.category == "İçecekler" -> 1.00
            food.category == "Çorbalar" || name.contains("çorbası") -> 1.02
            name.contains("yulaf") -> 0.36
            food.category == "Kuruyemişler" -> 0.62
            food.category == "Salatalar" -> 0.48
            food.category == "Sebzeler" -> 0.65
            name.contains("pilav") || name.contains("makarna") -> 0.85
            food.category == "Tatlılar" -> 0.85
            food.category == "Ev Yemekleri" -> 0.90
            else -> 0.90
        }
    }

    fun grams(food: CatalogFood, unit: FoodMeasureUnit, count: Double, fill: SpoonFill = SpoonFill.LEVEL): Double? {
        if (!count.isFinite() || count <= 0 || count > 10_000) return null
        val grams = if (unit == FoodMeasureUnit.GRAM) count else {
            val fullness = if (unit.isSpoon && !isPourable(food)) fill.factor else 1.0
            count * requireNotNull(unit.volumeMl) * bulkDensity(food) * fullness
        }
        return grams.takeIf { it.isFinite() && it > 0 && it <= 10_000 }
    }

    fun kcal(food: CatalogFood, grams: Double): Double = food.calories100g * grams / 100.0
}
