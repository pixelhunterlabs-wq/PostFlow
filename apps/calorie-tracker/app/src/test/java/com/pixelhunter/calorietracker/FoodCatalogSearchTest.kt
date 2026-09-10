package com.pixelhunter.calorietracker

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FoodCatalogSearchTest {
    @Test fun normalizesTurkishCharacters() {
        assertEquals("tavuk gogsu", FoodCatalogSearch.normalize("Tavuk Göğsü"))
        assertEquals("cig kofte", FoodCatalogSearch.normalize("Çiğ Köfte"))
    }

    @Test fun matchesAliasesAndFuzzyTerms() {
        val foods = TurkishFoodCatalog.foods
        assertTrue(FoodCatalogSearch.search(foods, "baklawa").any { it.name == "Baklava" })
        assertTrue(FoodCatalogSearch.search(foods, "sutlac").any { it.name == "Sütlaç" })
        assertTrue(FoodCatalogSearch.search(foods, "kurufasulye").any { it.name == "Kuru fasulye" })
    }

    @Test fun validatesOfflineNutritionAndPortions() {
        assertTrue(TurkishFoodCatalog.foods.all { it.calories100g in 0.0..900.0 && it.protein100g >= 0 && it.carbs100g >= 0 && it.fat100g >= 0 && it.defaultPortionGrams > 0 })
    }
}
