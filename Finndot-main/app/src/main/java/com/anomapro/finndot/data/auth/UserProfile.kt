package com.anomapro.finndot.data.auth

/**
 * Basic user profile data that can be safely stored in the cloud when user signs in.
 *
 * Privacy-safe fields only - no financial/SMS data.
 * See docs/onboarding-auth.md for full guidance.
 */
data class UserProfile(
    val id: String,
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val appVersion: String? = null,
    val lastActiveAt: Long = System.currentTimeMillis(),
    val name: String? = null,
    val occupation: String? = null,
    val hoursWorkedPerMonth: Int? = null,
    val avgIncome: Long? = null,
    val mobileNumber: String? = null,
    val accountDeleted: Boolean = false,
    val isAnonymous: Boolean = false
)
