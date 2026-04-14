package com.anomapro.finndot.domain.usecase

import android.util.Log
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Part 8 — runs once per install: upgrades legacy EXPENSE/INCOME rows whose category is already
 * “Bank transfer” to [com.anomapro.finndot.data.database.entity.TransactionType.TRANSFER].
 */
@Singleton
class TransferSemanticsBackfillUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend fun runIfNeeded() {
        if (userPreferencesRepository.hasTransferCategoryBackfillRun()) return
        val updated = transactionRepository.backfillLegacyBankTransferCategoryRows()
        userPreferencesRepository.setTransferCategoryBackfillRun(true)
        if (updated > 0) {
            Log.d(TAG, "Transfer category backfill updated $updated row(s)")
        }
    }

    private companion object {
        private const val TAG = "TransferBackfill"
    }
}
