package com.anomapro.finndot.domain.analytics

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.domain.service.TransferLikeSmsClassifier
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDateTime

class SpendingAnalyticsFilterTest {

    @Test
    fun transferType_neverTrueSpending() {
        val t = base(TransactionType.TRANSFER, "Bank transfer")
        assertFalse(SpendingAnalyticsFilter.countsAsTrueSpending(t))
        assertFalse(SpendingAnalyticsFilter.countsAsTrueIncome(t))
    }

    @Test
    fun expense_bankTransferCategory_excluded() {
        val t = base(TransactionType.EXPENSE, TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY)
        assertFalse(SpendingAnalyticsFilter.countsAsTrueSpending(t))
    }

    @Test
    fun expense_normalMerchant_included() {
        val t = base(TransactionType.EXPENSE, "Food & Dining")
        assertTrue(SpendingAnalyticsFilter.countsAsTrueSpending(t))
    }

    @Test
    fun income_bankTransferCategory_excluded() {
        val t = base(TransactionType.INCOME, TransferLikeSmsClassifier.HEURISTIC_TRANSFER_CATEGORY)
        assertFalse(SpendingAnalyticsFilter.countsAsTrueIncome(t))
    }

    @Test
    fun income_salary_included() {
        val t = base(TransactionType.INCOME, "Salary")
        assertTrue(SpendingAnalyticsFilter.countsAsTrueIncome(t))
    }

    private fun base(type: TransactionType, category: String) = TransactionEntity(
        amount = BigDecimal("100"),
        merchantName = "X",
        category = category,
        transactionType = type,
        dateTime = LocalDateTime.now(),
        transactionHash = "h",
    )
}
