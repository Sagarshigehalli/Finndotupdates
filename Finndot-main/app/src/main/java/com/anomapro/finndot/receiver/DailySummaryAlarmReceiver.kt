package com.anomapro.finndot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.anomapro.finndot.worker.DailySummaryNotificationWorker

/**
 * Receives the daily summary alarm at the user-configured time.
 * Enqueues a OneTimeWorkRequest to show the notification and schedule the next day's alarm.
 */
class DailySummaryAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DAILY_SUMMARY_ALARM) return

        val workRequest = OneTimeWorkRequestBuilder<DailySummaryNotificationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    companion object {
        const val ACTION_DAILY_SUMMARY_ALARM = "com.anomapro.finndot.ACTION_DAILY_SUMMARY_ALARM"
    }
}
