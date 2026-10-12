package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodPortionCalculatorTest {
    private val honey = CatalogFood("Bal", 304.0, 0.0, 82.0, 0.0)
    private val almonds = CatalogFood("Badem", 579.0, 21.0, 22.0, 50.0, "Kuruyemişler")
    private val water = CatalogFood("Su", 0.0, 0.0, 0.0, 0.0, "İçecekler")

    @Test fun supportsAllHouseholdSpoons() {
        assertEquals(6, FoodPortionCalculator.spoons.size)
        assertEquals(8, FoodPortionCalculator.glasses.size)
        assertEquals(15.0, FoodMeasureUnit.TABLE_SPOON.volumeMl!!, 0.001)
        assertEquals(10.0, FoodMeasureUnit.DESSERT_SPOON.volumeMl!!, 0.001)
        assertEquals(5.0, FoodMeasureUnit.TEA_SPOON.volumeMl!!, 0.001)
    }

    @Test fun estimatesSpoonUsingFoodDensityInsteadOfAlwaysOneGramPerMl() {
        assertEquals(21.3, FoodPortionCalculator.grams(honey, FoodMeasureUnit.TABLE_SPOON, 1.0)!!, 0.001)
        assertEquals(9.3, FoodPortionCalculator.grams(almonds, FoodMeasureUnit.TABLE_SPOON, 1.0)!!, 0.001)
        assertEquals(64.752, FoodPortionCalculator.kcal(honey, 21.3), 0.001)
    }

    @Test fun silmeAndTepelemeChangeSolidWeights() {
        val level = FoodPortionCalculator.grams(almonds, FoodMeasureUnit.DESSERT_SPOON, 2.0, SpoonFill.LEVEL)!!
        val heaped = FoodPortionCalculator.grams(almonds, FoodMeasureUnit.DESSERT_SPOON, 2.0, SpoonFill.HEAPED)!!
        assertEquals(12.4, level, 0.001)
        assertEquals(18.6, heaped, 0.001)
    }

    @Test fun liquidsIgnoreHeapedSpoonFill() {
        assertTrue(FoodPortionCalculator.isPourable(water))
        val level = FoodPortionCalculator.grams(water, FoodMeasureUnit.TEA_SPOON, 2.0, SpoonFill.LEVEL)
        val heaped = FoodPortionCalculator.grams(water, FoodMeasureUnit.TEA_SPOON, 2.0, SpoonFill.HEAPED)
        assertEquals(10.0, level!!, 0.001)
        assertEquals(level, heaped)
    }

    @Test fun glassAndCupSupportHalfPortionsAndGramsRemainExact() {
        assertEquals(100.0, FoodPortionCalculator.grams(water, FoodMeasureUnit.WATER_GLASS, 0.5)!!, 0.001)
        assertEquals(300.0, FoodPortionCalculator.grams(water, FoodMeasureUnit.MUG, 1.0)!!, 0.001)
        assertEquals(42.5, FoodPortionCalculator.grams(almonds, FoodMeasureUnit.GRAM, 42.5)!!, 0.001)
    }

    @Test fun rejectsInvalidAmounts() {
        assertNull(FoodPortionCalculator.grams(water, FoodMeasureUnit.WATER_GLASS, 0.0))
        assertNull(FoodPortionCalculator.grams(water, FoodMeasureUnit.WATER_GLASS, -1.0))
        assertNull(FoodPortionCalculator.grams(water, FoodMeasureUnit.WATER_GLASS, Double.NaN))
        assertNull(FoodPortionCalculator.grams(water, FoodMeasureUnit.WATER_GLASS, 100.0))
        assertFalse(FoodPortionCalculator.isPourable(almonds))
    }
}
