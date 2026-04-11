package com.anomapro.finndot.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.anomapro.finndot.data.analytics.UsageStatsService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ScreenAnalyticsViewModel @Inject constructor(
    private val usageStatsService: UsageStatsService
) : ViewModel() {

    private var lastScreenId: String? = null
    private var lastScreenEnterTime: Long = 0L

    fun recordScreenVisit(screenId: String) {
        val now = System.currentTimeMillis()
        if (lastScreenId != screenId) {
            if (lastScreenId != null) {
                val durationMs = (now - lastScreenEnterTime).coerceAtLeast(0)
                usageStatsService.recordScreenDuration(lastScreenId!!, durationMs)
            }
            usageStatsService.recordScreenVisit(screenId)
            lastScreenId = screenId
            lastScreenEnterTime = now
        }
    }

    fun recordEvent(eventId: String) {
        usageStatsService.recordEvent(eventId)
    }
}
