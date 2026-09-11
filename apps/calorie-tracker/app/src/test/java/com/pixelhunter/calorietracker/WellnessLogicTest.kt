package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WellnessLogicTest {
    @Test
    fun recommendedCalories_staysWithinSafetyBounds() {
        val low = recommendedCalories("Kadın", 40, 150.0, 40.0, 1.2, 0.5)
        val high = recommendedCalories("Erkek", 25, 210.0, 220.0, 1.725, 0.0)
        assertTrue(low >= 1200)
        assertTrue(high <= 4500)
    }

    @Test
    fun weightLossGoal_reducesDailyTarget() {
        val maintenance = recommendedCalories("Erkek", 30, 180.0, 80.0, 1.55, 0.0)
        val loss = recommendedCalories("Erkek", 30, 180.0, 80.0, 1.55, 0.5)
        assertTrue(loss < maintenance)
    }

    @Test
    fun turkishCatalog_containsCoreFoods() {
        val names = TurkishFoodCatalog.foods.map { it.name }
        assertTrue("Mercimek çorbası" in names)
        assertTrue("Simit" in names)
        assertTrue("Lahmacun" in names)
        assertEquals(names.size, names.distinct().size)
    }
}
