package com.anomapro.finndot.domain.analytics

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.domain.service.TransferLikeSmsClassifier
import java.util.Locale

/**
 * Consistent rules for **operational** (true) spending and income vs internal / rail transfers (Part 7).
 *
 * - [TransactionType.TRANSFER] never counts as discretionary spend or as salary-like income.
 * - Rows categorized like bank / account transfers (even if type was not upgraded yet) are excluded
 *   from spend and income totals used for budgets, home summaries, analytics, and notifications.
 */
object SpendingAnalyticsFilter {

    private val transferLikeCategoriesLowercase: Set<String> = setOf(
        TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY.lowercase(Locale.ROOT),
        "internal transfer",
        "account transfer",
    )

    fun isTransferLikeCategory(category: String?): Boolean {
        if (category.isNullOrBlank()) return false
        val c = category.trim().lowercase(Locale.ROOT)
        if (c in transferLikeCategoriesLowercase) return true
        if (c.contains("transfer")) {
            if (c.contains("bank") || c.contains("neft") || c.contains("imps") || c.contains("rtgs")) {
                return true
            }
        }
        return false
    }

    fun excludesFromOperationalTotals(tx: TransactionEntity): Boolean {
        if (tx.transactionType == TransactionType.TRANSFER) return true
        return isTransferLikeCategory(tx.category)
    }

    /** Outflows that count toward “true spending”, budgets, and daily spend (expense + card spend). */
    fun countsAsTrueSpending(tx: TransactionEntity): Boolean {
        if (tx.transactionType == TransactionType.TRANSFER || tx.transactionType == TransactionType.INVESTMENT) {
            return false
        }
        if (tx.transactionType != TransactionType.EXPENSE && tx.transactionType != TransactionType.CREDIT) {
            return false
        }
        return !isTransferLikeCategory(tx.category)
    }

    /** Inflows that count toward earned income-style totals (excludes transfer legs). */
    fun countsAsTrueIncome(tx: TransactionEntity): Boolean {
        if (tx.transactionType != TransactionType.INCOME) return false
        return !isTransferLikeCategory(tx.category)
    }
}
