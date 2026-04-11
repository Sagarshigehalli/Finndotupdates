package com.anomapro.finndot.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.repository.BudgetRepository
import com.anomapro.finndot.data.repository.LlmRepository
import com.anomapro.finndot.data.repository.ModelRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.data.repository.ModelState
import com.anomapro.finndot.data.repository.SubscriptionRepository
import com.anomapro.finndot.presentation.common.TimePeriod
import com.anomapro.finndot.presentation.common.TransactionTypeFilter
import com.anomapro.finndot.presentation.common.getDateRangeForPeriod
import androidx.compose.ui.graphics.Color
import com.anomapro.finndot.ui.components.CategorySpendingData
import com.anomapro.finndot.ui.components.MonthlySpendingData
import com.anomapro.finndot.ui.icons.CategoryMapping
import com.anomapro.finndot.utils.CurrencyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class SavingsInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val amount: BigDecimal? = null,
    val actionHint: String? = null,
    val impactScore: Int = 0,
    val categoryName: String? = null,
    val merchantName: String? = null
)

enum class InsightType {
    TOP_CATEGORY,
    CATEGORY_OVERSPEND,
    MONTH_COMPARISON,
    SUBSCRIPTIONS,
    IMPULSE_SPENDING,
    REGRET_SPENDING,
    AVERAGE_TRANSACTION,
    SPENDING_VELOCITY,
    GET_ON_TRACK,
    DAY_PATTERN,
    BUDGET_OVERSPEND,
    MERCHANT_SPIKE,
    IMPROVEMENT_STREAK,
    WHAT_IF,
    POTENTIAL_SAVINGS
}

data class QuickWin(
    val action: String,
    val amount: BigDecimal,
    val categoryName: String? = null,
    val merchantName: String? = null
)

