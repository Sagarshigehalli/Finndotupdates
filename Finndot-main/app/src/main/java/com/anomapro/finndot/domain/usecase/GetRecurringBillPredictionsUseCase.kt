package com.anomapro.finndot.domain.usecase

import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.domain.analytics.RecurringBillPredictionResult
import com.anomapro.finndot.domain.analytics.RecurringBillPredictor
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/**
 * Recurring outflow prediction: [RecurringBillPredictionResult.monthly] (strict monthly pattern)
 * and [RecurringBillPredictionResult.interval] (longer fixed gaps, e.g. quarterly, from 2+ payments).
 *
 * Loads ~18 months of history so quarterly / longer gaps still have enough pairs; the predictor
 * keeps monthly rules on the last 6 months only.
 */
class GetRecurringBillPredictionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val recurringBillPredictor: RecurringBillPredictor,
) {
    suspend operator fun invoke(referenceDate: LocalDate = LocalDate.now()): RecurringBillPredictionResult {
        val startDate = referenceDate.minusMonths(17).withDayOfMonth(1).atStartOfDay()
        val endDate = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth()).atTime(23, 59, 59)
        val transactions = transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
        return recurringBillPredictor.predictAll(transactions, referenceDate)
    }
}
