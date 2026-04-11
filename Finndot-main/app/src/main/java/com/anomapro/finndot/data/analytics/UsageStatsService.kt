package com.anomapro.finndot.data.analytics

/**
 * Service for recording anonymized usage statistics (screen visits, time spent, feature usage).
 * Only records when user is signed in. No financial or sensitive data.
 *
 * Used to understand which features are used most and how long users spend on screens.
 */
interface UsageStatsService {
    /**
     * Record a screen visit (increments visit count). No-op if user is not signed in.
     * @param screenId e.g. "home", "transactions", "settings"
     */
    fun recordScreenVisit(screenId: String)

    /**
     * Record time spent on a screen. No-op if user is not signed in.
     * @param screenId the screen that was left
     * @param durationMs time spent on that screen in milliseconds
     */
    fun recordScreenDuration(screenId: String, durationMs: Long)

    /**
     * Record a feature/event usage. No-op if user is not signed in.
     * @param eventId e.g. "sms_scan", "export_backup", "add_transaction", "delete_data"
     */
    fun recordEvent(eventId: String)
}
