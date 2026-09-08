package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieV2LogicTest {
    private val input = PersonalGoalInput("Erkek", 30, 180.0, 80.0, 1.55, -0.5)
    @Test fun calculatesMifflinBmr() { assertEquals(1780.0, GoalCalculator.bmr(input), 0.1) }
    @Test fun calculatesTdee() { assertEquals(2759.0, GoalCalculator.tdee(input), 0.2) }
    @Test fun appliesSafeMacros() { val g = GoalCalculator.goals(input); assertTrue(g.calories >= 1500 && g.proteinG >= 50 && g.fatG >= 40) }
    @Test fun localRecommendationsAreAvailableWithoutAiKey() { assertTrue(LocalMealRecommendationProvider().recommend(500, 40).size >= 3) }
}
