package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeSupportTest {
    @Test fun normalizesSupportedEanAndUpcValues() {
        assertEquals("8690504011800", BarcodeSupport.normalize("8690 5040-11800"))
        assertEquals("01234567", BarcodeSupport.normalize("01234567"))
        assertEquals("012345678905", BarcodeSupport.normalize("012345678905"))
        assertNull(BarcodeSupport.normalize("12345"))
    }

    @Test fun scannerFailureExplainsManualFallback() {
        assertTrue(BarcodeSupport.scannerFailureMessage().contains("manuel"))
    }

    @Test fun parsesOpenFoodFactsNutrition() {
        val product = BarcodeSupport.parseOpenFoodFacts("8690504011800", """
            {"status":1,"product":{"product_name":"Test Ayran","brands":"Test Marka","nutriments":{"energy-kcal_100g":42,"proteins_100g":2.1,"carbohydrates_100g":3.4,"fat_100g":1.5}}}
        """.trimIndent())!!
        assertEquals("Test Ayran", product.name)
        assertEquals("Test Marka", product.brand)
        assertEquals(42.0, product.calories100g, 0.01)
        assertEquals(2.1, product.protein100g, 0.01)
    }

    @Test fun missingOpenFoodFactsProductReturnsNull() {
        assertNull(BarcodeSupport.parseOpenFoodFacts("8690504011800", "{\"status\":0}"))
    }
}
