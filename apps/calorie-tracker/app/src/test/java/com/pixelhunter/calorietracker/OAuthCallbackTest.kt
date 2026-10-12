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
    @Test fun rejectsAmbiguousAndTokenCallbacks() {
        listOf("calorietracker://login?code=a&code=b", "calorietracker://user@login?code=a",
            "calorietracker://login:42?code=a", "calorietracker://login?code=a#access_token=secret",
            "calorietracker://login?code=a&error_code=bad_code", "calorietracker://login?code=%ZZ")
            .forEach { assertFalse(it, OAuthCallback.isValid(it)) }
    }
    @Test fun messagesNeverEchoSecrets() {
        val secret = "private-token"
        val message = OAuthCallback.failureMessage(IllegalStateException("bad_code $secret"))
        assertTrue(message.contains("süresi dolmuş"))
        assertFalse(message.contains(secret))
        assertFalse(OAuthCallback.errorMessage("calorietracker://login?error=access_denied&error_description=$secret").contains(secret))
        assertTrue(OAuthCallback.failureMessage(java.io.IOException(secret)).contains("İnternet"))
        assertTrue(OAuthCallback.failureMessage(Exception("Unsupported provider: provider is not enabled")).contains("etkin değil"))
    }
}
