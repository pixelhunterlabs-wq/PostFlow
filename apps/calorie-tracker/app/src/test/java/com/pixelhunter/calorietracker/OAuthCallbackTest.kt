package com.pixelhunter.calorietracker

import org.junit.Assert.*
import org.junit.Test

class OAuthCallbackTest {
    @Test fun acceptsPkceCallback() {
        assertTrue(OAuthCallback.isValid("calorietracker://login?code=test-code"))
    }
    @Test fun rejectsCancelledAndMalformedCallbacks() {
        listOf("calorietracker://login", "calorietracker://login?code=", "calorietracker://login?error=access_denied", "calorietracker://login?code=test&error=access_denied", "other://login?code=test", "calorietracker://privacy?code=test", "calorietracker://login/unexpected?code=test", "not a URI").forEach {
            assertFalse(it, OAuthCallback.isValid(it))
        }
    }
}