private object CategoryBenchmarks {
    val maxPercent: Map<String, Int> = mapOf(
        "Food & Dining" to 25,
        "Groceries" to 18,
        "Transportation" to 15,
        "Shopping" to 15,
        "Entertainment" to 12,
        "Bills & Utilities" to 15,
        "Travel" to 20,
        "Personal Care" to 8,
        "Healthcare" to 10
    )
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val llmRepository: LlmRepository,
    private val modelRepository: ModelRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val budgetRepository: BudgetRepository
) : ViewModel() {
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.LAST_12_MONTHS)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.EXPENSE)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR") // Default to INR
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _availableCurrencies = MutableStateFlow<List<String>>(emptyList())
    val availableCurrencies: StateFlow<List<String>> = _availableCurrencies.asStateFlow()

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    val modelDownloaded: StateFlow<Boolean> = modelRepository.modelState
        .map { it == ModelState.READY }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val modelState: StateFlow<ModelState> = modelRepository.modelState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ModelState.NOT_DOWNLOADED)

    private val _aiAdviceState = MutableStateFlow<AiAdviceState>(AiAdviceState.Idle)
    val aiAdviceState: StateFlow<AiAdviceState> = _aiAdviceState.asStateFlow()
    
    init {
        loadAnalytics()
    }

    fun requestAiSavingsAdvice() {
        viewModelScope.launch {
            _aiAdviceState.value = AiAdviceState.Loading
            try {
                val ctx = buildAnalyticsContextForAi()
                var fullResponse = ""
                var lastUpdateTime = 0L
                val throttleMs = 400L
                withContext(Dispatchers.IO) {
                    withTimeout(120_000L) {
                        llmRepository.getAnalyticsSavingsAdvice(ctx)
                            .collect { chunk ->
                                fullResponse += chunk
                                val now = System.currentTimeMillis()
                                if (now - lastUpdateTime >= throttleMs) {
                                    lastUpdateTime = now
                                    withContext(Dispatchers.Main) {
                                        _aiAdviceState.value = AiAdviceState.Content(fullResponse)
                                    }
                                }
                            }
                    }
                }
                _aiAdviceState.value = AiAdviceState.Content(fullResponse)
            } catch (e: TimeoutCancellationException) {
                _aiAdviceState.value = AiAdviceState.Error("Request took too long. Try again or use chat.")
            } catch (e: Exception) {
                _aiAdviceState.value = AiAdviceState.Error(
                    e.message ?: "Failed to get advice"
                )
            }
        }
    }

    fun dismissAiAdvice() {
        _aiAdviceState.value = AiAdviceState.Idle
    }

    private fun buildAnalyticsContextForAi(): String {
        val state = _uiState.value
        val categoryLines = state.categoryBreakdown.take(5).joinToString("\n") { c ->
            "- ${c.name}: ${CurrencyFormatter.formatCurrency(c.amount, state.currency)} (${c.percentage.toInt()}%)"
        }
        val merchantLines = state.topMerchants.take(5).joinToString("\n") { m ->
            "- ${m.name}: ${CurrencyFormatter.formatCurrency(m.amount, state.currency)}"
        }
        val insightsLines = state.savingsInsights.take(5).joinToString("\n") { i ->
            "- ${i.title}: ${i.message.take(80)}..."
        }
        val quickWinsLine = state.quickWins.take(3).joinToString("\n") { w ->
            "- ${w.action} = ${CurrencyFormatter.formatCurrency(w.amount, state.currency)}"
        }
        return """
            Period: ${_selectedPeriod.value.label}
            Total: ${CurrencyFormatter.formatCurrency(state.totalSpending, state.currency)} across ${state.transactionCount} transactions
            Avg per txn: ${CurrencyFormatter.formatCurrency(state.averageAmount, state.currency)}
            This month vs last: ${CurrencyFormatter.formatCurrency(state.currentMonthTotal, state.currency)} vs ${CurrencyFormatter.formatCurrency(state.lastMonthTotal, state.currency)}
            Potential savings: ${CurrencyFormatter.formatCurrency(state.totalSavingsPotential, state.currency)}
            ${if (state.improvementStreak > 0) "Spending down ${state.improvementStreak} months in a row - good!" else ""}
            ${if (state.cutAmountToMatch != null) "Cut ${CurrencyFormatter.formatCurrency(state.cutAmountToMatch, state.currency)}/day to match last month." else ""}
            
            Top categories:
            $categoryLines
            
            Top merchants:
            $merchantLines
            
            Key money leaks:
            $insightsLines
            ${if (quickWinsLine.isNotEmpty()) "Quick wins:\n$quickWinsLine" else ""}
        """.trimIndent()
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
        loadAnalytics()
    }
    
    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
        loadAnalytics()
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
        loadAnalytics()
    }

    fun refresh() {
        loadAnalytics()
    }
    
    private fun loadAnalytics() {
        viewModelScope.launch {
            val dateRange = getDateRangeForPeriod(_selectedPeriod.value)
            val now = LocalDate.now()
            val twelveMonthsStart = now.minusMonths(11).withDayOfMonth(1).atStartOfDay()
            val twelveMonthsEnd = now.atTime(23, 59, 59)
            val investmentData = transactionRepository.getTransactionsBetweenDates(twelveMonthsStart, twelveMonthsEnd).first()

            transactionRepository.getTransactionsBetweenDates(
                startDate = dateRange.first,
                endDate = dateRange.second
            ).collect { transactions ->
                // Update available currencies
                val allCurrencies = transactions.map { it.currency }.distinct().sortedWith { a, b ->
                    when {
                        a == "INR" -> -1 // INR first
                        b == "INR" -> 1
                        else -> a.compareTo(b) // Alphabetical for others
                    }
                }
                _availableCurrencies.value = allCurrencies

                // Auto-select primary currency if not already selected or if current currency no longer exists
                val currentSelectedCurrency = _selectedCurrency.value
                if (!allCurrencies.contains(currentSelectedCurrency) && allCurrencies.isNotEmpty()) {
                    _selectedCurrency.value = if (allCurrencies.contains("INR")) "INR" else allCurrencies.first()
                }

                // Filter by selected currency first
                val currencyFilteredTransactions = transactions.filter { it.currency == _selectedCurrency.value }

                // Income vs expenses for selected period (helps low-income users see what's left)
                val periodIncome = currencyFilteredTransactions
                    .filter { it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME }
                    .sumOf { it.amount.abs() }
                val periodExpenses = currencyFilteredTransactions
                    .filter { it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE }
                    .sumOf { it.amount.abs() }
                val periodRemaining = periodIncome.subtract(periodExpenses)

                // Runway: approx days of money left at current spend rate (this month only)
                val runwayDays = if (_selectedPeriod.value == TimePeriod.THIS_MONTH &&
                    periodExpenses > BigDecimal.ZERO && periodRemaining > BigDecimal.ZERO) {
                    val daysElapsed = now.dayOfMonth.coerceAtLeast(1)
                    (periodRemaining / (periodExpenses / BigDecimal(daysElapsed))).toInt().coerceAtLeast(0)
                } else null

                // Filter by selected transaction type
                val filteredTransactions = when (_transactionTypeFilter.value) {
                    TransactionTypeFilter.ALL -> currencyFilteredTransactions
                    TransactionTypeFilter.INCOME -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME
                    }
                    TransactionTypeFilter.EXPENSE -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE
                    }
                    TransactionTypeFilter.SPEND -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE ||
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.CREDIT
                    }
                    TransactionTypeFilter.CREDIT -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.CREDIT
                    }
                    TransactionTypeFilter.TRANSFER -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.TRANSFER
                    }
                    TransactionTypeFilter.INVESTMENT -> currencyFilteredTransactions.filter {
                        it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INVESTMENT
                    }
                }
                
                // Calculate total (use abs() for consistency - expenses may be stored negative)
                val totalSpending = filteredTransactions.sumOf { it.amount.abs().toDouble() }.toBigDecimal()
                
                // Group by category
                val categoryBreakdown = filteredTransactions
                    .groupBy { it.category ?: "Others" }
                    .map { (categoryName, txns) -> 
                        val categoryTotal = txns.sumOf { it.amount.abs().toDouble() }.toBigDecimal()
                        CategoryData(
                            name = categoryName,
                            amount = categoryTotal,
                            percentage = if (totalSpending > BigDecimal.ZERO) {
                                (categoryTotal.divide(totalSpending, 4, java.math.RoundingMode.HALF_UP) * BigDecimal(100)).toFloat()
                            } else 0f,
                            transactionCount = txns.size
                        )
                    }
                    .sortedByDescending { it.amount }
                
                // Group by merchant
                val merchantBreakdown = filteredTransactions
                    .groupBy { it.merchantName }
                    .mapValues { (merchant, txns) -> 
                        MerchantData(
                            name = merchant,
                            amount = txns.sumOf { it.amount.abs().toDouble() }.toBigDecimal(),
                            transactionCount = txns.size,
                            isSubscription = txns.any { it.isRecurring }
                        )
                    }
                    .values
                    .sortedByDescending { it.amount }
                    .take(10) // Top 10 merchants
                
                // Calculate average amount
                val averageAmount = if (filteredTransactions.isNotEmpty()) {
                    totalSpending.divide(BigDecimal(filteredTransactions.size), 2, java.math.RoundingMode.HALF_UP)
                } else {
                    BigDecimal.ZERO
                }
                
                // Get top category info
                val topCategory = categoryBreakdown.firstOrNull()
                
                // Calculate current and last month totals for comparison
                val now = LocalDate.now()
                val currentMonthStart = now.withDayOfMonth(1).atStartOfDay()
                val currentMonthEnd = now.atTime(23, 59, 59)
                val lastMonth = now.minusMonths(1)
                val lastMonthStart = lastMonth.withDayOfMonth(1).atStartOfDay()
                val lastMonthEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth()).atTime(23, 59, 59)
                
                val currentMonthTransactions = transactions
                    .filter { it.currency == _selectedCurrency.value }
                    .filter { it.dateTime >= currentMonthStart && it.dateTime <= currentMonthEnd }
                    .filter { 
                        when (_transactionTypeFilter.value) {
                            TransactionTypeFilter.EXPENSE -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE
                            TransactionTypeFilter.INCOME -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME
                            else -> true
                        }
                    }
                
                val lastMonthTransactions = transactions
                    .filter { it.currency == _selectedCurrency.value }
                    .filter { it.dateTime >= lastMonthStart && it.dateTime <= lastMonthEnd }
                    .filter { 
                        when (_transactionTypeFilter.value) {
                            TransactionTypeFilter.EXPENSE -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE
                            TransactionTypeFilter.INCOME -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME
                            else -> true
                        }
                    }
                
                val currentMonthTotal = currentMonthTransactions.sumOf { it.amount.abs() }
                val lastMonthTotal = lastMonthTransactions.sumOf { it.amount.abs() }

                // Monthly trend for SpendingTrendChart
                val monthlyTrendData = currencyFilteredTransactions
                    .filter {
                        when (_transactionTypeFilter.value) {
                            TransactionTypeFilter.EXPENSE -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE
                            TransactionTypeFilter.INCOME -> it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME
                            else -> true
                        }
                    }
                    .groupBy { YearMonth.from(it.dateTime) }
                    .map { (month, monthTxns) ->
                        val expenses = monthTxns
                            .filter { it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE }
                            .sumOf { it.amount.abs() }
                        val income = monthTxns
                            .filter { it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INCOME }
                            .sumOf { it.amount.abs() }
                        val regret = monthTxns
                            .filter { it.isRegret && it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE }
                            .sumOf { it.amount.abs() }
                        MonthlySpendingData(month, expenses, income, regret, _selectedCurrency.value)
                    }
                    .sortedBy { it.month }

                // Category distribution with colors
                val categoryDistributionData = categoryBreakdown.map { cat ->
                    val info = CategoryMapping.categories[cat.name] ?: CategoryMapping.categories["Others"]!!
                    CategorySpendingData(cat.name, cat.amount, cat.percentage, info.color)
                }

                // Money leaks & savings insights - ordered by impact
                val insights = mutableListOf<SavingsInsight>()
                var totalSavingsPotential = BigDecimal.ZERO
                var cutAmountToMatch: BigDecimal? = null
                var whatIfSavings: BigDecimal? = null
                var improvementStreak = 0
                val quickWins = mutableListOf<QuickWin>()
                val sym = CurrencyFormatter.getCurrencySymbol(_selectedCurrency.value)

                // 1. Regret spending - money you wish you hadn't spent (high impact)
                val regretTotal = filteredTransactions.filter { it.isRegret }.sumOf { it.amount.abs() }
                if (regretTotal > BigDecimal.ZERO) {
                    val regretCount = filteredTransactions.count { it.isRegret }
                    val topRegretCategory = filteredTransactions.filter { it.isRegret }
                        .groupBy { it.category ?: "Others" }
                        .maxByOrNull { it.value.sumOf { t -> t.amount.abs() } }
                    insights.add(SavingsInsight(
                        InsightType.REGRET_SPENDING,
                        "Spending you marked as regret",
                        "You marked $sym${regretTotal.setScale(0, RoundingMode.HALF_UP)} across $regretCount purchases. ${topRegretCategory?.let { "Most in ${it.key}." } ?: ""} Tap to review and avoid repeating.",
                        regretTotal,
                        "Tap to view transactions",
                        90,
                        topRegretCategory?.key
                    ))
                    totalSavingsPotential = totalSavingsPotential.add(regretTotal)
                }

                // 2. Category overspend vs benchmarks
                categoryBreakdown.forEach { cat ->
                    val benchmark = CategoryBenchmarks.maxPercent[cat.name] ?: 25
                    if (cat.percentage > benchmark && totalSpending > BigDecimal.ZERO) {
                        val cut20Amount = cat.amount.multiply(BigDecimal("0.2")).setScale(0, RoundingMode.HALF_UP)
                        insights.add(SavingsInsight(
                            InsightType.CATEGORY_OVERSPEND,
                            "${cat.name} is ${cat.percentage.toInt()}% (typical: $benchmark%)",
                            "You're over. Cut 20% to save ${CurrencyFormatter.formatCurrency(cut20Amount, _selectedCurrency.value)}.",
                            cut20Amount,
                            "Tap to view transactions",
                            85,
                            cat.name
                        ))
                        totalSavingsPotential = totalSavingsPotential.add(cut20Amount)
                    }
                }

                // 3. Top category / biggest leak
                topCategory?.let { topCat ->
                    if (topCat.percentage > 25 && !insights.any { it.title.contains(topCat.name) }) {
                        val potential = categoryBreakdown.firstOrNull { it.name == topCat.name }?.let { c ->
                            c.amount.multiply(BigDecimal("0.2")).setScale(0, RoundingMode.HALF_UP)
                        }
                        insights.add(SavingsInsight(
                            InsightType.TOP_CATEGORY,
                            "Your biggest leak: ${topCat.name}",
                            "${topCat.percentage.toInt()}% of spending. Cut 20% to save ${potential?.let { CurrencyFormatter.formatCurrency(it, _selectedCurrency.value) } ?: ""}.",
                            potential,
                            "Tap to view transactions",
                            80,
                            topCat.name
                        ))
                        potential?.let { totalSavingsPotential = totalSavingsPotential.add(it) }
                    }
                }
                // 4. Impulse spending - many small transactions
                if (_transactionTypeFilter.value == TransactionTypeFilter.EXPENSE && filteredTransactions.isNotEmpty()) {
                    val impulseThreshold = maxOf(BigDecimal(200), averageAmount.multiply(BigDecimal("0.3")))
                    val impulseTxns = filteredTransactions.filter { it.amount.abs() < impulseThreshold }
                    val impulseTotal = impulseTxns.sumOf { it.amount.abs() }
                    if (impulseTxns.size >= 8 && impulseTotal > totalSpending.multiply(BigDecimal("0.15"))) {
                        insights.add(SavingsInsight(
                            InsightType.IMPULSE_SPENDING,
                            "${impulseTxns.size} small buys (under ${CurrencyFormatter.formatCurrency(impulseThreshold, _selectedCurrency.value)})",
                            "Adds up to ${CurrencyFormatter.formatCurrency(impulseTotal, _selectedCurrency.value)}. Small purchases often become regrets.",
                            impulseTotal,
                            "Review in Transactions",
                            75
                        ))
                        totalSavingsPotential = totalSavingsPotential.add(impulseTotal.multiply(BigDecimal("0.3")))
                    }
                }

                // 5. Subscriptions
                val activeSubs = subscriptionRepository.getActiveSubscriptions().first()
                if (activeSubs.isNotEmpty()) {
                    val subTotal = activeSubs.sumOf { it.amount }
                    val potentialCut = subTotal.multiply(BigDecimal("0.25"))
                    insights.add(SavingsInsight(
                        InsightType.SUBSCRIPTIONS,
                        "${activeSubs.size} subscriptions = ${CurrencyFormatter.formatCurrency(subTotal, _selectedCurrency.value)}/month",
                        "Cancel 1-2 unused. Most people save 25% on subs.",
                        subTotal,
                        "Review Subscriptions",
                        70
                    ))
                    totalSavingsPotential = totalSavingsPotential.add(potentialCut)
                }

                // 6. Month comparison
                if (currentMonthTotal > BigDecimal.ZERO && lastMonthTotal > BigDecimal.ZERO) {
                    val changePct = ((currentMonthTotal - lastMonthTotal) / lastMonthTotal * BigDecimal(100))
                    val isUp = changePct > BigDecimal.ZERO
                    val diff = (currentMonthTotal - lastMonthTotal).abs()
                    insights.add(SavingsInsight(
                        InsightType.MONTH_COMPARISON,
                        if (isUp) "Up ${changePct.setScale(0, RoundingMode.HALF_UP)}% vs last month" else "Down ${changePct.abs().setScale(0, RoundingMode.HALF_UP)}%",
                        if (isUp) "That's ${CurrencyFormatter.formatCurrency(diff, _selectedCurrency.value)} extra. Find which category grew." else "Saved ${CurrencyFormatter.formatCurrency(diff, _selectedCurrency.value)}.",
                        if (isUp) diff else null,
                        if (isUp) "Compare categories" else null,
                        60
                    ))
                }

                // 7. Spending velocity + Get on track
                if (_selectedPeriod.value == TimePeriod.THIS_MONTH || _selectedPeriod.value == TimePeriod.LAST_12_MONTHS) {
                    val daysElapsed = now.dayOfMonth
                    val daysInMonth = now.lengthOfMonth()
                    val daysRemaining = daysInMonth - daysElapsed
                    if (daysElapsed > 2 && currentMonthTotal > BigDecimal.ZERO && lastMonthTotal > BigDecimal.ZERO && daysRemaining > 0) {
                        val dailyRate = currentMonthTotal.divide(BigDecimal(daysElapsed), 2, RoundingMode.HALF_UP)
                        val projectedMonthEnd = dailyRate.multiply(BigDecimal(daysInMonth))
                        if (projectedMonthEnd > lastMonthTotal) {
                            val overProjection = projectedMonthEnd.subtract(lastMonthTotal)
                            val cutPerDay = overProjection.divide(BigDecimal(daysRemaining), 2, RoundingMode.HALF_UP)
                            cutAmountToMatch = cutPerDay
                            insights.add(SavingsInsight(
                                InsightType.SPENDING_VELOCITY,
                                "At this rate: ${CurrencyFormatter.formatCurrency(projectedMonthEnd, _selectedCurrency.value)} by month end",
                                "Cut ${CurrencyFormatter.formatCurrency(cutPerDay, _selectedCurrency.value)}/day for the next $daysRemaining days to match last month.",
                                overProjection,
                                "Reduce daily spending",
                                65
                            ))
                        }
                    }
                }

                // 8. Average transaction (lower priority)
                if (averageAmount > BigDecimal.ZERO && _transactionTypeFilter.value == TransactionTypeFilter.EXPENSE && filteredTransactions.size > 15) {
                    insights.add(SavingsInsight(
                        InsightType.AVERAGE_TRANSACTION,
                        "${CurrencyFormatter.formatCurrency(averageAmount, _selectedCurrency.value)} per transaction avg",
                        "With ${filteredTransactions.size} txns, cut 2-3 unnecessary ones to save.",
                        averageAmount.multiply(BigDecimal(3)),
                        null,
                        40
                    ))
                }

                // 9. Day-of-week pattern
                if (_transactionTypeFilter.value == TransactionTypeFilter.EXPENSE && currentMonthTransactions.isNotEmpty()) {
                    val byDay = currentMonthTransactions.groupBy { it.dateTime.dayOfWeek }
                    val weekend = listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
                    val weekendTotal = byDay.filterKeys { it in weekend }.values.flatten().sumOf { it.amount.abs() }
                    val weekdayTotal = byDay.filterKeys { it !in weekend }.values.flatten().sumOf { it.amount.abs() }
                    if (weekendTotal > BigDecimal.ZERO && weekdayTotal > BigDecimal.ZERO) {
                        val weekendPct = (weekendTotal.toDouble() / (weekendTotal + weekdayTotal).toDouble() * 100).toInt()
                        if (weekendPct > 40) {
                            insights.add(SavingsInsight(
                                InsightType.DAY_PATTERN,
                                "You spend more on weekends",
                                "${weekendPct}% of spending is Sat–Sun. Plan weekday meals to avoid weekend splurges.",
                                weekendTotal,
                                null,
                                55
                            ))
                        }
                    }
                }

                // 10. Budget overspend
                val yearMonth = YearMonth.from(now)
                val budgets = try {
                    budgetRepository.getBudgetsForMonth(yearMonth, _selectedCurrency.value).first()
                } catch (e: Exception) { emptyList() }
                if (budgets.isNotEmpty() && _transactionTypeFilter.value == TransactionTypeFilter.EXPENSE) {
                    val spendByCategory = transactions
                        .filter { it.currency == _selectedCurrency.value }
                        .filter { it.dateTime >= currentMonthStart && it.dateTime <= currentMonthEnd }
                        .filter {
                            it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.EXPENSE ||
                                it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.CREDIT
                        }
                        .groupBy { it.category ?: "Others" }
                    budgets.forEach { budget ->
                        if (budget.category != "All") {
                            val spent = spendByCategory[budget.category]?.sumOf { it.amount.abs() } ?: BigDecimal.ZERO
                            if (spent > budget.amount && budget.amount > BigDecimal.ZERO) {
                                val over = spent.subtract(budget.amount)
                                insights.add(SavingsInsight(
                                    InsightType.BUDGET_OVERSPEND,
                                    "${budget.category} over budget by ${CurrencyFormatter.formatCurrency(over, _selectedCurrency.value)}",
                                    "Spent ${CurrencyFormatter.formatCurrency(spent, _selectedCurrency.value)} vs ${CurrencyFormatter.formatCurrency(budget.amount, _selectedCurrency.value)} limit.",
                                    over,
                                    "Tap to view transactions",
                                    88,
                                    budget.category
                                ))
                            }
                        }
                    }
                }

                // 11. Merchant spike (vs last month)
                if (_transactionTypeFilter.value == TransactionTypeFilter.EXPENSE) {
                    val currentByMerchant = currentMonthTransactions.groupBy { it.merchantName }.mapValues { it.value.sumOf { t -> t.amount.abs() } }
                    val lastByMerchant = lastMonthTransactions.groupBy { it.merchantName }.mapValues { it.value.sumOf { t -> t.amount.abs() } }
                    val spikes = currentByMerchant.filter { (merchant, curr) ->
                        val last = lastByMerchant[merchant] ?: BigDecimal.ZERO
                        last > BigDecimal.ZERO && curr >= last.multiply(BigDecimal("1.5"))
                    }.toList().sortedByDescending { it.second }.take(2)
                    spikes.forEach { (merchant, curr) ->
                        val last = lastByMerchant[merchant]!!
                        val mult = (curr.toDouble() / last.toDouble()).toInt()
                        insights.add(SavingsInsight(
                            InsightType.MERCHANT_SPIKE,
                            "2x+ at $merchant vs last month",
                            "Spent ${CurrencyFormatter.formatCurrency(curr, _selectedCurrency.value)} (${mult}x). Check if it's worth it.",
                            curr.subtract(last),
                            "Tap to view",
                            72,
                            merchantName = merchant
                        ))
                    }
                }

                // 12. Improvement streak
                if (monthlyTrendData.size >= 2 && _transactionTypeFilter.value == TransactionTypeFilter.EXPENSE) {
                    val sortedMonths = monthlyTrendData.sortedBy { it.month }
                    val amounts = sortedMonths.map { it.amount }
                    var streak = 0
                    for (i in amounts.size - 1 downTo 1) {
                        if (amounts[i] < amounts[i - 1]) streak++ else break
                    }
                    if (streak >= 1) {
                        improvementStreak = streak
                        insights.add(SavingsInsight(
                            InsightType.IMPROVEMENT_STREAK,
                            "Spending down $streak month${if (streak > 1) "s" else ""} in a row",
                            "Keep it up. You're building good habits.",
                            null,
                            null,
                            50
                        ))
                    }
                }

                // 13. What-if summary
                if (categoryBreakdown.size >= 3 && totalSpending > BigDecimal.ZERO) {
                    val top3 = categoryBreakdown.take(3)
                    val cut15 = top3.sumOf { it.amount.multiply(BigDecimal("0.15")) }
                    if (cut15 > BigDecimal.ZERO) {
                        whatIfSavings = cut15.setScale(0, RoundingMode.HALF_UP)
                        insights.add(SavingsInsight(
                            InsightType.WHAT_IF,
                            "Cut top 3 categories 15% = ${CurrencyFormatter.formatCurrency(cut15.setScale(0, RoundingMode.HALF_UP), _selectedCurrency.value)}/period",
                            "Food, Groceries, Transport – small cuts add up.",
                            cut15.setScale(0, RoundingMode.HALF_UP),
                            null,
                            45
                        ))
                    }
                }

                // 14. Quick wins - concrete actions
                if (_transactionTypeFilter.value == TransactionTypeFilter.EXPENSE) {
                    topCategory?.let { topCat ->
                        val catAmount = categoryBreakdown.firstOrNull { it.name == topCat.name }?.amount ?: BigDecimal.ZERO
                        if (catAmount > BigDecimal.ZERO) {
                            val avgTxn = catAmount.divide(BigDecimal(topCat.transactionCount.coerceAtLeast(1)), 0, RoundingMode.HALF_UP)
                            if (avgTxn > BigDecimal(100)) {
                                quickWins.add(QuickWin(
                                    "Skip 2 ${topCat.name} orders this week",
                                    avgTxn.multiply(BigDecimal(2)),
                                    topCat.name
                                ))
                            }
                        }
                    }
                    activeSubs.take(1).forEach { sub ->
                        quickWins.add(QuickWin(
                            "Pause 1 subscription: ${sub.merchantName}",
                            sub.amount,
                            merchantName = sub.merchantName
                        ))
                    }
                    if (averageAmount > BigDecimal(500) && filteredTransactions.size > 10) {
                        quickWins.add(QuickWin(
                            "Cut 1 discretionary purchase today",
                            averageAmount,
                            null
                        ))
                    }
                }

                // 12-month investment and FinnDot score (for selected currency)
                val currency12 = investmentData.filter { it.currency == _selectedCurrency.value }
                val totalInvestment12Months = currency12
                    .filter { it.transactionType == com.anomapro.finndot.data.database.entity.TransactionType.INVESTMENT }
                    .sumOf { it.amount.abs() }
                val totalTxnAmount12 = currency12.sumOf { it.amount.abs() }
                val finnDotScore = if (totalTxnAmount12 > BigDecimal.ZERO) {
                    val baseScore = (totalInvestment12Months / totalTxnAmount12).toFloat().coerceIn(0f, 1f) * 100f
                    (baseScore + 70f).coerceIn(0f, 100f) / 100f
                } else 0.7f

                insights.sortByDescending { it.impactScore }

                _uiState.value = AnalyticsUiState(
                    totalSpending = totalSpending,
                    categoryBreakdown = categoryBreakdown,
                    topMerchants = merchantBreakdown,
                    transactionCount = filteredTransactions.size,
                    averageAmount = averageAmount,
                    topCategory = topCategory?.name,
                    topCategoryPercentage = topCategory?.percentage ?: 0f,
                    currency = _selectedCurrency.value,
                    isLoading = false,
                    currentMonthTotal = currentMonthTotal,
                    lastMonthTotal = lastMonthTotal,
                    monthlyTrendData = monthlyTrendData,
                    categoryDistributionData = categoryDistributionData,
                    savingsInsights = insights,
                    totalSavingsPotential = totalSavingsPotential,
                    quickWins = quickWins,
                    cutAmountToMatch = cutAmountToMatch,
                    whatIfSavings = whatIfSavings,
                    improvementStreak = improvementStreak,
                    totalInvestment12Months = totalInvestment12Months,
                    finnDotScore = finnDotScore,
                    periodIncome = periodIncome,
                    periodExpenses = periodExpenses,
                    periodRemaining = periodRemaining,
                    runwayDays = runwayDays,
                    activeSubscriptionsCount = activeSubs.size
                )
            }
        }
    }
    
}

