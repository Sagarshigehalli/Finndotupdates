package com.anomapro.finndot.data.manager

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import com.anomapro.finndot.receiver.DailySummaryAlarmReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailySummaryNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val REQUEST_CODE = 1001
        const val DEFAULT_HOUR = 21
        const val DEFAULT_MINUTE = 0
    }

    /**
     * Schedules daily summary notification at the user-configured time using AlarmManager.
     * Uses exact alarm for precise timing (e.g. 9:00 PM every day).
     * No-op if notifications are disabled.
     */
    suspend fun scheduleDailyNotification() {
        val isEnabled = userPreferencesRepository.dailySummaryNotificationEnabled.first()
        if (!isEnabled) {
            cancelDailyNotification()
            return
        }
        val hour = userPreferencesRepository.dailySummaryNotificationHour.first()
        val minute = userPreferencesRepository.dailySummaryNotificationMinute.first()
        scheduleDailyNotificationAt(hour, minute)
    }

    private fun scheduleDailyNotificationAt(hour: Int, minute: Int) {
        val intent = Intent(context, DailySummaryAlarmReceiver::class.java).apply {
            action = DailySummaryAlarmReceiver.ACTION_DAILY_SUMMARY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = getNextTriggerTimeMillis(hour, minute)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback if SCHEDULE_EXACT_ALARM not granted (Android 12+)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Returns epoch millis for the next occurrence of (hour, minute) in the device's timezone.
     */
    private fun getNextTriggerTimeMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(hour, minute)

        if (!now.isBefore(target)) {
            target = target.plusDays(1)
        }

        return target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /**
     * Cancels the daily summary notification alarm.
     */
    fun cancelDailyNotification() {
        val intent = Intent(context, DailySummaryAlarmReceiver::class.java).apply {
            action = DailySummaryAlarmReceiver.ACTION_DAILY_SUMMARY_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
