package com.anomapro.finndot.domain.model

import com.anomapro.finndot.data.database.entity.TransactionEntity
import java.util.Locale

/**
 * Normalized identity for matching SMS legs to accounts the user has linked or
 * that appear in balance history. Used for internal-transfer pairing (both legs "mine").
 *
 * Equality is case-insensitive on bank name and trimmed last4 so minor SMS vs UI
 * differences still match.
 */
data class LinkedAccountKey(
    val bankNameNormalized: String,
    val last4: String
) {
    init {
        require(bankNameNormalized.isNotBlank())
        require(last4.isNotBlank())
    }

    companion object {
        fun fromRaw(bankName: String?, last4: String?): LinkedAccountKey? {
            val b = bankName?.trim()?.lowercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return null
            val l = last4?.trim()?.takeIf { it.isNotBlank() } ?: return null
            return LinkedAccountKey(b, l)
        }

        fun fromTransactionLeg(transaction: TransactionEntity): LinkedAccountKey? =
            fromRaw(transaction.bankName, transaction.accountNumber)
    }
}
