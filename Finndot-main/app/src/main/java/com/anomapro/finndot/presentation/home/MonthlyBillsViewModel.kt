package com.anomapro.finndot.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.domain.analytics.RecurringBillCadence
import com.anomapro.finndot.domain.analytics.RecurringBillPrediction
import com.anomapro.finndot.domain.analytics.RecurringBillType
import com.anomapro.finndot.domain.analytics.SpendingAnalyticsFilter
import com.anomapro.finndot.domain.usecase.GetRecurringBillPredictionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class MonthlyBillUiItem(
    val merchant: String,
    val dueDate: LocalDate,
    val expectedAmount: BigDecimal,
    val type: RecurringBillType,
    val currency: String,
    val cadence: RecurringBillCadence = RecurringBillCadence.MONTHLY,
    val medianIntervalDays: Int? = null,
)

data class MonthlyBillsUiState(
    val isLoading: Boolean = true,
    val loadFailed: Boolean = false,
    /** True when the predictor found at least one recurring bill in the selected currency (any due month). */
    val hasRecurringBillsForCurrency: Boolean = false,
    val currency: String = "INR",
    val totalThisMonth: BigDecimal = BigDecimal.ZERO,
    val fixedAmountThisMonth: BigDecimal = BigDecimal.ZERO,
    val variableAmountThisMonth: BigDecimal = BigDecimal.ZERO,
    val paidCountThisMonth: Int = 0,
    val totalCountThisMonth: Int = 0,
    val dueThisWeek: List<MonthlyBillUiItem> = emptyList(),
    val dueNext: List<MonthlyBillUiItem> = emptyList(),
    val dueLater: List<MonthlyBillUiItem> = emptyList(),
    /** Longer-cycle recurring (e.g. quarterly); not included in "this month" totals above. */
    val intervalRecurringItems: List<MonthlyBillUiItem> = emptyList(),
    val paymentHistoryByMonth: List<MonthlyBillHistoryGroup> = emptyList(),
)

data class MonthlyBillHistoryItem(
    val merchant: String,
    val paidDate: LocalDate,
    val amount: BigDecimal,
    val currency: String,
)

data class MonthlyBillHistoryGroup(
    val month: YearMonth,
    val items: List<MonthlyBillHistoryItem>,
)

@HiltViewModel
class MonthlyBillsViewModel @Inject constructor(
    private val getRecurringBillPredictionsUseCase: GetRecurringBillPredictionsUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MonthlyBillsUiState())
    val uiState: StateFlow<MonthlyBillsUiState> = _uiState.asStateFlow()

    fun load(selectedCurrency: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                loadFailed = false,
                currency = selectedCurrency,
            )
            try {
                val today = LocalDate.now()
                val thisMonth = YearMonth.from(today)
                val predictionResult = getRecurringBillPredictionsUseCase(today)
                val monthlyPreds = predictionResult.monthly
                    .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
                val intervalPreds = predictionResult.interval
                    .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
                val predictionsForHistory = monthlyPreds + intervalPreds

                val monthPredictions = monthlyPreds
                    .filter { YearMonth.from(it.nextDueDate) == thisMonth }
                    .sortedBy { it.nextDueDate }

                val total = monthPredictions.sumOf { it.expectedAmount }
                val fixed = monthPredictions
                    .filter { it.type == RecurringBillType.FIXED }
                    .sumOf { it.expectedAmount }
                val variable = monthPredictions
                    .filter { it.type == RecurringBillType.VARIABLE }
                    .sumOf { it.expectedAmount }

                val startOfMonth = today.withDayOfMonth(1).atStartOfDay()
                val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)
                val monthTx = transactionRepository.getTransactionsBetweenDates(startOfMonth, endOfMonth).first()
                    .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                val paidMerchants = monthTx.map { (it.normalizedMerchantName ?: it.merchantName).trim().lowercase() }.toSet()
                val paidCount = monthPredictions.count { paidMerchants.contains(it.merchant.trim().lowercase()) }

                val items = monthPredictions.map { it.toUi() }
                val weekEnd = today.plusDays(7)
                val nextEnd = weekEnd.plusDays(7)
                val historyGroups = buildHistoryGroups(predictionsForHistory, selectedCurrency, today)
                val intervalItems = intervalPreds.map { it.toUi() }.sortedBy { it.dueDate }

                _uiState.value = MonthlyBillsUiState(
                    isLoading = false,
                    loadFailed = false,
                    hasRecurringBillsForCurrency = predictionsForHistory.isNotEmpty(),
                    currency = selectedCurrency,
                    totalThisMonth = total,
                    fixedAmountThisMonth = fixed,
                    variableAmountThisMonth = variable,
                    paidCountThisMonth = paidCount,
                    totalCountThisMonth = monthPredictions.size,
                    dueThisWeek = items.filter { !it.dueDate.isBefore(today) && !it.dueDate.isAfter(weekEnd) },
                    dueNext = items.filter { it.dueDate.isAfter(weekEnd) && !it.dueDate.isAfter(nextEnd) },
                    dueLater = items.filter { it.dueDate.isAfter(nextEnd) },
                    intervalRecurringItems = intervalItems,
                    paymentHistoryByMonth = historyGroups,
                )
            } catch (e: Exception) {
                Log.e("MonthlyBillsViewModel", "load failed", e)
                _uiState.value = MonthlyBillsUiState(
                    isLoading = false,
                    loadFailed = true,
                    hasRecurringBillsForCurrency = false,
                    currency = selectedCurrency,
                )
            }
        }
    }

    private fun RecurringBillPrediction.toUi() = MonthlyBillUiItem(
        merchant = merchant,
        dueDate = nextDueDate,
        expectedAmount = expectedAmount,
        type = type,
        currency = currency,
        cadence = cadence,
        medianIntervalDays = medianIntervalDays,
    )

    private suspend fun buildHistoryGroups(
        predictions: List<RecurringBillPrediction>,
        selectedCurrency: String,
        today: LocalDate,
    ): List<MonthlyBillHistoryGroup> {
        val historyStart = today.minusMonths(11).withDayOfMonth(1).atStartOfDay()
        val historyEnd = today.atTime(23, 59, 59)
        val trackedMerchants = predictions.map { it.merchant.trim().lowercase() }.toSet()
        if (trackedMerchants.isEmpty()) return emptyList()

        val tx = transactionRepository.getTransactionsBetweenDates(historyStart, historyEnd).first()
            .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
            .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
            .filter { trackedMerchants.contains((it.normalizedMerchantName ?: it.merchantName).trim().lowercase()) }

        return tx
            .groupBy { YearMonth.from(it.dateTime) }
            .toList()
            .sortedByDescending { it.first }
            .map { (month, monthTx) ->
                MonthlyBillHistoryGroup(
                    month = month,
                    items = monthTx
                        .sortedByDescending { it.dateTime }
                        .map {
                            MonthlyBillHistoryItem(
                                merchant = (it.normalizedMerchantName ?: it.merchantName).trim(),
                                paidDate = it.dateTime.toLocalDate(),
                                amount = it.amount.abs(),
                                currency = it.currency,
                            )
                        }
                )
            }
    }
}

