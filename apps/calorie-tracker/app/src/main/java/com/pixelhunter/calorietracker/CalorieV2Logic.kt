package com.pixelhunter.calorietracker

import kotlin.math.roundToInt

data class PersonalGoalInput(val gender: String, val age: Int, val heightCm: Double, val weightKg: Double, val activity: Double, val weeklyKg: Double)
data class MacroGoals(val calories: Int, val proteinG: Int, val carbsG: Int, val fatG: Int)

object GoalCalculator {
    fun bmr(input: PersonalGoalInput): Double = 10 * input.weightKg + 6.25 * input.heightCm - 5 * input.age + if (input.gender == "Erkek") 5 else -161
    fun tdee(input: PersonalGoalInput): Double = bmr(input) * input.activity
    fun goals(input: PersonalGoalInput): MacroGoals {
        val adjustment = (input.weeklyKg * 7700 / 7).coerceIn(-750.0, 500.0)
        val calories = (tdee(input) + adjustment).roundToInt().coerceAtLeast(if (input.gender == "Erkek") 1500 else 1200)
        val protein = (input.weightKg * if (input.weeklyKg < 0) 1.8 else 1.6).roundToInt().coerceAtLeast(50)
        val fat = (input.weightKg * 0.8).roundToInt().coerceAtLeast(40)
        val carbs = ((calories - protein * 4 - fat * 9) / 4.0).roundToInt().coerceAtLeast(0)
        return MacroGoals(calories, protein, carbs, fat)
    }
}

interface MealRecommendationProvider { fun recommend(remainingCalories: Int, remainingProtein: Int): List<String> }
class LocalMealRecommendationProvider : MealRecommendationProvider {
    override fun recommend(remainingCalories: Int, remainingProtein: Int): List<String> = listOf(
        "Tavuk göğsü + pilav + ayran", "Yoğurt + yulaf + muz", "Ton balıklı tam buğday sandviç"
    ).filterIndexed { index, _ -> remainingCalories >= listOf(470, 390, 420)[index] || index == 0 }.take(3)
}
class AiMealRecommendationProvider : MealRecommendationProvider { override fun recommend(remainingCalories: Int, remainingProtein: Int) = emptyList<String>() }

data class WeeklyNutrition(val calories: Double, val protein: Double, val carbs: Double, val fat: Double, val waterMl: Int)
fun weeklyNutrition(entries: List<CalorieEntry>, waterAmounts: List<Int>): WeeklyNutrition = WeeklyNutrition(entries.sumOf { it.calories } / 7, entries.sumOf { it.proteinG } / 7, entries.sumOf { it.carbsG } / 7, entries.sumOf { it.fatG } / 7, waterAmounts.sum())
fun savedMealTotals(items: List<SavedMeal>): SavedMeal = SavedMeal("total", "Toplam", items.sumOf { it.calories }, items.sumOf { it.proteinG }, items.sumOf { it.carbsG }, items.sumOf { it.fatG }, items.sumOf { it.grams })
