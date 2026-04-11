package com.anomapro.finndot.data.analytics

import com.anomapro.finndot.data.auth.CloudUserProfileService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firestore implementation of UsageStatsService.
 * Stores screen visit counts in user_usage/{userId}.
 * Also updates lastActiveAt in user_profiles.
 *
 * Document structure (user_usage):
 * - {screenId}_visits: number (e.g. home_visits, transactions_visits)
 * - last_{screenId}_visit: timestamp
 * - lastUpdated: timestamp
 */
@Singleton
class FirestoreUsageStatsService @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val cloudUserProfileService: CloudUserProfileService
) : UsageStatsService {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val collection = firestore.collection(COLLECTION_NAME)

    override fun recordScreenVisit(screenId: String) {
        val user = firebaseAuth.currentUser ?: return
        val userId = user.uid
        val sanitized = screenId.sanitizeForFirestore()
        if (sanitized.isEmpty()) return

        scope.launch {
            try {
                val docRef = collection.document(userId)
                val updates = mutableMapOf<String, Any>(
                    "${sanitized}_visits" to FieldValue.increment(1),
                    "last_${sanitized}_visit" to System.currentTimeMillis(),
                    "lastUpdated" to System.currentTimeMillis(),
                    "isAnonymous" to user.isAnonymous
                )
                docRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                if (!user.isAnonymous) {
                    cloudUserProfileService.updateLastActive(userId)
                }
            } catch (_: Exception) {
                // Silently ignore analytics failures
            }
        }
    }

    override fun recordScreenDuration(screenId: String, durationMs: Long) {
        if (durationMs <= 0) return
        val user = firebaseAuth.currentUser ?: return
        val userId = user.uid
        val sanitized = screenId.sanitizeForFirestore()
        if (sanitized.isEmpty()) return

        scope.launch {
            try {
                val docRef = collection.document(userId)
                val updates = mutableMapOf<String, Any>(
                    "${sanitized}_time_ms" to FieldValue.increment(durationMs),
                    "lastUpdated" to System.currentTimeMillis(),
                    "isAnonymous" to user.isAnonymous
                )
                docRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
            } catch (_: Exception) {
                // Silently ignore analytics failures
            }
        }
    }

    override fun recordEvent(eventId: String) {
        val user = firebaseAuth.currentUser ?: return
        val userId = user.uid
        val sanitized = eventId.sanitizeForFirestore()
        if (sanitized.isEmpty()) return

        scope.launch {
            try {
                val docRef = collection.document(userId)
                val updates = mutableMapOf<String, Any>(
                    "${sanitized}_count" to FieldValue.increment(1),
                    "last_${sanitized}_at" to System.currentTimeMillis(),
                    "lastUpdated" to System.currentTimeMillis(),
                    "isAnonymous" to user.isAnonymous
                )
                docRef.set(updates, com.google.firebase.firestore.SetOptions.merge()).await()
                if (!user.isAnonymous) {
                    cloudUserProfileService.updateLastActive(userId)
                }
            } catch (_: Exception) {
                // Silently ignore analytics failures
            }
        }
    }

    private fun String.sanitizeForFirestore(): String =
        replace(Regex("[^a-zA-Z0-9_]"), "_").take(50)

    companion object {
        private const val COLLECTION_NAME = "user_usage"
    }
}
