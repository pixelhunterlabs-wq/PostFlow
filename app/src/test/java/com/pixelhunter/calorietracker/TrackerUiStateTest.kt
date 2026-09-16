package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class TrackerUiStateTest {
    private fun entry(daysAgo: Long, calories: Double) = CalorieEntry(
        userId = "test-user",
        foodName = "Test",
        grams = 100.0,
        calories = calories,
        mealType = "Öğün",
        eatenAt = LocalDate.now().minusDays(daysAgo).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toString()
    )

    @Test
    fun todayTotalsOnlyIncludeToday() {
        val state = TrackerUiState(entries = listOf(entry(0, 600.0), entry(1, 900.0)))
        assertEquals(600.0, state.caloriesToday, 0.01)
    }

    @Test
    fun sevenDayAverageUsesSevenCalendarDays() {
        val state = TrackerUiState(entries = listOf(entry(0, 700.0), entry(1, 700.0)))
        assertEquals(200, state.averageSince(7))
    }

    @Test
    fun recentFoodsAreUniqueByNameIgnoringCase() {
        val state = TrackerUiState(entries = listOf(
            entry(0, 100.0).copy(foodName = "Elma"),
            entry(1, 120.0).copy(foodName = "elma"),
            entry(2, 200.0).copy(foodName = "Yoğurt")
        ))
        assertEquals(2, state.recentFoods.size)
    }
}
