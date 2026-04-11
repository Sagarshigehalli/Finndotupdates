package com.anomapro.finndot.data.auth

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StandardAuthRepository @Inject constructor(
    private val googleSignInHelper: GoogleSignInHelper,
    private val firebaseSignInService: FirebaseSignInService,
    private val firebaseAuth: FirebaseAuth
) : AuthRepository {

    override suspend fun signInWithGoogle(activity: Activity): Result<UserProfile> {
        val idTokenResult = googleSignInHelper.getGoogleIdToken(activity)
        return idTokenResult.fold(
            onSuccess = { idToken -> firebaseSignInService.signInWithGoogleIdToken(idToken) },
            onFailure = { Result.failure(it) }
        )
    }

    override suspend fun signOut() {
        firebaseAuth.signOut()
    }

    override fun getCurrentUser(): UserProfile? {
        val user = firebaseAuth.currentUser ?: return null
        return UserProfile(
            id = user.uid,
            displayName = user.displayName,
            email = user.email,
            photoUrl = user.photoUrl?.toString(),
            isAnonymous = user.isAnonymous
        )
    }

    override suspend fun ensureAnonymousAuth() {
        if (firebaseAuth.currentUser == null) {
            try {
                firebaseAuth.signInAnonymously().await()
            } catch (_: Exception) {
                // Anonymous Auth may be disabled in Firebase Console - app continues without it
            }
        }
    }
}
