package com.pixelhunter.calorietracker

data class SavedMealEditTotals(val grams: Double, val calories: Double, val protein: Double, val carbs: Double, val fat: Double)

fun isValidSavedMealItem(item: SavedMealItemRemote): Boolean = item.foodName.isNotBlank() && item.grams > 0 &&
    item.calories >= 0 && item.proteinG >= 0 && item.carbsG >= 0 && item.fatG >= 0

fun savedMealTotals(items: List<SavedMealItemRemote>) = SavedMealEditTotals(
    items.sumOf { it.grams }, items.sumOf { it.calories }, items.sumOf { it.proteinG },
    items.sumOf { it.carbsG }, items.sumOf { it.fatG }
)
