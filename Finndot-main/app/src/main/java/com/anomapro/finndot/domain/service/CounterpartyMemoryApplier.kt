package com.anomapro.finndot.domain.service

import android.util.Log
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.preferences.CounterpartyMemoryRepository
import com.anomapro.finndot.domain.model.CounterpartyMemoryKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies persisted user choices for recurring counterparties (Part 6 — "ask once").
 *
 * Runs **after** merchant name normalization and merchant→category mapping, **before**
 * the rule engine, so explicit active rules can still override.
 */
@Singleton
class CounterpartyMemoryApplier @Inject constructor(
    private val counterpartyMemoryRepository: CounterpartyMemoryRepository,
) {
    suspend fun applyAfterMerchantMapping(entity: TransactionEntity): TransactionEntity {
        val key = CounterpartyMemoryKey.fromEntity(entity) ?: return entity
        val entry = counterpartyMemoryRepository.get(key) ?: return entity
        val type = runCatching { TransactionType.valueOf(entry.transactionType) }.getOrElse {
            Log.w(TAG, "Ignoring counterparty memory with unknown type: ${entry.transactionType}")
            return entity
        }
        if (type == entity.transactionType && entry.category == entity.category) {
            return entity
        }
        Log.d(TAG, "Applied counterparty memory for key=$key → type=$type category=${entry.category}")
        return entity.copy(
            transactionType = type,
            category = entry.category,
            updatedAt = java.time.LocalDateTime.now(),
        )
    }

    private companion object {
        private const val TAG = "CounterpartyMemory"
    }
}
