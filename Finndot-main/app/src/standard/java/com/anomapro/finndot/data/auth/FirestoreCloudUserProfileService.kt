package com.anomapro.finndot.data.auth

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of CloudUserProfileService.
 * Stores user profiles in the 'user_profiles' collection.
 *
 * Collection structure:
 * - id: document ID (user's Firebase Auth UID)
 * - displayName: string?
 * - email: string?
 * - photoUrl: string?
 * - createdAt: long
 * - appVersion: string?
 */
@Singleton
class FirestoreCloudUserProfileService @Inject constructor(
    private val firestore: FirebaseFirestore
) : CloudUserProfileService {

    private val collection = firestore.collection(COLLECTION_NAME)

    override suspend fun syncProfile(profile: UserProfile): Result<Unit> = runCatching {
        val data = hashMapOf<String, Any?>(
            "displayName" to profile.displayName,
            "email" to profile.email,
            "photoUrl" to profile.photoUrl,
            "createdAt" to profile.createdAt,
            "appVersion" to profile.appVersion,
            "lastActiveAt" to profile.lastActiveAt,
            "name" to profile.name,
            "occupation" to profile.occupation,
            "hoursWorkedPerMonth" to profile.hoursWorkedPerMonth,
            "avgIncome" to profile.avgIncome,
            "mobileNumber" to profile.mobileNumber,
            "accountDeleted" to profile.accountDeleted
        )
        collection.document(profile.id).set(data).await()
    }

    override suspend fun updateLastActive(userId: String): Result<Unit> = runCatching {
        val data = hashMapOf(
            "lastActiveAt" to System.currentTimeMillis()
        )
        collection.document(userId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    override suspend fun markAccountDeleted(profile: UserProfile): Result<Unit> = runCatching {
        val data = hashMapOf<String, Any?>(
            "displayName" to profile.displayName,
            "email" to profile.email,
            "accountDeleted" to true,
            "lastActiveAt" to System.currentTimeMillis()
        )
        collection.document(profile.id).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
    }

    companion object {
        private const val COLLECTION_NAME = "user_profiles"
    }
}
