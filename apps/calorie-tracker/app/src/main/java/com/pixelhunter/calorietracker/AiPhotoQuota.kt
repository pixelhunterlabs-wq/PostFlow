package com.pixelhunter.calorietracker

/**
 * Read-only photo quota summary returned by the authenticated Premium endpoint.
 * Do not use this for authorization; the server reserves and checks each AI request.
 */
internal data class AiPhotoQuota(val used: Int, val limit: Int, val premium: Boolean) {
    val remaining: Int get() = (limit - used).coerceAtLeast(0)
    val exhausted: Boolean get() = limit > 0 && remaining == 0
}
