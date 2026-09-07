package com.pixelhunter.calorietracker

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class SavedMeal(
    val id: String,
    val name: String,
    val calories: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val grams: Double
)

data class DynamicGoalSuggestion(
    val currentGoal: Int,
    val suggestedGoal: Int,
    val observedWeeklyChangeKg: Double,
    val targetWeeklyLossKg: Double
)

class AdvancedWellnessStore(context: Context) {
    private val prefs = context.getSharedPreferences("calorie_advanced_wellness", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun remindersEnabled(): Boolean = prefs.getBoolean("meal_reminders", false)
    fun setRemindersEnabled(value: Boolean) = prefs.edit { putBoolean("meal_reminders", value) }

    fun targetWeeklyLossKg(): Double = java.lang.Double.longBitsToDouble(
        prefs.getLong("weekly_loss_bits", java.lang.Double.doubleToRawLongBits(0.25))
    )

    fun setTargetWeeklyLossKg(value: Double) = prefs.edit {
        putLong("weekly_loss_bits", java.lang.Double.doubleToRawLongBits(value.coerceIn(0.0, 1.0)))
    }

    fun savedMeals(): List<SavedMeal> = runCatching {
        json.decodeFromString<List<SavedMeal>>(prefs.getString("saved_meals", "[]") ?: "[]")
    }.getOrDefault(emptyList())

    fun saveMeal(meal: SavedMeal): List<SavedMeal> {
        val next = savedMeals().filterNot { it.id == meal.id }.toMutableList().apply { add(0, meal) }.take(20)
        prefs.edit { putString("saved_meals", json.encodeToString(next)) }
        return next
    }

    fun deleteMeal(id: String): List<SavedMeal> {
        val next = savedMeals().filterNot { it.id == id }
        prefs.edit { putString("saved_meals", json.encodeToString(next)) }
        return next
    }
}

fun dynamicGoalSuggestion(
    currentGoal: Int,
    weights: List<WeightEntry>,
    targetWeeklyLossKg: Double
): DynamicGoalSuggestion? {
    val parsed = weights.mapNotNull { entry ->
        runCatching { Instant.parse(entry.measuredAt) to entry.weightKg }.getOrNull()
    }.sortedBy { it.first }
    if (parsed.size < 2) return null

    val latest = parsed.last()
    val cutoff = latest.first.minusSeconds(14L * 24L * 3600L)
    val oldest = parsed.firstOrNull { it.first >= cutoff } ?: parsed.first()
    val days = ((latest.first.epochSecond - oldest.first.epochSecond) / 86400.0).coerceAtLeast(3.0)
    val observedWeeklyLoss = (oldest.second - latest.second) / (days / 7.0)
    val difference = targetWeeklyLossKg - observedWeeklyLoss
    if (abs(difference) < 0.08) {
        return DynamicGoalSuggestion(currentGoal, currentGoal, observedWeeklyLoss, targetWeeklyLossKg)
    }

    val adjustment = ((difference * 7700.0) / 7.0).roundToInt().coerceIn(-250, 250)
    val suggested = (currentGoal - adjustment).coerceIn(1200, 4500)
    return DynamicGoalSuggestion(currentGoal, suggested, observedWeeklyLoss, targetWeeklyLossKg)
}

object MealReminderScheduler {
    private const val CHANNEL_ID = "meal_reminders"
    private val reminders = listOf(
        Triple(8101, 8, "Kahvaltını kaydetmeyi unutma"),
        Triple(8102, 13, "Öğle öğününü günlüğüne ekle"),
        Triple(8103, 19, "Akşam öğününü kaydet ve gününü tamamla")
    )

    fun enable(context: Context) {
        createChannel(context)
        reminders.forEach { (requestCode, hour, message) -> schedule(context, requestCode, hour, message) }
    }

    fun disable(context: Context) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        reminders.forEach { (requestCode, _, _) -> alarm.cancel(pendingIntent(context, requestCode, "")) }
    }

    private fun schedule(context: Context, requestCode: Int, hour: Int, message: String) {
        val alarm = context.getSystemService(AlarmManager::class.java)
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(hour, 0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        alarm.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            millis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent(context, requestCode, message)
        )
    }

    private fun pendingIntent(context: Context, requestCode: Int, message: String): PendingIntent {
        val intent = Intent(context, MealReminderReceiver::class.java).putExtra("message", message)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Öğün hatırlatmaları", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Yemek kayıtlarını gün içinde hatırlatır"
                }
            )
        }
    }

    fun channelId(): String = CHANNEL_ID
}

class MealReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        MealReminderScheduler.createChannel(context)
        val openIntent = Intent(context, ModernMainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            9001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, MealReminderScheduler.channelId())
        } else {
            @Suppress("DEPRECATION") android.app.Notification.Builder(context)
        }.setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kalori Takip")
            .setContentText(intent.getStringExtra("message") ?: "Öğününü kaydetmeyi unutma")
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && AdvancedWellnessStore(context).remindersEnabled()) {
            MealReminderScheduler.enable(context)
        }
    }
}
