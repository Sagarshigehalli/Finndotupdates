package com.anomapro.finndot.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.anomapro.finndot.di.DailySummaryEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Reschedules the daily summary notification when the device boots.
 * Alarms are cleared on reboot, so we must reschedule if the user had it enabled.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val applicationContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            DailySummaryEntryPoint::class.java
        )
        val manager = entryPoint.dailySummaryNotificationManager()

        scope.launch {
            manager.scheduleDailyNotification()
        }
    }
}
