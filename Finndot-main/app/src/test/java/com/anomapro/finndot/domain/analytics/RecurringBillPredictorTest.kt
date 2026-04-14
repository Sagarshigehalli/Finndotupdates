package com.anomapro.finndot.domain.analytics

import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

class RecurringBillPredictorTest {

    private val predictor = RecurringBillPredictor()

    @Test
    fun predict_identifiesFixedMonthlyBill() {
        val reference = LocalDate.of(2026, 4, 10)
        val txs = listOf(
            tx("Airtel Fiber", "999", LocalDateTime.of(2025, 11, 5, 9, 0)),
            tx("Airtel Fiber", "1000", LocalDateTime.of(2025, 12, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 1, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 2, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 3, 5, 9, 0)),
        )

        val result = predictor.predictAll(txs, reference).monthly
        assertEquals(1, result.size)
        val p = result.first()
        assertEquals("Airtel Fiber", p.merchant)
        assertEquals("INR", p.currency)
        assertEquals(RecurringBillType.FIXED, p.type)
        assertEquals(LocalDate.of(2026, 5, 5), p.nextDueDate)
        assertEquals(25, p.daysUntilDue)
    }

    @Test
    fun predict_identifiesVariableWhenVarianceAboveTenPercent() {
        val reference = LocalDate.of(2026, 4, 1)
        val txs = listOf(
            tx("Electricity Board", "1200", LocalDateTime.of(2025, 11, 12, 8, 0)),
            tx("Electricity Board", "1500", LocalDateTime.of(2025, 12, 12, 8, 0)),
            tx("Electricity Board", "1100", LocalDateTime.of(2026, 1, 12, 8, 0)),
            tx("Electricity Board", "1700", LocalDateTime.of(2026, 2, 12, 8, 0)),
        )

        val p = predictor.predictAll(txs, reference).monthly.first()
        assertEquals(RecurringBillType.VARIABLE, p.type)
    }

    @Test
    fun predict_nextDueClampsToLastDayOfFebruaryWhenTypicalDueIs30th() {
        val reference = LocalDate.of(2026, 2, 10)
        val txs = listOf(
            tx("Rent Co", "5000", LocalDateTime.of(2025, 11, 30, 9, 0)),
            tx("Rent Co", "5000", LocalDateTime.of(2025, 12, 30, 9, 0)),
            tx("Rent Co", "5000", LocalDateTime.of(2026, 1, 30, 9, 0)),
        )

        val p = predictor.predictAll(txs, reference).monthly.first()
        assertEquals(LocalDate.of(2026, 2, 28), p.nextDueDate)
    }

    @Test
    fun predict_returnsEmptyWhenAllSpendingAmountsAreZero() {
        val reference = LocalDate.of(2026, 4, 1)
        val txs = listOf(
            tx("Airtel Fiber", "0", LocalDateTime.of(2025, 12, 5, 9, 0)),
            tx("Airtel Fiber", "0", LocalDateTime.of(2026, 1, 5, 9, 0)),
            tx("Airtel Fiber", "0", LocalDateTime.of(2026, 2, 5, 9, 0)),
        )
        assertTrue(predictor.predictAll(txs, reference).monthly.isEmpty())
        assertTrue(predictor.predictAll(txs, reference).interval.isEmpty())
    }

    @Test
    fun predict_ignoresNonMonthlyAndInsufficientAppearances() {
        val reference = LocalDate.of(2026, 4, 1)
        val txs = listOf(
            tx("Coffee Shop", "250", LocalDateTime.of(2026, 2, 1, 10, 0)),
            tx("Coffee Shop", "260", LocalDateTime.of(2026, 2, 10, 10, 0)),
            tx("Coffee Shop", "255", LocalDateTime.of(2026, 2, 20, 10, 0)),
            tx("Random One", "999", LocalDateTime.of(2026, 3, 1, 10, 0)),
            tx("Random One", "999", LocalDateTime.of(2026, 4, 1, 10, 0)),
        )

        val result = predictor.predictAll(txs, reference)
        assertTrue(result.monthly.isEmpty())
        assertTrue(result.interval.isEmpty())
    }

    @Test
    fun predictAll_identifiesQuarterlyWithTwoPayments() {
        val reference = LocalDate.of(2026, 6, 1)
        val txs = listOf(
            tx("Jio", "899", LocalDateTime.of(2026, 1, 10, 10, 0)),
            tx("Jio", "899", LocalDateTime.of(2026, 4, 12, 10, 0)),
        )
        val all = predictor.predictAll(txs, reference)
        assertTrue(all.monthly.isEmpty())
        assertEquals(1, all.interval.size)
        val p = all.interval.first()
        assertEquals("Jio", p.merchant)
        assertEquals(RecurringBillCadence.INTERVAL, p.cadence)
        assertEquals(92, p.medianIntervalDays)
        assertEquals(RecurringBillType.FIXED, p.type)
        assertTrue(p.daysUntilDue >= 0)
    }

    @Test
    fun predictAll_intervalDetectsPairWhenPaymentsFallOutsideSixMonthMonthlyWindow() {
        val reference = LocalDate.of(2026, 6, 15)
        val txs = listOf(
            tx("Jio", "899", LocalDateTime.of(2025, 5, 1, 10, 0)),
            tx("Jio", "899", LocalDateTime.of(2025, 8, 1, 10, 0)),
        )
        val all = predictor.predictAll(txs, reference)
        assertTrue(all.monthly.isEmpty())
        assertEquals(1, all.interval.size)
        assertEquals(RecurringBillCadence.INTERVAL, all.interval.first().cadence)
    }

    @Test
    fun predictAll_monthlyWinsWhenPatternIsMonthly() {
        val reference = LocalDate.of(2026, 4, 10)
        val txs = listOf(
            tx("Airtel Fiber", "999", LocalDateTime.of(2025, 11, 5, 9, 0)),
            tx("Airtel Fiber", "1000", LocalDateTime.of(2025, 12, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 1, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 2, 5, 9, 0)),
            tx("Airtel Fiber", "999", LocalDateTime.of(2026, 3, 5, 9, 0)),
        )
        val all = predictor.predictAll(txs, reference)
        assertEquals(1, all.monthly.size)
        assertEquals(0, all.interval.size)
        assertEquals(RecurringBillCadence.MONTHLY, all.monthly.first().cadence)
    }

    private fun tx(merchant: String, amount: String, at: LocalDateTime) = TransactionEntity(
        amount = BigDecimal(amount),
        merchantName = merchant,
        category = "Bills & Utilities",
        transactionType = TransactionType.EXPENSE,
        dateTime = at,
        transactionHash = "$merchant-$amount-$at",
    )
}
