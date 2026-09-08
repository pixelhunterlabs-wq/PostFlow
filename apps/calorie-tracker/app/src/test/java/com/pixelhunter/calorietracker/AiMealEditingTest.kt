package com.pixelhunter.calorietracker
import org.junit.Assert.assertEquals
import org.junit.Test
class AiMealEditingTest {
    private val meal = EditableAiMeal(AiMealItem("Tavuk", 100.0, 165.0, 31.0, 0.0, 3.6))

    @Test fun scalesMacrosFromOriginalValues() {
        val x = scaleAiMeal(meal, 150.0)
        assertEquals(247.5, x.calories, 0.01)
        assertEquals(46.5, x.proteinG, 0.01)
    }

    @Test fun selectedItemsExcludeUncheckedAndRejectInvalidValues() {
        assertEquals(1, selectedAiMeals(listOf(meal, meal.copy(included = false))).size)
        assertEquals(false, isValidAiMeal(meal.item.copy(grams = 0.0)))
        assertEquals(false, isValidAiMeal(meal.item.copy(calories = -1.0)))
    }
}
