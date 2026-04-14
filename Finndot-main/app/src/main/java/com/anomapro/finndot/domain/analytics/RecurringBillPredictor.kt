package com.anomapro.finndot.domain.analytics

import com.anomapro.finndot.data.database.entity.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

enum class RecurringBillType {
    FIXED,
    VARIABLE,
}

/** Monthly vs longer fixed-interval (e.g. quarterly) recurring spend. */
enum class RecurringBillCadence {
    MONTHLY,
    INTERVAL,
}

data class RecurringBillPrediction(
    val merchant: String,
    val currency: String,
    val type: RecurringBillType,
    val expectedAmount: BigDecimal,
    val nextDueDate: LocalDate,
    val daysUntilDue: Int,
    val monthsAppeared: Int,
    val occurrences: Int,
    val cadence: RecurringBillCadence = RecurringBillCadence.MONTHLY,
    /** Median days between payments; set for [RecurringBillCadence.INTERVAL] predictions. */
    val medianIntervalDays: Int? = null,
)

data class RecurringBillPredictionResult(
    val monthly: List<RecurringBillPrediction>,
    val interval: List<RecurringBillPrediction>,
)

@Singleton
class RecurringBillPredictor @Inject constructor() {

    /**
     * @deprecated Use [predictAll] for monthly + longer-interval recurring.
     */
    fun predict(
        transactions: List<TransactionEntity>,
        referenceDate: LocalDate = LocalDate.now(),
    ): List<RecurringBillPrediction> = predictAll(transactions, referenceDate).monthly

    fun predictAll(
        transactions: List<TransactionEntity>,
        referenceDate: LocalDate = LocalDate.now(),
    ): RecurringBillPredictionResult {
        if (transactions.isEmpty()) {
            return RecurringBillPredictionResult(emptyList(), emptyList())
        }

        val endMonth = YearMonth.from(referenceDate)
        val monthlyWindowStart = endMonth.minusMonths(5)
        val extendedStartMonth = endMonth.minusMonths(17)

        val candidates = transactions
            .asSequence()
            .filter { !it.isDeleted }
            .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
            .filter { it.amount.abs() > BigDecimal.ZERO }
            .filter { YearMonth.from(it.dateTime) in extendedStartMonth..endMonth }
            .filter { isMerchantUsable(it.normalizedMerchantName ?: it.merchantName) }
            .groupBy {
                val merchant = normalizeMerchant(it.normalizedMerchantName ?: it.merchantName)
                "$merchant|${it.currency.uppercase(Locale.ROOT)}"
            }

        val monthly = mutableListOf<RecurringBillPrediction>()
        val interval = mutableListOf<RecurringBillPrediction>()

        for ((_, txs) in candidates) {
            val sortedFull = txs.sortedBy { it.dateTime }
            val sortedMonthlyWindow = sortedFull.filter {
                val ym = YearMonth.from(it.dateTime)
                ym >= monthlyWindowStart && ym <= endMonth
            }
            predictMonthly(sortedMonthlyWindow, referenceDate)?.let { monthly.add(it) }
                ?: predictInterval(sortedFull, referenceDate)?.let { interval.add(it) }
        }

        val sortPreds = compareBy<RecurringBillPrediction> { it.daysUntilDue }
            .thenByDescending { it.expectedAmount }

        return RecurringBillPredictionResult(
            monthly = monthly.sortedWith(sortPreds),
            interval = interval.sortedWith(sortPreds),
        )
    }

