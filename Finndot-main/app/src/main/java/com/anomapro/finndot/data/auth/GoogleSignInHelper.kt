package com.anomapro.finndot.data.auth

import android.app.Activity

/**
 * Helper for Google Sign-In flow.
 * Returns the Google ID token on success, for use with Firebase Auth.
 *
 * Standard flavor: Uses Credential Manager
 * F-Droid flavor: No-op (never called when GOOGLE_SIGNIN_AVAILABLE is false)
 */
interface GoogleSignInHelper {
    suspend fun getGoogleIdToken(activity: Activity): Result<String>
}
