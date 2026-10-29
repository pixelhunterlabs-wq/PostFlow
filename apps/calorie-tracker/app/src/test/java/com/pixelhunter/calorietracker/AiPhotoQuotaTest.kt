package com.pixelhunter.calorietracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPhotoQuotaTest {
    @Test fun freeQuota() {
        val quota = AiPhotoQuota(used = 2, limit = 5, premium = false)
        assertEquals(3, quota.remaining)
        assertFalse(quota.exhausted)
    }

    @Test fun premiumQuota() {
        val quota = AiPhotoQuota(used = 99, limit = 100, premium = true)
        assertEquals(1, quota.remaining)
        assertFalse(quota.exhausted)
    }

    @Test fun exhaustedQuotaDoesNotBecomeNegative() {
        val quota = AiPhotoQuota(used = 101, limit = 100, premium = true)
        assertEquals(0, quota.remaining)
        assertTrue(quota.exhausted)
    }

    @Test fun unavailableQuotaDoesNotBlockClientBeforeServerChecks() {
        val quota = AiPhotoQuota(used = 0, limit = 0, premium = false)
        assertFalse(quota.exhausted)
    }
}
