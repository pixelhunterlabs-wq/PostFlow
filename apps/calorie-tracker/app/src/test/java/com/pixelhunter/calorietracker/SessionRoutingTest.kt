package com.pixelhunter.calorietracker

import org.junit.Assert.*
import org.junit.Test

class SessionRoutingTest {
    @Test fun restoringSessionDoesNotShowOnboarding() {
        assertFalse(TrackerUiState(signedIn = true, authChecking = true).requiresOnboarding)
    }
    @Test fun completedProfileWithNoMealsDoesNotRepeatOnboarding() {
        assertFalse(TrackerUiState(signedIn = true, authChecking = false, onboardingCompleted = true).requiresOnboarding)
    }
    @Test fun newAccountRequiresOnboardingOnlyAfterRestoration() {
        assertTrue(TrackerUiState(signedIn = true, authChecking = false).requiresOnboarding)
        assertFalse(TrackerUiState(authChecking = false).requiresOnboarding)
    }
}
