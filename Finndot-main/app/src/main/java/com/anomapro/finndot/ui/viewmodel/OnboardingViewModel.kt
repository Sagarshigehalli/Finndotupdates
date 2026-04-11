package com.anomapro.finndot.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.auth.AuthRepository
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _navigateNext = MutableSharedFlow<Unit>()
    val navigateNext: SharedFlow<Unit> = _navigateNext.asSharedFlow()

    init {
        viewModelScope.launch {
            if (userPreferencesRepository.hasCompletedOnboarding().first()) {
                _navigateNext.emit(Unit)
            }
        }
    }

    fun onSignInWithGoogle(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.signInWithGoogle(activity)
                .onSuccess {
                    userPreferencesRepository.setOnboardingCompleted(true)
                    _navigateNext.emit(Unit)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Sign in failed"
                        )
                    }
                }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onSkipSignIn() {
        viewModelScope.launch {
            userPreferencesRepository.setOnboardingCompleted(true)
            _navigateNext.emit(Unit)
        }
    }
}
