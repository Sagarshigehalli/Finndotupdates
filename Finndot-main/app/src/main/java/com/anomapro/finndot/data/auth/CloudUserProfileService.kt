package com.anomapro.finndot.data.auth

/**
 * Interface for syncing user profile to a cloud backend when user signs in.
 *
 * Implement with Firebase Firestore, Supabase, or your own API.
 * See docs/onboarding-auth.md for setup instructions.
 */
interface CloudUserProfileService {
    suspend fun syncProfile(profile: UserProfile): Result<Unit>

    /**
     * Update last active timestamp for a user. No-op if not implemented.
     */
    suspend fun updateLastActive(userId: String): Result<Unit> = Result.success(Unit)

    /**
     * Mark user account as deleted. Keeps only displayName and email in cloud.
     * No-op if not implemented.
     */
    suspend fun markAccountDeleted(profile: UserProfile): Result<Unit> = Result.success(Unit)
}
