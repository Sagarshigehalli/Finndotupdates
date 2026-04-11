package com.anomapro.finndot.presentation.common

import java.time.LocalDate
import java.time.YearMonth

enum class TimePeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    LAST_3_MONTHS("Last 3 Months"),
    LAST_6_MONTHS("Last 6 Months"),
    LAST_12_MONTHS("Last 12 Months"),
    CURRENT_FY("Current FY"),
    ALL("All Time")
}

enum class TransactionTypeFilter(val label: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense"),
    SPEND("Spent"),
    CREDIT("Credit"),
    TRANSFER("Transfer"),
    INVESTMENT("Investment")
}

fun getDateRangeForPeriod(period: TimePeriod): Pair<LocalDate, LocalDate> {
    val today = LocalDate.now()
    return when (period) {
        TimePeriod.THIS_MONTH -> {
            val start = YearMonth.now().atDay(1)
            start to today
        }
        TimePeriod.LAST_MONTH -> {
            val lastMonth = YearMonth.now().minusMonths(1)
            val start = lastMonth.atDay(1)
            val end = lastMonth.atEndOfMonth()
            start to end
        }
        TimePeriod.LAST_3_MONTHS -> {
            val start = today.minusMonths(3)
            start to today
        }
        TimePeriod.LAST_6_MONTHS -> {
            val start = today.minusMonths(6)
            start to today
        }
        TimePeriod.LAST_12_MONTHS -> {
            val start = today.minusMonths(12)
            start to today
        }
        TimePeriod.CURRENT_FY -> {
            // Indian Financial Year: April 1 to March 31
            val currentYear = today.year
            val currentMonth = today.monthValue
            val fyStart = if (currentMonth >= 4) {
                LocalDate.of(currentYear, 4, 1)  // Apr 1 of current year
            } else {
                LocalDate.of(currentYear - 1, 4, 1)  // Apr 1 of previous year
            }
            fyStart to today
        }
        TimePeriod.ALL -> {
            // Use a reasonable date range for "All Time" - 10 years back to today
            val start = today.minusYears(10)
            start to today
        }
    }
}