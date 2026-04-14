package com.anomapro.finndot.domain.service

import com.anomapro.finndot.core.CompiledPatterns
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Flags obvious bank-rail / account-to-account transfer SMS when [InternalTransferPairingService]
 * cannot yet pair two legs (single account, delay, etc.).
 *
 * **Order vs rule engine:** Call [applyAfterRules] **after** rules have been evaluated on the
 * transaction so user/template rules can set type or category first. This classifier **does not**
 * override rows that are already [TransactionType.TRANSFER], [TransactionType.CREDIT], or
 * [TransactionType.INVESTMENT], or **recurring** (subscription) rows.
 */
@Singleton
class TransferLikeSmsClassifier @Inject constructor() {

    companion object {
        /** Category applied when promoting EXPENSE/INCOME to TRANSFER via SMS heuristic. */
        const val HEURISTIC_TRANSFER_CATEGORY: String = "Bank transfer"
    }

    /**
     * If SMS looks like NEFT/IMPS/RTGS-style transfer and does not look like POS/merchant spend,
     * promotes [TransactionEntity.transactionType] to [TransactionType.TRANSFER] and sets
     * [HEURISTIC_TRANSFER_CATEGORY]. Otherwise returns [entity] unchanged.
     */
    fun applyAfterRules(entity: TransactionEntity, smsBody: String?): TransactionEntity {
        if (smsBody.isNullOrBlank()) return entity
        if (entity.isRecurring) return entity
        when (entity.transactionType) {
            TransactionType.EXPENSE, TransactionType.INCOME -> Unit
            else -> return entity
        }
        if (CompiledPatterns.TransferHeuristic.MERCHANT_OR_POS_BLOCK.containsMatchIn(smsBody)) {
            return entity
        }
        if (!CompiledPatterns.TransferHeuristic.TRANSFER_RAILS.containsMatchIn(smsBody)) {
            return entity
        }
        val now = LocalDateTime.now()
        return entity.copy(
            transactionType = TransactionType.TRANSFER,
            category = HEURISTIC_TRANSFER_CATEGORY,
            updatedAt = now
        )
    }
}
