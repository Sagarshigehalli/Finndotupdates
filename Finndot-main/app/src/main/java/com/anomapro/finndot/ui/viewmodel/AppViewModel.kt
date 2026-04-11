package com.anomapro.finndot.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.auth.AuthRepository
import com.anomapro.finndot.data.manager.DailySummaryNotificationManager
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for app-level state used to determine initial navigation.
 * Resolves onboarding status before showing NavHost to prevent login screen flash.
 * Ensures anonymous auth for analytics (standard build only).
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val dailySummaryNotificationManager: DailySummaryNotificationManager
) : ViewModel() {

    private val _startDestinationReady = MutableStateFlow<Boolean?>(null)
    val startDestinationReady: StateFlow<Boolean?> = _startDestinationReady.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.ensureAnonymousAuth()
            dailySummaryNotificationManager.scheduleDailyNotification()
            val hasCompleted = userPreferencesRepository.hasCompletedOnboarding().first()
            _startDestinationReady.value = hasCompleted
        }
    }
}
