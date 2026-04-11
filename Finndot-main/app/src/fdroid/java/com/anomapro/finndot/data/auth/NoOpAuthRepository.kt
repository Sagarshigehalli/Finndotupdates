package com.anomapro.finndot.data.auth

import android.app.Activity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpAuthRepository @Inject constructor() : AuthRepository {
    override suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> =
        Result.failure(UnsupportedOperationException("Google Sign-In not available"))

    override suspend fun signOut() {}

    override fun getCurrentUser(): UserProfile? = null

    override suspend fun ensureAnonymousAuth() {}
}
