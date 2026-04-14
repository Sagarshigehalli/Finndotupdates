package com.anomapro.finndot.presentation.home

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkInfo
import androidx.work.workDataOf
import com.anomapro.finndot.data.database.entity.AccountBalanceEntity
import com.anomapro.finndot.data.database.entity.SubscriptionEntity
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.analytics.UsageStatsService
import com.anomapro.finndot.data.manager.InAppUpdateManager
import com.anomapro.finndot.data.manager.InAppReviewManager
import com.anomapro.finndot.data.currency.CurrencyConversionService
import com.anomapro.finndot.data.repository.AccountBalanceRepository
import com.anomapro.finndot.data.repository.BudgetRepository
import com.anomapro.finndot.data.repository.LlmRepository
import com.anomapro.finndot.data.repository.SubscriptionRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.domain.usecase.GetRecurringBillPredictionsUseCase
import com.anomapro.finndot.worker.OptimizedSmsReaderWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.anomapro.finndot.ui.components.MonthlySpendingData
import com.anomapro.finndot.ui.components.CategorySpendingData
import com.anomapro.finndot.ui.icons.CategoryMapping
import com.anomapro.finndot.domain.analytics.SpendingAnalyticsFilter

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val accountBalanceRepository: AccountBalanceRepository,
    private val budgetRepository: BudgetRepository,
    private val llmRepository: LlmRepository,
    private val currencyConversionService: CurrencyConversionService,
    private val inAppUpdateManager: InAppUpdateManager,
    private val inAppReviewManager: InAppReviewManager,
    private val usageStatsService: UsageStatsService,
    private val getRecurringBillPredictionsUseCase: GetRecurringBillPredictionsUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)
    private fun manualBudgetKey(yearMonth: YearMonth, currency: String): String =
        "manual_budget_${yearMonth}_$currency"

    private fun isManualBudget(yearMonth: YearMonth, currency: String): Boolean =
        sharedPrefs.getBoolean(manualBudgetKey(yearMonth, currency), false)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()

    // SMS scanning work progress tracking
    private val _smsScanWorkInfo = MutableStateFlow<WorkInfo?>(null)
    val smsScanWorkInfo: StateFlow<WorkInfo?> = _smsScanWorkInfo.asStateFlow()

    // Store currency breakdown maps for quick access when switching currencies
    private var currentMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> = emptyMap()
    private var lastMonthBreakdownMap: Map<String, TransactionRepository.MonthlyBreakdown> = emptyMap()
    
    init {
        loadHomeData()
    }
    
    private fun loadHomeData() {
        _uiState.value = _uiState.value.copy(loadingMessage = "Loading accounts...")
        viewModelScope.launch {
            // Load current month breakdown by currency
            transactionRepository.getCurrentMonthBreakdownByCurrency().collect { breakdownByCurrency ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, isCurrentMonth = true)
            }
        }
        
        viewModelScope.launch {
            // Load account balances
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading accounts...")
            accountBalanceRepository.getAllLatestBalances().collect { allBalances ->
                // Get hidden accounts from SharedPreferences
                val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
                
                // Filter out hidden accounts
                val balances = allBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                // Separate credit cards from regular accounts (hide zero balance accounts)
                val regularAccounts = balances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
                val creditCards = balances.filter { it.isCreditCard }
                
                // Account loading completed
                Log.d("HomeViewModel", "Loaded ${balances.size} account(s)")
                
                // Check if we have multiple currencies and refresh exchange rates if needed
                val accountCurrencies = regularAccounts.map { it.currency }.distinct()
                val hasMultipleCurrencies = accountCurrencies.size > 1

                if (hasMultipleCurrencies) {
                    currencyConversionService.refreshExchangeRatesForAccount(accountCurrencies)
                }

                // Convert all account balances to selected currency for total
                val selectedCurrency = _uiState.value.selectedCurrency
                val totalBalanceInSelectedCurrency = regularAccounts.sumOf { account ->
                    if (account.currency == selectedCurrency) {
                        account.balance
                    } else {
                        // Convert to selected currency
                        currencyConversionService.convertAmount(
                            amount = account.balance,
                            fromCurrency = account.currency,
                            toCurrency = selectedCurrency
                        ) ?: account.balance
                    }
                }

                val totalAvailableCreditInSelectedCurrency = creditCards.sumOf { card ->
                    // Available = Credit Limit - Outstanding Balance, converted to selected currency
                    val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                    if (card.currency == selectedCurrency) {
                        availableInCardCurrency
                    } else {
                        currencyConversionService.convertAmount(
                            amount = availableInCardCurrency,
                            fromCurrency = card.currency,
                            toCurrency = selectedCurrency
                        ) ?: availableInCardCurrency
                    }
                }

                _uiState.value = _uiState.value.copy(
                    accountBalances = regularAccounts,  // Only regular bank accounts
                    creditCards = creditCards,           // Only credit cards
                    totalBalance = totalBalanceInSelectedCurrency,
                    totalAvailableCredit = totalAvailableCreditInSelectedCurrency
                )
            }
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading transactions...")
            // Load current month transactions by type (currency-filtered)
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

            transactionRepository.getTransactionsBetweenDates(
                startDate = startOfMonth,
                endDate = endOfMonth
            ).collect { transactions ->
                updateTransactionTypeTotals(transactions)
            }
        }
        
        viewModelScope.launch {
            // Load last month breakdown by currency
            transactionRepository.getLastMonthBreakdownByCurrency().collect { breakdownByCurrency ->
                updateBreakdownForSelectedCurrency(breakdownByCurrency, isCurrentMonth = false)
            }
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading recent transactions...")
            // Load recent transactions (last 3)
            transactionRepository.getRecentTransactions(limit = 3).collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    recentTransactions = transactions,
                    isLoading = false,
                    loadingMessage = null
                )
            }
        }
        
        viewModelScope.launch {
            // Load daily summary
            loadDailySummary()
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading subscriptions...")
            // Load all active subscriptions
            subscriptionRepository.getActiveSubscriptions().collect { subscriptions ->
                val totalAmount = subscriptions.sumOf { it.amount }
                _uiState.value = _uiState.value.copy(
                    upcomingSubscriptions = subscriptions,
                    upcomingSubscriptionsTotal = totalAmount
                )
            }
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading insights...")
            // Load aggregated investment and score data for last 12 months
            loadInvestmentAndScoreData()
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading trends...")
            // Load income vs spending trend for last 12 months
            loadSpendingTrendData()
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading categories...")
            // Load category distribution data
            loadCategoryDistributionData()
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading budget...")
            // Load budget data
            loadBudgetData()
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(loadingMessage = "Loading visuals...")
            loadHomeVisualsData()
        }

        viewModelScope.launch {
            refreshRecurringMonthlyCommitments()
        }
    }

    private suspend fun refreshRecurringMonthlyCommitments() {
        try {
            val today = LocalDate.now()
            val selectedCurrency = _uiState.value.selectedCurrency
            val result = getRecurringBillPredictionsUseCase(today)
            val monthlyPreds = result.monthly
                .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
            val intervalPreds = result.interval
                .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }

            val thisMonth = YearMonth.from(today)
            val thisMonthPredictions = monthlyPreds.filter { YearMonth.from(it.nextDueDate) == thisMonth }
            val totalPredicted = thisMonthPredictions.sumOf { it.expectedAmount }
            val nextDue = monthlyPreds
                .filter { it.daysUntilDue >= 0 }
                .minByOrNull { it.daysUntilDue }
                ?: monthlyPreds.minByOrNull { it.daysUntilDue }
            val nextInterval = intervalPreds
                .filter { it.daysUntilDue >= 0 }
                .minByOrNull { it.daysUntilDue }
                ?: intervalPreds.minByOrNull { it.daysUntilDue }

            val startOfMonth = today.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(23, 59, 59)
            val monthTx = transactionRepository.getTransactionsBetweenDates(startOfMonth, endOfMonth).first()
                .filter { it.currency.equals(selectedCurrency, ignoreCase = true) }
                .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
            val paidByMerchant = monthTx
                .groupBy { (it.normalizedMerchantName ?: it.merchantName).trim().lowercase() }
                .mapValues { (_, txs) -> txs.sumOf { it.amount.abs() } }
            val paidPredicted = thisMonthPredictions.sumOf { prediction ->
                paidByMerchant[prediction.merchant.trim().lowercase()]?.let { paid ->
                    minOf(paid, prediction.expectedAmount)
                } ?: BigDecimal.ZERO
            }
            val progress = if (totalPredicted > BigDecimal.ZERO) {
                (paidPredicted / totalPredicted).toFloat().coerceIn(0f, 1f)
            } else {
                0f
            }

            _uiState.value = _uiState.value.copy(
                recurringBillsPredictedTotalThisMonth = totalPredicted,
                recurringBillsExpectedCountThisMonth = thisMonthPredictions.size,
                recurringBillsPaidAmountThisMonth = paidPredicted,
                recurringBillsProgressThisMonth = progress,
                recurringBillsNextMerchant = nextDue?.merchant,
                recurringBillsNextAmount = nextDue?.expectedAmount,
                recurringBillsNextDaysUntilDue = nextDue?.daysUntilDue,
                intervalRecurringCount = intervalPreds.size,
                intervalRecurringNextMerchant = nextInterval?.merchant,
                intervalRecurringNextAmount = nextInterval?.expectedAmount,
                intervalRecurringNextDaysUntilDue = nextInterval?.daysUntilDue,
            )
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "refreshRecurringMonthlyCommitments failed", e)
            _uiState.value = _uiState.value.copy(
                recurringBillsPredictedTotalThisMonth = BigDecimal.ZERO,
                recurringBillsExpectedCountThisMonth = 0,
                recurringBillsPaidAmountThisMonth = BigDecimal.ZERO,
                recurringBillsProgressThisMonth = 0f,
                recurringBillsNextMerchant = null,
                recurringBillsNextAmount = null,
                recurringBillsNextDaysUntilDue = null,
                intervalRecurringCount = 0,
                intervalRecurringNextMerchant = null,
                intervalRecurringNextAmount = null,
                intervalRecurringNextDaysUntilDue = null,
            )
        }
    }
    
    private fun loadHomeVisualsData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val selectedCurrency = _uiState.value.selectedCurrency
            val startOfMonth = now.withDayOfMonth(1).atStartOfDay()
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(23, 59, 59)
            val sevenDaysAgo = now.minusDays(6).atStartOfDay()
            
            transactionRepository.getTransactionsBetweenDates(sevenDaysAgo, endOfMonth).collect { transactions ->
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                val expenseAndCredit = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                
                val last7Days = (0..6).map { dayOffset ->
                    val day = now.minusDays((6 - dayOffset).toLong())
                    val dayStart = day.atStartOfDay()
                    val dayEnd = day.atTime(23, 59, 59)
                    expenseAndCredit
                        .filter { it.dateTime >= dayStart && it.dateTime <= dayEnd }
                        .sumOf { it.amount.abs() }
                }
                
                val currentMonthOnly = currencyTransactions.filter {
                    it.dateTime >= startOfMonth && it.dateTime <= endOfMonth
                }
                val monthExpenseCredit = currentMonthOnly
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                val monthlyRegret = monthExpenseCredit
                    .filter { it.isRegret }
                    .sumOf { it.amount.abs() }
                
                val dayOfMonth = now.dayOfMonth
                val lengthOfMonth = now.lengthOfMonth()
                val currentSpend = monthExpenseCredit.sumOf { it.amount.abs() }
                val projected = if (dayOfMonth > 0 && currentSpend > BigDecimal.ZERO) {
                    (currentSpend / BigDecimal(dayOfMonth)) * BigDecimal(lengthOfMonth)
                } else BigDecimal.ZERO
                
                _uiState.value = _uiState.value.copy(
                    last7DaysDailySpend = last7Days,
                    monthlyRegretAmount = monthlyRegret,
                    projectedMonthEndSpend = projected,
                    daysLeftInMonth = (lengthOfMonth - dayOfMonth).coerceAtLeast(0),
                    monthProgressPercent = (dayOfMonth.toFloat() / lengthOfMonth) * 100f
                )
            }
        }
    }
    
    private fun loadBudgetData() {
        viewModelScope.launch {
            try {
                val now = LocalDate.now()
                val currentMonth = YearMonth.from(now)
                
                // Observe both currency changes and budget changes
                @OptIn(ExperimentalCoroutinesApi::class)
                combine(
                    _uiState.map { it.selectedCurrency }.distinctUntilChanged(),
                    _uiState.map { it.selectedCurrency }
                        .distinctUntilChanged()
                        .flatMapLatest { currency ->
                            budgetRepository.getBudgetsForMonth(currentMonth, currency)
                                .catch { e ->
                                    Log.e("HomeViewModel", "Error in budget flow", e)
                                    emit(emptyList())
                                }
                        }
                ) { selectedCurrency, budgets ->
                    Pair(selectedCurrency, budgets)
                }.collect { (selectedCurrency, budgets) ->
                    try {
                        val currentMonth = YearMonth.from(now)
                        val hasManualBudget = isManualBudget(currentMonth, selectedCurrency)
                        // Get total budget for current month
                        val totalBudget = budgets.find { it.category == "All" && it.currency == selectedCurrency }
                        
                        // Get actual spending for current month
                        val startDate = now.withDayOfMonth(1).atStartOfDay()
                        val endDate = now.atTime(23, 59, 59)
                        val transactions = try {
                            transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
                        } catch (e: Exception) {
                            Log.e("HomeViewModel", "Error getting transactions", e)
                            emptyList()
                        }
                        
                        val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                        val actualSpending = currencyTransactions
                            .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                            .sumOf { it.amount.abs() }
                        
                        val customBudgetAmount = totalBudget?.amount ?: BigDecimal.ZERO
                        val recommendedBudget = _uiState.value.recommendedMonthlyBudget
                        val effectiveBudgetAmount = if (hasManualBudget && customBudgetAmount > BigDecimal.ZERO) {
                            customBudgetAmount
                        } else {
                            recommendedBudget
                        }
                        val remaining = effectiveBudgetAmount - actualSpending
                        val percentageUsed = if (effectiveBudgetAmount > BigDecimal.ZERO) {
                            (actualSpending / effectiveBudgetAmount * BigDecimal(100)).toFloat().coerceIn(0f, 100f)
                        } else {
                            0f
                        }
                        
                        val now = LocalDate.now()
                        val lengthOfMonth = now.lengthOfMonth()
                        val dailyBudgetAmount = if (lengthOfMonth > 0 && effectiveBudgetAmount > BigDecimal.ZERO) {
                            effectiveBudgetAmount / BigDecimal(lengthOfMonth)
                        } else BigDecimal.ZERO
                        
                        // Calculate budget score (0-100): Higher score = better budget management
                        // Score = 100 - percentageUsed, but with bonus for staying under budget
                        val budgetScore = if (effectiveBudgetAmount > BigDecimal.ZERO) {
                            val baseScore = 100f - percentageUsed
                            val bonus = if (remaining > BigDecimal.ZERO) {
                                // Bonus for having remaining budget (up to 20 points)
                                (remaining / effectiveBudgetAmount * BigDecimal(20)).toFloat().coerceIn(0f, 20f)
                            } else {
                                0f
                            }
                            (baseScore + bonus).coerceIn(0f, 100f)
                        } else {
                            0f
                        }
                        
                        _uiState.value = _uiState.value.copy(
                            monthlyBudget = effectiveBudgetAmount,
                            monthlyActualSpending = actualSpending,
                            monthlyBudgetRemaining = remaining,
                            monthlyBudgetPercentageUsed = percentageUsed,
                            budgetScore = budgetScore,
                            dailyBudgetAmount = dailyBudgetAmount,
                            hasCustomBudget = hasManualBudget && customBudgetAmount > BigDecimal.ZERO
                        )
                    } catch (e: Exception) {
                        Log.e("HomeViewModel", "Error loading budget data", e)
                        // Set default values on error
                        _uiState.value = _uiState.value.copy(
                            monthlyBudget = BigDecimal.ZERO,
                            monthlyActualSpending = BigDecimal.ZERO,
                            monthlyBudgetRemaining = BigDecimal.ZERO,
                            monthlyBudgetPercentageUsed = 0f,
                            budgetScore = 0f,
                            dailyBudgetAmount = BigDecimal.ZERO
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error initializing budget data loader", e)
                // Set default values on initialization error
                _uiState.value = _uiState.value.copy(
                    monthlyBudget = BigDecimal.ZERO,
                    monthlyActualSpending = BigDecimal.ZERO,
                    monthlyBudgetRemaining = BigDecimal.ZERO,
                    monthlyBudgetPercentageUsed = 0f,
                    budgetScore = 0f,
                    dailyBudgetAmount = BigDecimal.ZERO,
                    hasCustomBudget = false
                )
            }
        }
    }
    
    fun refreshBudgetData() {
        viewModelScope.launch {
            // Force refresh by reloading budget data
            val now = LocalDate.now()
            val currentMonth = YearMonth.from(now)
            val selectedCurrency = _uiState.value.selectedCurrency
            val hasManualBudget = isManualBudget(currentMonth, selectedCurrency)
            
            try {
                // Get total budget for current month
                val totalBudget = budgetRepository.getTotalBudgetForMonth(currentMonth, selectedCurrency)
                
                // Get actual spending for current month
                val startDate = now.withDayOfMonth(1).atStartOfDay()
                val endDate = now.atTime(23, 59, 59)
                val transactions = transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                val actualSpending = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                    .sumOf { it.amount.abs() }
                
                val customBudgetAmount = totalBudget?.amount ?: BigDecimal.ZERO
                val recommendedBudget = _uiState.value.recommendedMonthlyBudget
                val effectiveBudgetAmount = if (hasManualBudget && customBudgetAmount > BigDecimal.ZERO) {
                    customBudgetAmount
                } else {
                    recommendedBudget
                }
                val remaining = effectiveBudgetAmount - actualSpending
                val percentageUsed = if (effectiveBudgetAmount > BigDecimal.ZERO) {
                    (actualSpending / effectiveBudgetAmount * BigDecimal(100)).toFloat().coerceIn(0f, 100f)
                } else {
                    0f
                }
                
                val lengthOfMonth = now.lengthOfMonth()
                val dailyBudgetAmount = if (lengthOfMonth > 0 && effectiveBudgetAmount > BigDecimal.ZERO) {
                    effectiveBudgetAmount / BigDecimal(lengthOfMonth)
                } else BigDecimal.ZERO
                
                // Calculate budget score
                val budgetScore = if (effectiveBudgetAmount > BigDecimal.ZERO) {
                    val baseScore = 100f - percentageUsed
                    val bonus = if (remaining > BigDecimal.ZERO) {
                        (remaining / effectiveBudgetAmount * BigDecimal(20)).toFloat().coerceIn(0f, 20f)
                    } else {
                        0f
                    }
                    (baseScore + bonus).coerceIn(0f, 100f)
                } else {
                    0f
                }
                
                _uiState.value = _uiState.value.copy(
                    monthlyBudget = effectiveBudgetAmount,
                    monthlyActualSpending = actualSpending,
                    monthlyBudgetRemaining = remaining,
                    monthlyBudgetPercentageUsed = percentageUsed,
                    budgetScore = budgetScore,
                    dailyBudgetAmount = dailyBudgetAmount,
                    hasCustomBudget = hasManualBudget && customBudgetAmount > BigDecimal.ZERO
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error refreshing budget data", e)
            }
        }
    }
    
    private fun loadSpendingTrendData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val months = _uiState.value.selectedTimeRange.months
            val startMonth = now.minusMonths((months - 1).toLong())
            val startDate = startMonth.withDayOfMonth(1).atStartOfDay()
            val endDate = now.atTime(23, 59, 59)
            
            transactionRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                val selectedCurrency = _uiState.value.selectedCurrency
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                
                // Group by month and calculate expense and income totals
                val monthlyData = currencyTransactions
                    .groupBy { YearMonth.from(it.dateTime) }
                    .map { (month, monthTransactions) ->
                        val expenses = monthTransactions
                            .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                            .sumOf { it.amount.abs() }
                        val income = monthTransactions
                            .filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) }
                            .sumOf { it.amount.abs() }
                        val regretAmount = monthTransactions
                            .filter { it.isRegret && SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                            .sumOf { it.amount.abs() }
                        MonthlySpendingData(
                            month = month,
                            amount = expenses,
                            income = income,
                            regretAmount = regretAmount,
                            currency = selectedCurrency
                        )
                    }
                    .sortedBy { it.month }
                
                // Fill in missing months with zero
                val allMonths = mutableListOf<MonthlySpendingData>()
                var currentMonth = YearMonth.from(startMonth)
                val endMonth = YearMonth.from(now)
                
                while (currentMonth <= endMonth) {
                    val existing = monthlyData.find { it.month == currentMonth }
                    allMonths.add(
                        existing ?: MonthlySpendingData(
                            month = currentMonth,
                            amount = BigDecimal.ZERO,
                            income = BigDecimal.ZERO,
                            regretAmount = BigDecimal.ZERO,
                            currency = selectedCurrency
                        )
                    )
                    currentMonth = currentMonth.plusMonths(1)
                }
                
                _uiState.value = _uiState.value.copy(
                    monthlySpendingData = allMonths
                )
            }
        }
    }
    
    private fun loadCategoryDistributionData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val months = _uiState.value.selectedTimeRange.months
            val startMonth = now.minusMonths((months - 1).toLong())
            val startDate = startMonth.withDayOfMonth(1).atStartOfDay()
            val endDate = now.atTime(23, 59, 59)
            
            transactionRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                val selectedCurrency = _uiState.value.selectedCurrency
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                
                val expenses = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                
                val totalExpenses = expenses.sumOf { it.amount.abs() }
                
                if (totalExpenses > BigDecimal.ZERO) {
                    val categoryData = expenses
                        .groupBy { it.category }
                        .map { (category, categoryTransactions) ->
                            val amount = categoryTransactions.sumOf { it.amount.abs() }
                            val percentage = (amount / totalExpenses * BigDecimal(100)).toFloat()
                            val categoryInfo = CategoryMapping.categories[category] ?: CategoryMapping.categories["Others"]!!
                            
                            CategorySpendingData(
                                category = category,
                                amount = amount,
                                percentage = percentage,
                                color = categoryInfo.color
                            )
                        }
                        .sortedByDescending { it.amount }
                    
                    _uiState.value = _uiState.value.copy(
                        categoryDistributionData = categoryData
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        categoryDistributionData = emptyList()
                    )
                }
            }
        }
    }
    
    private fun loadInvestmentAndScoreData() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val months = _uiState.value.selectedTimeRange.months
            val startMonth = now.minusMonths((months - 1).toLong())
            val startDate = startMonth.withDayOfMonth(1).atStartOfDay()
            val endDate = now.atTime(23, 59, 59)
            
            transactionRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                val selectedCurrency = _uiState.value.selectedCurrency
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                
                // Calculate aggregate investment (sum of all investments in 12 months)
                val totalInvestment = currencyTransactions
                    .filter { it.transactionType == TransactionType.INVESTMENT }
                    .sumOf { it.amount.abs() }
                
                // Calculate total transaction amount (sum of all transactions in 12 months)
                val totalTransactionAmount = currencyTransactions
                    .sumOf { it.amount.abs() }
                
                // Calculate Finn dot score: (investment / total transaction amount) * 100 + 70, capped at 100
                val finnDotScore = if (totalTransactionAmount > BigDecimal.ZERO) {
                    val baseScore = (totalInvestment / totalTransactionAmount).toFloat().coerceIn(0f, 1f) * 100f
                    (baseScore + 70f).coerceIn(0f, 100f) / 100f // Convert back to 0-1 range for display
                } else {
                    0.7f // 70% when no transactions
                }
                
                // Calculate total income and spending for the selected period
                val totalIncome = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) }
                    .sumOf { it.amount.abs() }
                
                val totalSpend = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                    .sumOf { it.amount.abs() }

                val divisor = BigDecimal(months).coerceAtLeast(BigDecimal.ONE)
                val averageIncome = totalIncome / divisor
                val averageSpend = totalSpend / divisor

                // Recommended budget: (last 3 months income / 3) * 85%
                // Use already-loaded currencyTransactions (covers selected range) - filter to last 3 months
                val threeMonthsAgoStart = now.minusMonths(2).withDayOfMonth(1).atStartOfDay()
                val incomeLast3Months = currencyTransactions
                    .filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) && it.dateTime >= threeMonthsAgoStart }
                    .sumOf { it.amount.abs() }
                val recommendedBudget = if (incomeLast3Months > BigDecimal.ZERO) {
                    (incomeLast3Months / BigDecimal(3)).multiply(BigDecimal("0.85"))
                } else {
                    BigDecimal(30000) // Default when no income data (display only, not persisted)
                }
                
                _uiState.value = _uiState.value.copy(
                    totalInvestment12Months = totalInvestment,
                    finnDotScore = finnDotScore,
                    averageIncome12Months = averageIncome,
                    incomeLast12Months = totalIncome,
                    spendLast12Months = totalSpend,
                    recommendedMonthlyBudget = recommendedBudget
                )
                // Only persist when we have actual income - never persist default before SMS/transactions load
                applyRecommendedBudgetIfNeeded(hasIncomeData = incomeLast3Months > BigDecimal.ZERO)
            }
        }
    }

    private fun applyRecommendedBudgetIfNeeded(hasIncomeData: Boolean = false) {
        val current = _uiState.value
        if (current.hasCustomBudget || current.recommendedMonthlyBudget <= BigDecimal.ZERO) return
        // Never persist default 30k - only persist when we have actual income from transactions
        if (!hasIncomeData) return

        // Auto-persist budget (from actual income) and distribute across categories
        viewModelScope.launch {
            try {
                val now = LocalDate.now()
                val currentMonth = YearMonth.from(now)
                val hasManualBudget = isManualBudget(currentMonth, current.selectedCurrency)
                if (hasManualBudget) return@launch
                val existing = budgetRepository.getTotalBudgetForMonth(currentMonth, current.selectedCurrency)
                val totalBudget = current.recommendedMonthlyBudget
                val defaultCategoryAllocations = listOf(
                    "Food & Dining" to BigDecimal("0.35"),
                    "Bills & Utilities" to BigDecimal("0.25"),
                    "Transportation" to BigDecimal("0.20"),
                    "Shopping" to BigDecimal("0.20")
                )
                if (existing == null || existing.amount.compareTo(totalBudget) != 0) {
                    budgetRepository.createOrUpdateBudget(
                        category = "All",
                        amount = totalBudget,
                        yearMonth = currentMonth,
                        currency = current.selectedCurrency
                    )
                }
                val existingMonthBudgets = budgetRepository
                    .getBudgetsForMonth(currentMonth, current.selectedCurrency)
                    .first()
                    .filter { it.category != "All" }

                val shouldApplyDefaultCategorySplit = existingMonthBudgets.isEmpty() ||
                    existingMonthBudgets.all { budget ->
                        defaultCategoryAllocations.any { it.first == budget.category }
                    }

                if (shouldApplyDefaultCategorySplit && totalBudget > BigDecimal.ZERO) {
                    defaultCategoryAllocations.forEach { (category, ratio) ->
                        val categoryBudget = totalBudget
                            .multiply(ratio)
                            .setScale(0, java.math.RoundingMode.HALF_UP)
                        if (categoryBudget > BigDecimal.ZERO) {
                            budgetRepository.createOrUpdateBudget(
                                category = category,
                                amount = categoryBudget,
                                yearMonth = currentMonth,
                                currency = current.selectedCurrency
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error persisting default budget", e)
            }
        }

        val now = LocalDate.now()
        val lengthOfMonth = now.lengthOfMonth()
        val effectiveBudgetAmount = current.recommendedMonthlyBudget
        val remaining = effectiveBudgetAmount - current.monthlyActualSpending
        val percentageUsed = if (effectiveBudgetAmount > BigDecimal.ZERO) {
            (current.monthlyActualSpending / effectiveBudgetAmount * BigDecimal(100)).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
        val dailyBudgetAmount = if (lengthOfMonth > 0) {
            effectiveBudgetAmount / BigDecimal(lengthOfMonth)
        } else {
            BigDecimal.ZERO
        }
        val budgetScore = if (effectiveBudgetAmount > BigDecimal.ZERO) {
            val baseScore = 100f - percentageUsed
            val bonus = if (remaining > BigDecimal.ZERO) {
                (remaining / effectiveBudgetAmount * BigDecimal(20)).toFloat().coerceIn(0f, 20f)
            } else {
                0f
            }
            (baseScore + bonus).coerceIn(0f, 100f)
        } else {
            0f
        }

        _uiState.value = current.copy(
            monthlyBudget = effectiveBudgetAmount,
            monthlyBudgetRemaining = remaining,
            monthlyBudgetPercentageUsed = percentageUsed,
            budgetScore = budgetScore,
            dailyBudgetAmount = dailyBudgetAmount
        )
    }
    
    private fun calculateMonthlyChange() {
        val currentExpenses = _uiState.value.currentMonthExpenses
        val lastExpenses = _uiState.value.lastMonthExpenses
        val currentTotal = _uiState.value.currentMonthTotal
        val lastTotal = _uiState.value.lastMonthTotal
        
        // Calculate expense change for simple comparison
        val expenseChange = currentExpenses - lastExpenses
        val totalChange = currentTotal - lastTotal
        
        _uiState.value = _uiState.value.copy(
            monthlyChange = totalChange,
            monthlyChangePercent = 0 // We're not using percentage anymore
        )
    }
    
    fun refreshHiddenAccounts() {
        viewModelScope.launch {
            // Force re-read of hidden accounts from SharedPreferences
            val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()
            
            // Re-fetch all accounts and filter
            accountBalanceRepository.getAllLatestBalances().first().let { allBalances ->
                val visibleBalances = allBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                
                // Separate credit cards from regular accounts (hide zero balance accounts)
                val regularAccounts = visibleBalances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
                val creditCards = visibleBalances.filter { it.isCreditCard }
                
                // Update UI state
                _uiState.value = _uiState.value.copy(
                    accountBalances = regularAccounts,
                    creditCards = creditCards,
                    totalBalance = regularAccounts.sumOf { it.balance },
                    totalAvailableCredit = creditCards.sumOf { 
                        // Available = Credit Limit - Outstanding Balance
                        (it.creditLimit ?: BigDecimal.ZERO) - it.balance
                    }
                )
            }
        }
    }
    
    fun scanSmsMessages() {
        usageStatsService.recordEvent("sms_scan")
        val workRequest = OneTimeWorkRequestBuilder<OptimizedSmsReaderWorker>()
            .addTag(OptimizedSmsReaderWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            OptimizedSmsReaderWorker.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        // Update UI to show scanning
        _uiState.value = _uiState.value.copy(isScanning = true)

        // Track work progress
        observeWorkProgress()
    }

    private fun observeWorkProgress() {
        val workManager = WorkManager.getInstance(context)

        workManager.getWorkInfosByTagLiveData(OptimizedSmsReaderWorker.WORK_NAME).observeForever { workInfos ->
            val taggedWork = workInfos.filter { it.tags.contains(OptimizedSmsReaderWorker.WORK_NAME) }
            // Prefer RUNNING/ENQUEUED work; with REPLACE policy, cancelled work may appear first
            val currentWork = taggedWork
                .filter { it.state in listOf(WorkInfo.State.RUNNING, WorkInfo.State.ENQUEUED) }
                .maxByOrNull { it.id }
                ?: taggedWork.maxByOrNull { it.id }
            if (currentWork != null) {
                _smsScanWorkInfo.value = currentWork

                // Update scanning state based on work state
                when (currentWork.state) {
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED,
                    WorkInfo.State.BLOCKED -> {
                        _uiState.value = _uiState.value.copy(isScanning = false)
                        if (currentWork.state == WorkInfo.State.SUCCEEDED) {
                            refreshHomeData()
                        }
                    }
                    else -> {
                        // Still running or enqueued
                        _uiState.value = _uiState.value.copy(isScanning = true)
                    }
                }
            }
        }
    }

    fun cancelSmsScan() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(OptimizedSmsReaderWorker.WORK_NAME)
        _uiState.value = _uiState.value.copy(isScanning = false)
    }

    /**
     * Refreshes all home data (balances, budget, visuals, recent transactions).
     * Used for pull-to-refresh and after SMS scan completes.
     */
    fun refreshHomeData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            refreshAccountBalances()
            refreshBudgetData()
            loadSpendingTrendData()
            loadHomeVisualsData()
            refreshRecurringMonthlyCommitments()
            val recent = transactionRepository.getRecentTransactions(limit = 3).first()
            _uiState.value = _uiState.value.copy(recentTransactions = recent)
            loadDailySummary()
            kotlinx.coroutines.delay(500)
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    fun refreshAccountBalances() {
        viewModelScope.launch {
            // Force refresh the account balances by retriggering the calculation
            accountBalanceRepository.getAllLatestBalances().collect { allBalances ->
                // Get hidden accounts from SharedPreferences
                val hiddenAccounts = sharedPrefs.getStringSet("hidden_accounts", emptySet()) ?: emptySet()

                // Filter out hidden accounts
                val balances = allBalances.filter { account ->
                    val key = "${account.bankName}_${account.accountLast4}"
                    !hiddenAccounts.contains(key)
                }
                // Separate credit cards from regular accounts (hide zero balance accounts)
                val regularAccounts = balances.filter { !it.isCreditCard && it.balance != BigDecimal.ZERO }
                val creditCards = balances.filter { it.isCreditCard }

                // Account loading completed
                Log.d("HomeViewModel", "Refreshed ${balances.size} account(s)")

                // Check if we have multiple currencies and refresh exchange rates if needed
                val accountCurrencies = regularAccounts.map { it.currency }.distinct()
                val creditCardCurrencies = creditCards.map { it.currency }.distinct()
                val allAccountCurrencies = (accountCurrencies + creditCardCurrencies).distinct()
                val hasMultipleCurrencies = allAccountCurrencies.size > 1

                if (hasMultipleCurrencies) {
                    currencyConversionService.refreshExchangeRatesForAccount(allAccountCurrencies)
                }

                // Update available currencies to include account currencies
                val currentAvailableCurrencies = _uiState.value.availableCurrencies.toSet()
                val updatedAvailableCurrencies = (currentAvailableCurrencies + allAccountCurrencies)
                    .sortedWith { a, b ->
                        when {
                            a == "INR" -> -1 // INR first
                            b == "INR" -> 1
                            else -> a.compareTo(b) // Alphabetical for others
                        }
                    }

                // Convert all account balances to selected currency for total
                val selectedCurrency = _uiState.value.selectedCurrency
                val totalBalanceInSelectedCurrency = regularAccounts.sumOf { account ->
                    if (account.currency == selectedCurrency) {
                        account.balance
                    } else {
                        // Convert to selected currency
                        currencyConversionService.convertAmount(
                            amount = account.balance,
                            fromCurrency = account.currency,
                            toCurrency = selectedCurrency
                        ) ?: account.balance
                    }
                }

                val totalAvailableCreditInSelectedCurrency = creditCards.sumOf { card ->
                    // Available = Credit Limit - Outstanding Balance, converted to selected currency
                    val availableInCardCurrency = (card.creditLimit ?: BigDecimal.ZERO) - card.balance
                    if (card.currency == selectedCurrency) {
                        availableInCardCurrency
                    } else {
                        currencyConversionService.convertAmount(
                            amount = availableInCardCurrency,
                            fromCurrency = card.currency,
                            toCurrency = selectedCurrency
                        ) ?: availableInCardCurrency
                    }
                }

                _uiState.value = _uiState.value.copy(
                    accountBalances = regularAccounts,  // Only regular bank accounts
                    creditCards = creditCards,           // Only credit cards
                    totalBalance = totalBalanceInSelectedCurrency,
                    totalAvailableCredit = totalAvailableCreditInSelectedCurrency,
                    availableCurrencies = updatedAvailableCurrencies
                )
            }
        }
    }
    
    fun updateSystemPrompt() {
        viewModelScope.launch {
            try {
                llmRepository.updateSystemPrompt()
            } catch (e: Exception) {
                // Handle error silently or add error state if needed
            }
        }
    }
    
    fun showBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = true)
    }
    
    fun hideBreakdownDialog() {
        _uiState.value = _uiState.value.copy(showBreakdownDialog = false)
    }
    
    /**
     * Checks for app updates using Google Play In-App Updates.
     * Should be called with the current activity context.
     * @param activity The activity context
     * @param snackbarHostState Optional SnackbarHostState for showing restart prompt
     * @param scope Optional CoroutineScope for launching the snackbar
     */
    fun checkForAppUpdate(
        activity: ComponentActivity,
        snackbarHostState: androidx.compose.material3.SnackbarHostState? = null,
        scope: kotlinx.coroutines.CoroutineScope? = null
    ) {
        inAppUpdateManager.checkForUpdate(activity, snackbarHostState, scope)
    }
    
    fun toggleRegretStatus(transactionId: Long, isRegret: Boolean) {
        viewModelScope.launch {
            try {
                transactionRepository.updateRegretStatus(transactionId, isRegret)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error toggling regret status", e)
            }
        }
    }
    
    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            _deletedTransaction.value = transaction
            transactionRepository.deleteTransaction(transaction)
        }
    }
    
    fun undoDelete() {
        _deletedTransaction.value?.let { transaction ->
            viewModelScope.launch {
                transactionRepository.undoDeleteTransaction(transaction)
                _deletedTransaction.value = null
            }
        }
    }
    
    fun undoDeleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            transactionRepository.undoDeleteTransaction(transaction)
        }
    }
    
    fun clearDeletedTransaction() {
        _deletedTransaction.value = null
    }
    
    /**
     * Checks if eligible for in-app review and shows if appropriate.
     * Should be called with the current activity context.
     */
    fun checkForInAppReview(activity: ComponentActivity) {
        viewModelScope.launch {
            // Get current transaction count as additional eligibility factor
            val transactionCount = transactionRepository.getAllTransactions().first().size
            inAppReviewManager.checkAndShowReviewIfEligible(activity, transactionCount)
        }
    }
    
    fun selectCurrency(currency: String) {
        // Update monthly breakdown values from stored maps
        val availableCurrencies = _uiState.value.availableCurrencies
        updateUIStateForCurrency(currency, availableCurrencies)

        // Refresh account balances to convert them to the new selected currency
        refreshAccountBalances()

        // Also refresh transaction type totals for new currency
        viewModelScope.launch {
            val now = LocalDate.now()
            val startOfMonth = now.withDayOfMonth(1)
            val endOfMonth = now.withDayOfMonth(now.lengthOfMonth())

            val transactions = transactionRepository.getTransactionsBetweenDates(
                startDate = startOfMonth,
                endDate = endOfMonth
            ).first()
            updateTransactionTypeTotals(transactions)
        }
        
        // Refresh investment and score data for new currency
        loadInvestmentAndScoreData()
        loadSpendingTrendData()
        loadCategoryDistributionData()
        loadHomeVisualsData()
        viewModelScope.launch { refreshRecurringMonthlyCommitments() }
    }

    fun selectTimeRange(range: HomeTimeRange) {
        _uiState.value = _uiState.value.copy(selectedTimeRange = range)
        loadInvestmentAndScoreData()
        loadSpendingTrendData()
        loadCategoryDistributionData()
    }

    private fun updateTransactionTypeTotals(transactions: List<TransactionEntity>) {
        // Filter transactions by selected currency
        val selectedCurrency = _uiState.value.selectedCurrency
        val currencyTransactions = transactions.filter { it.currency == selectedCurrency }

        val creditCardTotal = currencyTransactions
            .filter { it.transactionType == TransactionType.CREDIT && SpendingAnalyticsFilter.countsAsTrueSpending(it) }
            .sumOf { it.amount }
        val transferTotal = currencyTransactions
            .filter { it.transactionType == TransactionType.TRANSFER }
            .sumOf { it.amount }
        val investmentTotal = currencyTransactions
            .filter { it.transactionType == TransactionType.INVESTMENT }
            .sumOf { it.amount }

        _uiState.value = _uiState.value.copy(
            currentMonthCreditCard = creditCardTotal,
            currentMonthTransfer = transferTotal,
            currentMonthInvestment = investmentTotal
        )
    }

    private fun updateBreakdownForSelectedCurrency(
        breakdownByCurrency: Map<String, TransactionRepository.MonthlyBreakdown>,
        isCurrentMonth: Boolean
    ) {
        // Store the breakdown map for later use when switching currencies
        if (isCurrentMonth) {
            currentMonthBreakdownMap = breakdownByCurrency
        } else {
            lastMonthBreakdownMap = breakdownByCurrency
        }

        // Update available currencies from all stored data
        val allCurrencies = (currentMonthBreakdownMap.keys + lastMonthBreakdownMap.keys).distinct()
        val availableCurrencies = allCurrencies.sortedWith { a, b ->
            when {
                a == "INR" -> -1 // INR first
                b == "INR" -> 1
                else -> a.compareTo(b) // Alphabetical for others
            }
        }

        // Auto-select primary currency if not already selected or if current currency no longer exists
        val currentSelectedCurrency = _uiState.value.selectedCurrency
        val selectedCurrency = if (!availableCurrencies.contains(currentSelectedCurrency) && availableCurrencies.isNotEmpty()) {
            if (availableCurrencies.contains("INR")) "INR" else availableCurrencies.first()
        } else {
            currentSelectedCurrency
        }

        // Update UI state with values for selected currency
        updateUIStateForCurrency(selectedCurrency, availableCurrencies)
    }

    private fun updateUIStateForCurrency(selectedCurrency: String, availableCurrencies: List<String>) {
        // Get breakdown for selected currency from stored maps
        val currentBreakdown = currentMonthBreakdownMap[selectedCurrency] ?: TransactionRepository.MonthlyBreakdown(
            total = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            expenses = BigDecimal.ZERO
        )

        val lastBreakdown = lastMonthBreakdownMap[selectedCurrency] ?: TransactionRepository.MonthlyBreakdown(
            total = BigDecimal.ZERO,
            income = BigDecimal.ZERO,
            expenses = BigDecimal.ZERO
        )

        _uiState.value = _uiState.value.copy(
            currentMonthTotal = currentBreakdown.total,
            currentMonthIncome = currentBreakdown.income,
            currentMonthExpenses = currentBreakdown.expenses,
            lastMonthTotal = lastBreakdown.total,
            lastMonthIncome = lastBreakdown.income,
            lastMonthExpenses = lastBreakdown.expenses,
            selectedCurrency = selectedCurrency,
            availableCurrencies = availableCurrencies
        )
        calculateMonthlyChange()
    }
    
    private fun loadDailySummary() {
        viewModelScope.launch {
            val now = LocalDate.now()
            val startOfDay = now.atStartOfDay()
            val endOfDay = now.atTime(23, 59, 59)
            
            transactionRepository.getTransactionsBetweenDates(startOfDay, endOfDay).collect { transactions ->
                val selectedCurrency = _uiState.value.selectedCurrency
                val currencyTransactions = transactions.filter { it.currency == selectedCurrency }
                
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
                
                _uiState.value = _uiState.value.copy(
                    dailySummary = DailySummary(
                        earnings = earnings,
                        spending = spending,
                        regretSpending = regretSpending,
                        netAmount = netAmount,
                        currency = selectedCurrency
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        inAppUpdateManager.cleanup()
    }
}

data class DailySummary(
    val earnings: BigDecimal = BigDecimal.ZERO,
    val spending: BigDecimal = BigDecimal.ZERO,
    val regretSpending: BigDecimal = BigDecimal.ZERO,
    val netAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "INR"
)

enum class HomeTimeRange(val months: Int, val label: String) {
    MONTHS_3(3, "3M"),
    MONTHS_6(6, "6M"),
    MONTHS_12(12, "12M");

    fun subtitle() = "Last $months months"
}

data class HomeUiState(
    val currentMonthTotal: BigDecimal = BigDecimal.ZERO,
    val currentMonthIncome: BigDecimal = BigDecimal.ZERO,
    val currentMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val currentMonthCreditCard: BigDecimal = BigDecimal.ZERO,
    val currentMonthTransfer: BigDecimal = BigDecimal.ZERO,
    val currentMonthInvestment: BigDecimal = BigDecimal.ZERO,
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    val lastMonthIncome: BigDecimal = BigDecimal.ZERO,
    val lastMonthExpenses: BigDecimal = BigDecimal.ZERO,
    val monthlyChange: BigDecimal = BigDecimal.ZERO,
    val monthlyChangePercent: Int = 0,
    val dailySummary: DailySummary = DailySummary(),
    val recentTransactions: List<TransactionEntity> = emptyList(),
    val upcomingSubscriptions: List<SubscriptionEntity> = emptyList(),
    val upcomingSubscriptionsTotal: BigDecimal = BigDecimal.ZERO,
    val accountBalances: List<AccountBalanceEntity> = emptyList(),
    val creditCards: List<AccountBalanceEntity> = emptyList(),
    val totalBalance: BigDecimal = BigDecimal.ZERO,
    val totalAvailableCredit: BigDecimal = BigDecimal.ZERO,
    val selectedCurrency: String = "INR",
    val availableCurrencies: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val loadingMessage: String? = null,
    val isScanning: Boolean = false,
    val isRefreshing: Boolean = false,
    val showBreakdownDialog: Boolean = false,
    val totalInvestment12Months: BigDecimal = BigDecimal.ZERO,
    val averageIncome12Months: BigDecimal = BigDecimal.ZERO,
    val incomeLast12Months: BigDecimal = BigDecimal.ZERO,
    val spendLast12Months: BigDecimal = BigDecimal.ZERO,
    val finnDotScore: Float = 0f,
    val monthlySpendingData: List<MonthlySpendingData> = emptyList(),
    val categoryDistributionData: List<CategorySpendingData> = emptyList(),
    val monthlyBudget: BigDecimal = BigDecimal.ZERO,
    val hasCustomBudget: Boolean = false,
    val recommendedMonthlyBudget: BigDecimal = BigDecimal.ZERO,
    val monthlyActualSpending: BigDecimal = BigDecimal.ZERO,
    val monthlyBudgetRemaining: BigDecimal = BigDecimal.ZERO,
    val monthlyBudgetPercentageUsed: Float = 0f,
    val budgetScore: Float = 0f,
    val monthlyRegretAmount: BigDecimal = BigDecimal.ZERO,
    val selectedTimeRange: HomeTimeRange = HomeTimeRange.MONTHS_12,
    val last7DaysDailySpend: List<BigDecimal> = emptyList(),
    val projectedMonthEndSpend: BigDecimal = BigDecimal.ZERO,
    val daysLeftInMonth: Int = 0,
    val monthProgressPercent: Float = 0f,
    val dailyBudgetAmount: BigDecimal = BigDecimal.ZERO,
    val recurringBillsPredictedTotalThisMonth: BigDecimal = BigDecimal.ZERO,
    val recurringBillsExpectedCountThisMonth: Int = 0,
    val recurringBillsPaidAmountThisMonth: BigDecimal = BigDecimal.ZERO,
    val recurringBillsProgressThisMonth: Float = 0f,
    val recurringBillsNextMerchant: String? = null,
    val recurringBillsNextAmount: BigDecimal? = null,
    val recurringBillsNextDaysUntilDue: Int? = null,
    val intervalRecurringCount: Int = 0,
    val intervalRecurringNextMerchant: String? = null,
    val intervalRecurringNextAmount: BigDecimal? = null,
    val intervalRecurringNextDaysUntilDue: Int? = null,
)