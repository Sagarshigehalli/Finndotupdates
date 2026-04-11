package com.anomapro.finndot.ui.screens.settings

import com.anomapro.finndot.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.auth.AuthRepository
import com.anomapro.finndot.data.auth.CloudUserProfileService
import com.anomapro.finndot.data.auth.UserProfile
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UserProfileUiState(
    val name: String? = null,
    val occupation: String? = null,
    val hoursWorkedPerMonth: Int? = null,
    val avgIncome: Long? = null,
    val mobileNumber: String? = null,
    val signedInUser: UserProfile? = null,
    val saveMessage: String? = null
)

@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authRepository: AuthRepository,
    private val cloudUserProfileService: CloudUserProfileService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()
    
    init {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.userName,
                userPreferencesRepository.userOccupation,
                userPreferencesRepository.hoursWorkedPerMonth,
                userPreferencesRepository.avgIncome,
                userPreferencesRepository.userMobileNumber
            ) { name, occupation, hours, income, mobileNumber ->
                UserProfileUiState(
                    name = name,
                    occupation = occupation,
                    hoursWorkedPerMonth = hours,
                    avgIncome = income,
                    mobileNumber = mobileNumber,
                    signedInUser = authRepository.getCurrentUser()
                )
            }.collect { state ->
                _uiState.value = state.copy(signedInUser = authRepository.getCurrentUser())
            }
        }
    }
    
    fun saveProfile(
        name: String?,
        occupation: String?,
        hoursWorkedPerMonth: Int?,
        avgIncome: Long?,
        mobileNumber: String?
    ) {
        viewModelScope.launch {
            userPreferencesRepository.updateUserName(name)
            userPreferencesRepository.updateUserOccupation(occupation)
            userPreferencesRepository.updateHoursWorkedPerMonth(hoursWorkedPerMonth)
            userPreferencesRepository.updateAvgIncome(avgIncome)
            userPreferencesRepository.updateUserMobileNumber(mobileNumber)
            val syncResult = syncProfileToCloud(name, occupation, hoursWorkedPerMonth, avgIncome, mobileNumber)
            _uiState.update {
                it.copy(
                    saveMessage = syncResult.fold(
                        onSuccess = { "Profile saved" },
                        onFailure = { "Profile saved locally. Cloud sync failed." }
                    )
                )
            }
        }
    }
    
    fun clearSaveMessage() {
        _uiState.update { it.copy(saveMessage = null) }
    }
    
    private suspend fun syncProfileToCloud(
        name: String?,
        occupation: String?,
        hoursWorkedPerMonth: Int?,
        avgIncome: Long?,
        mobileNumber: String?
    ): Result<Unit> {
        val authUser = authRepository.getCurrentUser() ?: return Result.success(Unit)
        val profile = UserProfile(
            id = authUser.id,
            displayName = authUser.displayName ?: name,
            email = authUser.email,
            photoUrl = authUser.photoUrl,
            createdAt = authUser.createdAt,
            appVersion = BuildConfig.VERSION_NAME,
            lastActiveAt = System.currentTimeMillis(),
            name = name,
            occupation = occupation,
            hoursWorkedPerMonth = hoursWorkedPerMonth,
            avgIncome = avgIncome,
            mobileNumber = mobileNumber
        )
        return cloudUserProfileService.syncProfile(profile)
    }
}

