package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DynamicGoalSuggestionTest {
    @Test
    fun requiresAtLeastTwoWeightEntries() {
        val result = dynamicGoalSuggestion(
            currentGoal = 2000,
            weights = listOf(
                WeightEntry(userId = "u", weightKg = 80.0, measuredAt = "2026-09-01T08:00:00Z")
            ),
            targetWeeklyLossKg = 0.5
        )
        assertNull(result)
    }

    @Test
    fun lowersCaloriesWhenLossIsSlowerThanTarget() {
        val result = dynamicGoalSuggestion(
            currentGoal = 2000,
            weights = listOf(
                WeightEntry(userId = "u", weightKg = 80.0, measuredAt = "2026-09-01T08:00:00Z"),
                WeightEntry(userId = "u", weightKg = 79.8, measuredAt = "2026-09-08T08:00:00Z")
            ),
            targetWeeklyLossKg = 0.5
        )
        assertNotNull(result)
        requireNotNull(result)
        assertEquals(1750, result.suggestedGoal)
    }

    @Test
    fun keepsGoalWhenTrendMatchesTarget() {
        val result = dynamicGoalSuggestion(
            currentGoal = 2100,
            weights = listOf(
                WeightEntry(userId = "u", weightKg = 80.0, measuredAt = "2026-09-01T08:00:00Z"),
                WeightEntry(userId = "u", weightKg = 79.5, measuredAt = "2026-09-08T08:00:00Z")
            ),
            targetWeeklyLossKg = 0.5
        )
        assertNotNull(result)
        assertEquals(2100, requireNotNull(result).suggestedGoal)
    }
}