sealed class AiAdviceState {
    data object Idle : AiAdviceState()
    data object Loading : AiAdviceState()
    data class Content(val text: String) : AiAdviceState()
    data class Error(val message: String) : AiAdviceState()
}

data class AnalyticsUiState(
    val totalSpending: BigDecimal = BigDecimal.ZERO,
    val categoryBreakdown: List<CategoryData> = emptyList(),
    val topMerchants: List<MerchantData> = emptyList(),
    val transactionCount: Int = 0,
    val averageAmount: BigDecimal = BigDecimal.ZERO,
    val topCategory: String? = null,
    val topCategoryPercentage: Float = 0f,
    val currency: String = "INR",
    val isLoading: Boolean = true,
    val currentMonthTotal: BigDecimal = BigDecimal.ZERO,
    val lastMonthTotal: BigDecimal = BigDecimal.ZERO,
    val monthlyTrendData: List<MonthlySpendingData> = emptyList(),
    val categoryDistributionData: List<CategorySpendingData> = emptyList(),
    val savingsInsights: List<SavingsInsight> = emptyList(),
    val totalSavingsPotential: BigDecimal = BigDecimal.ZERO,
    val quickWins: List<QuickWin> = emptyList(),
    val cutAmountToMatch: BigDecimal? = null,
    val whatIfSavings: BigDecimal? = null,
    val improvementStreak: Int = 0,
    val totalInvestment12Months: BigDecimal = BigDecimal.ZERO,
    val finnDotScore: Float = 0.7f,
    val periodIncome: BigDecimal = BigDecimal.ZERO,
    val periodExpenses: BigDecimal = BigDecimal.ZERO,
    val periodRemaining: BigDecimal = BigDecimal.ZERO,
    val runwayDays: Int? = null,
    val activeSubscriptionsCount: Int = 0
)

data class CategoryData(
    val name: String,
    val amount: BigDecimal,
    val percentage: Float,
    val transactionCount: Int
)

data class MerchantData(
    val name: String,
    val amount: BigDecimal,
    val transactionCount: Int,
    val isSubscription: Boolean
)

