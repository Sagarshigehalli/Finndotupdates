package com.anomapro.finndot.data.auth

import android.app.Activity

/**
 * Repository for authentication operations.
 * Standard: Firebase Auth + Google Sign-In
 * F-Droid: No-op
 */
interface AuthRepository {
    suspend fun signInWithGoogle(activity: Activity): Result<UserProfile>
    suspend fun signOut()
    fun getCurrentUser(): UserProfile?

    /**
     * Ensures a Firebase Auth user exists (for analytics). If not signed in,
     * signs in anonymously. No-op on F-Droid.
     */
    suspend fun ensureAnonymousAuth()
}
