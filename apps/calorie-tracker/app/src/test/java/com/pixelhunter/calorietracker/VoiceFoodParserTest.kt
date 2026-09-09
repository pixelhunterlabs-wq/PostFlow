package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceFoodParserTest {
    @Test
    fun parsesMultipleTurkishFoodsWithGrams() {
        val result = parseVoiceFoods("150 gram tavuk, 200 gram pilav ve 1 bardak ayran")
        assertEquals(3, result.size)
        assertEquals("Tavuk göğsü, pişmiş", result[0].food.name)
        assertEquals(150.0, result[0].grams, 0.01)
        assertEquals("Pirinç pilavı, pişmiş", result[1].food.name)
        assertEquals(200.0, result[1].grams, 0.01)
        assertEquals("Ayran", result[2].food.name)
        assertEquals(200.0, result[2].grams, 0.01)
    }

    @Test
    fun convertsEggCountToApproximateGrams() {
        val result = parseVoiceFoods("2 adet yumurta")
        assertEquals(1, result.size)
        assertEquals(100.0, result.first().grams, 0.01)
    }

    @Test
    fun ignoresUnknownFood() {
        assertTrue(parseVoiceFoods("bir şeyler yedim").isEmpty())
    }

    @Test fun scalesEditedVoiceResult() {
        val pilav = parseVoiceFoods("1 tabak pilav").single()
        val edited = scaleVoiceFood(pilav, 150.0)
        assertEquals(150.0, edited.grams, 0.01)
        assertEquals(pilav.calories * .75, edited.calories, .01)
    }
}
