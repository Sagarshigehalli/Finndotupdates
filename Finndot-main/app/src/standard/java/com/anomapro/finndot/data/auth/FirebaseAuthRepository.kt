package com.anomapro.finndot.data.auth

import com.anomapro.finndot.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSignInService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val cloudUserProfileService: CloudUserProfileService
) {

    suspend fun signInWithGoogleIdToken(idToken: String): Result<UserProfile> = runCatching {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val currentUser = firebaseAuth.currentUser
        val result = if (currentUser?.isAnonymous == true) {
            try {
                currentUser.linkWithCredential(credential).await()
            } catch (e: Exception) {
                if (e.isCredentialAlreadyInUse()) {
                    // Google account already exists - sign out anonymous and sign in with Google
                    firebaseAuth.signOut()
                    firebaseAuth.signInWithCredential(credential).await()
                } else {
                    throw e
                }
            }
        } else {
            firebaseAuth.signInWithCredential(credential).await()
        }
        val user = result.user ?: throw IllegalStateException("Sign-in succeeded but no user")
        val profile = UserProfile(
            id = user.uid,
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            createdAt = System.currentTimeMillis(),
            appVersion = BuildConfig.VERSION_NAME
        )
        cloudUserProfileService.syncProfile(profile).onFailure {
            android.util.Log.w("FirebaseSignIn", "Profile sync failed (Firestore rules?), continuing sign-in", it)
        }
        profile
    }

    private fun Exception.isCredentialAlreadyInUse(): Boolean {
        if (this is FirebaseAuthException) {
            val code = errorCode
            return code == "ERROR_CREDENTIAL_ALREADY_IN_USE" ||
                code == "credential-already-in-use" ||
                code == "auth/credential-already-in-use"
        }
        val msg = message?.lowercase() ?: ""
        return msg.contains("credential") && (msg.contains("associated") || msg.contains("already in use"))
    }
}
