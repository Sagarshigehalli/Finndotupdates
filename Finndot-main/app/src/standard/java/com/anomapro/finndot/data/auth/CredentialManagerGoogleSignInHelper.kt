package com.anomapro.finndot.data.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.anomapro.finndot.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class CredentialManagerGoogleSignInHelper(
    private val credentialManager: CredentialManager
) : GoogleSignInHelper {

    override suspend fun getGoogleIdToken(activity: Activity): Result<String> = runCatching {
        val webClientId = BuildConfig.FIREBASE_WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            throw IllegalStateException(
                "FIREBASE_WEB_CLIENT_ID not configured. Add to local.properties from Firebase Console."
            )
        }

        // GetSignInWithGoogleOption is designed for explicit "Sign in with Google" button taps.
        // It shows all accounts (including signed-out) and handles add-account flow better than
        // GetGoogleIdOption, which can cause "no credential available" on some devices.
        val signInOption = GetSignInWithGoogleOption.Builder(webClientId).build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        val response = credentialManager.getCredential(activity, request)
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            googleCredential.idToken
        } else {
            throw IllegalStateException("Invalid credential type")
        }
    }.recoverCatching { e ->
        val msg = e.message?.lowercase() ?: ""
        when {
            msg.contains("no credential") || msg.contains("no credentials available") -> {
                throw IllegalStateException(
                    "No credentials available. Enable \"Sign in with Google\" for third-party apps in your Google account settings (myaccount.google.com/connections/settings)."
                )
            }
            // Error 16 / "account reauth failed" - typically SHA fingerprint not registered for release builds
            msg.contains("16") && msg.contains("reauth") -> {
                throw IllegalStateException(
                    "Sign-in failed (16). Add the release SHA-1 fingerprint to Firebase Console → Project Settings → Your apps. For Play Store: use App Integrity → App signing key certificate."
                )
            }
            msg.contains("16") || msg.contains("reauth failed") -> {
                throw IllegalStateException(
                    "Sign-in failed. Add the release SHA-1 fingerprint to Firebase Console. For Play Store apps, use the SHA from App Integrity."
                )
            }
            else -> throw e
        }
    }
}
