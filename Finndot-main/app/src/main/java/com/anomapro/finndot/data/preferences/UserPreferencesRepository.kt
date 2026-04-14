package com.anomapro.finndot.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val HAS_SKIPPED_SMS_PERMISSION = booleanPreferencesKey("has_skipped_sms_permission")
        val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        val SYSTEM_PROMPT = stringPreferencesKey("system_prompt")
        val HAS_SHOWN_SCAN_TUTORIAL = booleanPreferencesKey("has_shown_scan_tutorial")
        val ACTIVE_DOWNLOAD_ID = longPreferencesKey("active_download_id")
        val SMS_SCAN_MONTHS = intPreferencesKey("sms_scan_months")
        val SMS_SCAN_ALL_TIME = booleanPreferencesKey("sms_scan_all_time")
        val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
        val LAST_SCAN_PERIOD = intPreferencesKey("last_scan_period")
        val BASE_CURRENCY = stringPreferencesKey("base_currency")
        
        // In-App Review preferences
        val FIRST_LAUNCH_TIME = longPreferencesKey("first_launch_time")
        val HAS_SHOWN_REVIEW_PROMPT = booleanPreferencesKey("has_shown_review_prompt")
        val LAST_REVIEW_PROMPT_TIME = longPreferencesKey("last_review_prompt_time")
        
        // User Profile preferences
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_OCCUPATION = stringPreferencesKey("user_occupation")
        val HOURS_WORKED_PER_MONTH = intPreferencesKey("hours_worked_per_month")
        val AVG_INCOME = longPreferencesKey("avg_income") // Using long to store income in smallest currency unit
        val USER_MOBILE_NUMBER = stringPreferencesKey("user_mobile_number")
        
        // Notification preferences
        val DAILY_SUMMARY_NOTIFICATION_ENABLED = booleanPreferencesKey("daily_summary_notification_enabled")
        val DAILY_SUMMARY_NOTIFICATION_HOUR = intPreferencesKey("daily_summary_notification_hour")
        val DAILY_SUMMARY_NOTIFICATION_MINUTE = intPreferencesKey("daily_summary_notification_minute")

        /** Part 8 — one-time DB backfill for legacy bank-transfer rows. */
        val HAS_RUN_TRANSFER_CATEGORY_BACKFILL_V1 = booleanPreferencesKey("has_run_transfer_category_backfill_v1")
    }

    suspend fun hasTransferCategoryBackfillRun(): Boolean =
        context.dataStore.data.first()[PreferencesKeys.HAS_RUN_TRANSFER_CATEGORY_BACKFILL_V1] == true

    suspend fun setTransferCategoryBackfillRun(done: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_RUN_TRANSFER_CATEGORY_BACKFILL_V1] = done
        }
    }

    val userPreferences: Flow<UserPreferences> = context.dataStore.data
        .map { preferences ->
            UserPreferences(
                isDarkThemeEnabled = preferences[PreferencesKeys.DARK_THEME_ENABLED],
                isDynamicColorEnabled = preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] ?: false,
                hasSkippedSmsPermission = preferences[PreferencesKeys.HAS_SKIPPED_SMS_PERMISSION] ?: false,
                hasCompletedOnboarding = preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false,
                hasShownScanTutorial = preferences[PreferencesKeys.HAS_SHOWN_SCAN_TUTORIAL] ?: false,
                smsScanMonths = preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 12, // Default to 12 months
                smsScanAllTime = preferences[PreferencesKeys.SMS_SCAN_ALL_TIME] ?: false, // Default to false
                baseCurrency = preferences[PreferencesKeys.BASE_CURRENCY] ?: "INR", // Default to INR
                userName = preferences[PreferencesKeys.USER_NAME],
                userOccupation = preferences[PreferencesKeys.USER_OCCUPATION],
                hoursWorkedPerMonth = preferences[PreferencesKeys.HOURS_WORKED_PER_MONTH],
                avgIncome = preferences[PreferencesKeys.AVG_INCOME],
                userMobileNumber = preferences[PreferencesKeys.USER_MOBILE_NUMBER]
            )
        }

    val baseCurrency: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.BASE_CURRENCY] ?: "INR"
        }

    suspend fun updateDarkThemeEnabled(enabled: Boolean?) {
        context.dataStore.edit { preferences ->
            if (enabled == null) {
                preferences.remove(PreferencesKeys.DARK_THEME_ENABLED)
            } else {
                preferences[PreferencesKeys.DARK_THEME_ENABLED] = enabled
            }
        }
    }

    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }
    
    suspend fun updateSkippedSmsPermission(skipped: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SKIPPED_SMS_PERMISSION] = skipped
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    fun hasCompletedOnboarding(): Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.HAS_COMPLETED_ONBOARDING] ?: false }
    
    suspend fun updateSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT] = prompt
        }
    }
    
    fun getSystemPrompt(): Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SYSTEM_PROMPT]
        }
    
    suspend fun markScanTutorialShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_SCAN_TUTORIAL] = true
        }
    }
    
    suspend fun saveActiveDownloadId(id: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_DOWNLOAD_ID] = id
        }
    }
    
    suspend fun getActiveDownloadId(): Long? {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.ACTIVE_DOWNLOAD_ID] }
            .first()
    }
    
    suspend fun clearActiveDownloadId() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.ACTIVE_DOWNLOAD_ID)
        }
    }
    
    val smsScanMonths: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 12 // Default to 12 months
        }
    
    suspend fun updateSmsScanMonths(months: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_MONTHS] = months
        }
    }

    suspend fun getSmsScanMonths(): Int {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.SMS_SCAN_MONTHS] ?: 12 }
            .first()
    }

    val smsScanAllTime: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_ALL_TIME] ?: false
        }

    suspend fun updateSmsScanAllTime(allTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMS_SCAN_ALL_TIME] = allTime
        }
    }

    suspend fun getSmsScanAllTime(): Boolean {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.SMS_SCAN_ALL_TIME] ?: false }
            .first()
    }
    
    suspend fun setLastScanTimestamp(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] = timestamp
        }
    }
    
    suspend fun setLastScanPeriod(period: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_SCAN_PERIOD] = period
        }
    }
    
    suspend fun setFirstLaunchTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_LAUNCH_TIME] = timestamp
        }
    }
    
    suspend fun hasShownReviewPrompt(): Boolean {
        return context.dataStore.data
            .map { preferences -> preferences[PreferencesKeys.HAS_SHOWN_REVIEW_PROMPT] ?: false }
            .first()
    }
    
    suspend fun markReviewPromptShown() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_REVIEW_PROMPT] = true
            preferences[PreferencesKeys.LAST_REVIEW_PROMPT_TIME] = System.currentTimeMillis()
        }
    }
    
    // Flow methods for backup/restore
    fun getLastScanTimestamp(): Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_SCAN_TIMESTAMP] }
    
    fun getLastScanPeriod(): Flow<Int?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_SCAN_PERIOD] }
    
    fun getFirstLaunchTime(): Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.FIRST_LAUNCH_TIME] }
    
    fun getHasShownReviewPrompt(): Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.HAS_SHOWN_REVIEW_PROMPT] ?: false }
    
    fun getLastReviewPromptTime(): Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.LAST_REVIEW_PROMPT_TIME] }
    
    // Update methods for import
    suspend fun updateDarkTheme(enabled: Boolean?) {
        updateDarkThemeEnabled(enabled)
    }
    
    suspend fun updateDynamicColor(enabled: Boolean) {
        updateDynamicColorEnabled(enabled)
    }
    
    suspend fun updateHasSkippedSmsPermission(skipped: Boolean) {
        updateSkippedSmsPermission(skipped)
    }
    
    suspend fun updateLastScanTimestamp(timestamp: Long) {
        setLastScanTimestamp(timestamp)
    }
    
    suspend fun updateLastScanPeriod(period: Int) {
        setLastScanPeriod(period)
    }
    
    suspend fun updateFirstLaunchTime(timestamp: Long) {
        setFirstLaunchTime(timestamp)
    }
    
    suspend fun updateHasShownScanTutorial(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_SCAN_TUTORIAL] = shown
        }
    }
    
    suspend fun updateHasShownReviewPrompt(shown: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_SHOWN_REVIEW_PROMPT] = shown
        }
    }
    
    suspend fun updateLastReviewPromptTime(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_REVIEW_PROMPT_TIME] = timestamp
        }
    }
    
    // User Profile methods
    val userName: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_NAME] }
    
    val userOccupation: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_OCCUPATION] }
    
    val hoursWorkedPerMonth: Flow<Int?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.HOURS_WORKED_PER_MONTH] }
    
    val avgIncome: Flow<Long?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.AVG_INCOME] }
    
    suspend fun updateUserName(name: String?) {
        context.dataStore.edit { preferences ->
            if (name.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.USER_NAME)
            } else {
                preferences[PreferencesKeys.USER_NAME] = name
            }
        }
    }
    
    suspend fun updateUserOccupation(occupation: String?) {
        context.dataStore.edit { preferences ->
            if (occupation.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.USER_OCCUPATION)
            } else {
                preferences[PreferencesKeys.USER_OCCUPATION] = occupation
            }
        }
    }
    
    suspend fun updateHoursWorkedPerMonth(hours: Int?) {
        context.dataStore.edit { preferences ->
            if (hours == null) {
                preferences.remove(PreferencesKeys.HOURS_WORKED_PER_MONTH)
            } else {
                preferences[PreferencesKeys.HOURS_WORKED_PER_MONTH] = hours
            }
        }
    }
    
    suspend fun updateAvgIncome(income: Long?) {
        context.dataStore.edit { preferences ->
            if (income == null) {
                preferences.remove(PreferencesKeys.AVG_INCOME)
            } else {
                preferences[PreferencesKeys.AVG_INCOME] = income
            }
        }
    }

    val userMobileNumber: Flow<String?> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.USER_MOBILE_NUMBER] }

    suspend fun updateUserMobileNumber(mobileNumber: String?) {
        context.dataStore.edit { preferences ->
            if (mobileNumber.isNullOrBlank()) {
                preferences.remove(PreferencesKeys.USER_MOBILE_NUMBER)
            } else {
                preferences[PreferencesKeys.USER_MOBILE_NUMBER] = mobileNumber
            }
        }
    }
    
    // Daily Summary Notification methods
    val dailySummaryNotificationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_ENABLED] ?: true }

    val dailySummaryNotificationHour: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_HOUR] ?: 21 }

    val dailySummaryNotificationMinute: Flow<Int> = context.dataStore.data
        .map { preferences -> preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_MINUTE] ?: 0 }
    
    suspend fun updateDailySummaryNotificationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_ENABLED] = enabled
        }
    }

    suspend fun updateDailySummaryNotificationTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_HOUR] = hour
            preferences[PreferencesKeys.DAILY_SUMMARY_NOTIFICATION_MINUTE] = minute
        }
    }
}

data class UserPreferences(
    val isDarkThemeEnabled: Boolean? = null, // null means follow system
    val isDynamicColorEnabled: Boolean = false, // Default to custom brand colors
    val hasSkippedSmsPermission: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val hasShownScanTutorial: Boolean = false,
    val smsScanMonths: Int = 12, // Default to 12 months
    val smsScanAllTime: Boolean = false, // Default to false
    val baseCurrency: String = "INR", // Default to INR
    val userName: String? = null,
    val userOccupation: String? = null,
    val hoursWorkedPerMonth: Int? = null,
    val avgIncome: Long? = null, // Income in smallest currency unit
    val userMobileNumber: String? = null
)