    /** ≥3 distinct months, ≥2 gaps in 25–35 days; next due from calendar day-of-month. */
    private fun predictMonthly(sorted: List<TransactionEntity>, referenceDate: LocalDate): RecurringBillPrediction? {
        val monthsAppeared = sorted.map { YearMonth.from(it.dateTime) }.distinct().size
        if (monthsAppeared < 3) return null

        val gaps = sorted.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a.dateTime.toLocalDate(), b.dateTime.toLocalDate()).toInt()
        }
        if (gaps.size < 2) return null
        val monthlyGapMatches = gaps.count { it in MONTHLY_GAP_MIN..MONTHLY_GAP_MAX }
        if (monthlyGapMatches < 2) return null

        return buildPrediction(sorted, referenceDate, RecurringBillCadence.MONTHLY, null)
    }

    /**
     * ≥2 payments, median gap in [40, 220] days (~6–7 months max), gaps consistent when >1 gap.
     * Next due = last payment + k * median gap until after reference date.
     */
    private fun predictInterval(sorted: List<TransactionEntity>, referenceDate: LocalDate): RecurringBillPrediction? {
        if (sorted.size < 2) return null

        val gaps = sorted.zipWithNext { a, b ->
            ChronoUnit.DAYS.between(a.dateTime.toLocalDate(), b.dateTime.toLocalDate()).toInt()
        }
        val medianGap = medianInt(gaps)
        if (medianGap < INTERVAL_GAP_MIN || medianGap > INTERVAL_GAP_MAX) return null

        if (gaps.size >= 2) {
            val tolerance = maxOf(14, (medianGap * 0.22).toInt())
            if (gaps.any { abs(it - medianGap) > tolerance }) return null
        }

        // Exclude patterns that are really monthly (handled in monthly branch first — monthly runs before this is only called from elvis when monthly returns null, so gaps won't be 2 monthly matches)

        val lastPaid = sorted.last().dateTime.toLocalDate()
        val nextDue = nextDueFromInterval(lastPaid, medianGap, referenceDate)
        val daysUntil = ChronoUnit.DAYS.between(referenceDate, nextDue).toInt()

        val amounts = sorted.map { it.amount.abs().setScale(2, RoundingMode.HALF_UP) }
        val expectedAmount = median(amounts)
        val varianceRatio = relativeVariance(amounts, expectedAmount)
        val type = if (varianceRatio <= BigDecimal("0.10")) {
            RecurringBillType.FIXED
        } else {
            RecurringBillType.VARIABLE
        }

        return RecurringBillPrediction(
            merchant = displayMerchant(sorted.first()),
            currency = sorted.first().currency.uppercase(Locale.ROOT),
            type = type,
            expectedAmount = expectedAmount,
            nextDueDate = nextDue,
            daysUntilDue = daysUntil,
            monthsAppeared = sorted.map { YearMonth.from(it.dateTime) }.distinct().size,
            occurrences = sorted.size,
            cadence = RecurringBillCadence.INTERVAL,
            medianIntervalDays = medianGap,
        )
    }

    private fun buildPrediction(
        sorted: List<TransactionEntity>,
        referenceDate: LocalDate,
        cadence: RecurringBillCadence,
        medianIntervalDays: Int?,
    ): RecurringBillPrediction {
        val amounts = sorted.map { it.amount.abs().setScale(2, RoundingMode.HALF_UP) }
        val expectedAmount = median(amounts)
        val varianceRatio = relativeVariance(amounts, expectedAmount)
        val type = if (varianceRatio <= BigDecimal("0.10")) {
            RecurringBillType.FIXED
        } else {
            RecurringBillType.VARIABLE
        }

        val dueDay = dominantDueDay(sorted.map { it.dateTime.toLocalDate().dayOfMonth })
        val nextDue = computeNextDueDate(referenceDate, dueDay)
        val daysUntil = ChronoUnit.DAYS.between(referenceDate, nextDue).toInt()
        val monthsAppeared = sorted.map { YearMonth.from(it.dateTime) }.distinct().size

        return RecurringBillPrediction(
            merchant = displayMerchant(sorted.first()),
            currency = sorted.first().currency.uppercase(Locale.ROOT),
            type = type,
            expectedAmount = expectedAmount,
            nextDueDate = nextDue,
            daysUntilDue = daysUntil,
            monthsAppeared = monthsAppeared,
            occurrences = sorted.size,
            cadence = cadence,
            medianIntervalDays = medianIntervalDays,
        )
    }

    private fun displayMerchant(tx: TransactionEntity): String {
        return (tx.normalizedMerchantName ?: tx.merchantName).trim()
    }

    private fun normalizeMerchant(raw: String): String = raw.trim().lowercase(Locale.ROOT)

    private fun isMerchantUsable(raw: String): Boolean {
        val normalized = raw.trim().lowercase(Locale.ROOT)
        return normalized.isNotBlank() && normalized !in setOf(
            "unknown",
            "unknown merchant",
            "na",
            "n/a",
            "not available",
            "null",
            "-",
        )
    }

    private fun median(values: List<BigDecimal>): BigDecimal {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]).divide(BigDecimal("2"), 2, RoundingMode.HALF_UP)
        }
    }

    private fun medianInt(values: List<Int>): Int {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2
        }
    }

    private fun relativeVariance(values: List<BigDecimal>, median: BigDecimal): BigDecimal {
        if (median <= BigDecimal.ZERO) return BigDecimal.ZERO
        val max = values.maxOrNull() ?: return BigDecimal.ZERO
        val min = values.minOrNull() ?: return BigDecimal.ZERO
        return max.subtract(min)
            .divide(median, 4, RoundingMode.HALF_UP)
            .abs()
    }

    private fun dominantDueDay(days: List<Int>): Int {
        return days.groupingBy { it }.eachCount()
            .maxWithOrNull(compareBy<Map.Entry<Int, Int>> { it.value }.thenBy { -abs(15 - it.key) })
            ?.key ?: 1
    }

    private fun computeNextDueDate(referenceDate: LocalDate, dayOfMonth: Int): LocalDate {
        val thisMonth = YearMonth.from(referenceDate)
        val thisMonthDue = thisMonth.atDay(dayOfMonth.coerceAtMost(thisMonth.lengthOfMonth()))
        if (thisMonthDue.isAfter(referenceDate)) return thisMonthDue

        val nextMonth = thisMonth.plusMonths(1)
        return nextMonth.atDay(dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth()))
    }

    private fun nextDueFromInterval(lastPaid: LocalDate, intervalDays: Int, referenceDate: LocalDate): LocalDate {
        if (intervalDays <= 0) return referenceDate
        var next = lastPaid.plusDays(intervalDays.toLong())
        var guard = 0
        while (!next.isAfter(referenceDate) && guard++ < 500) {
            next = next.plusDays(intervalDays.toLong())
        }
        return next
    }

    private companion object {
        const val MONTHLY_GAP_MIN = 25
        const val MONTHLY_GAP_MAX = 35
        const val INTERVAL_GAP_MIN = 40
        const val INTERVAL_GAP_MAX = 220
    }
}
