package com.anomapro.finndot.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.anomapro.finndot.MainActivity
import com.anomapro.finndot.R
import com.anomapro.finndot.data.database.FinndotDatabase
import com.anomapro.finndot.domain.analytics.SpendingAnalyticsFilter
import com.anomapro.finndot.data.manager.DailySummaryNotificationManager
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import com.anomapro.finndot.utils.CurrencyFormatter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.time.LocalDate

@HiltWorker
class DailySummaryNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val dailySummaryNotificationManager: DailySummaryNotificationManager
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val CHANNEL_ID = "daily_summary_channel"
        private const val CHANNEL_NAME = "Daily Summary"
        private const val NOTIFICATION_ID = 1001
    }
    
    override suspend fun doWork(): Result {
        return try {
            // Check if notifications are enabled
            val isEnabled = userPreferencesRepository.dailySummaryNotificationEnabled.first()
            if (!isEnabled) {
                return Result.success() // Not enabled, but work completed successfully
            }
            
            // Get today's transactions
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay()
            val endOfDay = today.atTime(23, 59, 59)
            
            val database = FinndotDatabase.getInstance(applicationContext)
            val transactionDao = database.transactionDao()
            val transactions = transactionDao.getTransactionsBetweenDatesList(startOfDay, endOfDay)
            
            // Get base currency (default to INR)
            val baseCurrency = userPreferencesRepository.baseCurrency.first()
            
            // Filter by base currency
            val currencyTransactions = transactions.filter { it.currency == baseCurrency }
            
            // Calculate summary
            val earnings = currencyTransactions
                .filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) }
                .sumOf { it.amount.abs() }
            
            val spending = currencyTransactions
                .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                .sumOf { it.amount.abs() }
            
            val regretSpending = currencyTransactions
                .filter { it.isRegret && SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                .sumOf { it.amount.abs() }
            
            val netAmount = earnings - spending
            
            // Create notification
            showNotification(earnings, spending, regretSpending, netAmount, baseCurrency)
            
            // Schedule next day's alarm at the configured time
            dailySummaryNotificationManager.scheduleDailyNotification()
            
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
    
    private fun showNotification(
        earnings: BigDecimal,
        spending: BigDecimal,
        regretSpending: BigDecimal,
        netAmount: BigDecimal,
        currency: String
    ) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily financial summary notifications"
        }
        notificationManager.createNotificationChannel(channel)
        
        // Build notification message
        val isPositive = netAmount >= BigDecimal.ZERO
        val emoji = if (isPositive) "📈" else "📉"
        val title = "$emoji Today's Summary"
        
        val message = buildString {
            append("Earnings: ${CurrencyFormatter.formatCurrency(earnings, currency)}\n")
            append("Spending: ${CurrencyFormatter.formatCurrency(spending, currency)}\n")
            if (regretSpending > BigDecimal.ZERO) {
                append("Regret: ${CurrencyFormatter.formatCurrency(regretSpending, currency)}\n")
            }
            append("Net: ${CurrencyFormatter.formatCurrency(netAmount.abs(), currency)} ${if (isPositive) "saved" else "spent"}")
        }
        
        // Motivational message
        val motivationalMessage = when {
            regretSpending > BigDecimal.ZERO && regretSpending == spending -> {
                "All spending was marked as regret. Reflect and plan better tomorrow! 💭"
            }
            regretSpending > BigDecimal.ZERO -> {
                "You marked ${CurrencyFormatter.formatCurrency(regretSpending, currency)} as regret. Learn and grow! 📈"
            }
            spending > BigDecimal.ZERO && earnings > BigDecimal.ZERO -> {
                val savingsRate = ((netAmount / earnings) * BigDecimal(100)).toFloat()
                when {
                    savingsRate >= 50f -> "Excellent! You saved ${String.format("%.0f", savingsRate)}% today! 🎉"
                    savingsRate >= 30f -> "Great job! You saved ${String.format("%.0f", savingsRate)}% today! 💪"
                    savingsRate >= 0f -> "Good! You saved ${String.format("%.0f", savingsRate)}% today! 👍"
                    else -> "Spent more than earned. Try to balance tomorrow! ⚖️"
                }
            }
            earnings > BigDecimal.ZERO -> {
                "No spending today! Perfect discipline! 🌟"
            }
            spending > BigDecimal.ZERO -> {
                "Tracked your spending. Awareness is the first step! 👁️"
            }
            else -> {
                "Start your financial journey today! 💰"
            }
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val launchPendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$message\n\n$motivationalMessage"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(launchPendingIntent)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}

