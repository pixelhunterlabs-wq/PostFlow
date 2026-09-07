package com.pixelhunter.calorietracker

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectBridge(private val context: Context) {
    companion object {
        val stepPermissions = setOf(HealthPermission.getReadPermission(StepsRecord::class))
    }

    val sdkStatus: Int
        get() = HealthConnectClient.getSdkStatus(context)

    val available: Boolean
        get() = sdkStatus == HealthConnectClient.SDK_AVAILABLE

    private val client: HealthConnectClient?
        get() = if (available) HealthConnectClient.getOrCreate(context) else null

    suspend fun hasStepPermission(): Boolean {
        val current = client ?: return false
        return current.permissionController.getGrantedPermissions().containsAll(stepPermissions)
    }

    suspend fun todaySteps(): Long {
        val current = client ?: return 0L
        if (!hasStepPermission()) return 0L
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val end = Instant.now()
        val result = current.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
        )
        return result[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
