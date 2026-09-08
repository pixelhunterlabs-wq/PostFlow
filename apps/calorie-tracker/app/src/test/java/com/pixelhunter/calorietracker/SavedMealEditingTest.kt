package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SavedMealEditingTest {
    private fun item(grams: Double = 100.0, calories: Double = 150.0) = SavedMealItemRemote("meal", "user", "Yemek", grams, calories, 10.0, 20.0, 5.0)
    @Test fun updatedTotalsAreCalculatedFromItems() { val total = savedMealTotals(listOf(item(), item(50.0, 75.0))); assertEquals(150.0, total.grams, 0.01); assertEquals(225.0, total.calories, 0.01) }
    @Test fun invalidAndEmptyItemsAreRejected() { assertFalse(isValidSavedMealItem(item(0.0))); assertFalse(isValidSavedMealItem(item(calories = -1.0))) }
}
