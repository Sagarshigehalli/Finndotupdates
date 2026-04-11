package com.anomapro.finndot.data.analytics

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpUsageStatsService @Inject constructor() : UsageStatsService {
    override fun recordScreenVisit(screenId: String) {}
    override fun recordScreenDuration(screenId: String, durationMs: Long) {}
    override fun recordEvent(eventId: String) {}
}
