package com.anomapro.finndot.data.auth

import android.app.Activity
import javax.inject.Inject

class NoOpGoogleSignInHelper @Inject constructor() : GoogleSignInHelper {
    override suspend fun getGoogleIdToken(activity: Activity): Result<String> =
        Result.failure(UnsupportedOperationException("Google Sign-In not available on F-Droid"))
}
