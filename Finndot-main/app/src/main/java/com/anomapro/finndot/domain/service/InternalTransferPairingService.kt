package com.anomapro.finndot.domain.service

import android.util.Log
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.TransactionRepository
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Pairs two SMS legs of the same internal (own-account) transfer using amount, time window,
 * opposite [TransactionType] (EXPENSE vs INCOME), and [LinkedAccountsProvider] — no SMS keywords.
 *
 * When a match is found, both rows are set to [TransactionType.TRANSFER] with shared
 * [TransactionEntity.fromAccount] / [TransactionEntity.toAccount] labels (debit → credit).
 */
@Singleton
class InternalTransferPairingService @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val linkedAccountsProvider: LinkedAccountsProvider
) {
    companion object {
        private const val TAG = "InternalTransferPairing"
        private const val WINDOW_MINUTES = 30L
    }

    suspend fun tryPairAfterInsert(anchor: TransactionEntity) {
        if (anchor.id == 0L) return
        if (anchor.isDeleted) return
        if (anchor.transactionType == TransactionType.TRANSFER) return

        val oppositeType = when (anchor.transactionType) {
            TransactionType.EXPENSE -> TransactionType.INCOME
            TransactionType.INCOME -> TransactionType.EXPENSE
            else -> return
        }

        val ownedKeys = linkedAccountsProvider.loadOwnedAccountKeys()
        if (!linkedAccountsProvider.isLegOwned(anchor, ownedKeys)) {
            Log.d(TAG, "Skip pairing: anchor not in linked-account set (id=${anchor.id})")
            return
        }

        val windowStart = anchor.dateTime.minusMinutes(WINDOW_MINUTES)
        val windowEnd = anchor.dateTime.plusMinutes(WINDOW_MINUTES)

        val candidates = transactionRepository.findInternalTransferPairCandidatesForAnchor(
            anchor = anchor,
            oppositeType = oppositeType,
            windowStart = windowStart,
            windowEnd = windowEnd,
            limit = 25
        )

        val match = candidates
            .asSequence()
            .filter { !it.isDeleted && it.transactionType == oppositeType }
            .filter { linkedAccountsProvider.isLegOwned(it, ownedKeys) }
            .filter { linkedAccountsProvider.areBothLegsOwned(anchor, it, ownedKeys) }
            .minByOrNull {
                abs(Duration.between(anchor.dateTime, it.dateTime).seconds)
            }
            ?: return

        val (expenseLeg, incomeLeg) = if (anchor.transactionType == TransactionType.EXPENSE) {
            anchor to match
        } else {
            match to anchor
        }

        val fromLabel = accountLabel(expenseLeg.bankName, expenseLeg.accountNumber)
        val toLabel = accountLabel(incomeLeg.bankName, incomeLeg.accountNumber)
        if (fromLabel == null || toLabel == null) {
            Log.d(TAG, "Skip pairing: missing bank/account for labels (anchor id=${anchor.id})")
            return
        }

        val now = LocalDateTime.now()
        val anchorUpdated = anchor.copy(
            transactionType = TransactionType.TRANSFER,
            fromAccount = fromLabel,
            toAccount = toLabel,
            updatedAt = now
        )
        val matchUpdated = match.copy(
            transactionType = TransactionType.TRANSFER,
            fromAccount = fromLabel,
            toAccount = toLabel,
            updatedAt = now
        )

        transactionRepository.updateTransaction(anchorUpdated)
        transactionRepository.updateTransaction(matchUpdated)
        Log.d(
            TAG,
            "Paired internal transfer: ${anchor.id} <-> ${match.id} ($fromLabel -> $toLabel)"
        )
    }

    private fun accountLabel(bankName: String?, last4: String?): String? {
        val l = last4?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val bank = bankName?.trim()?.takeIf { it.isNotBlank() } ?: "Account"
        return "$bank **$l"
    }
}
