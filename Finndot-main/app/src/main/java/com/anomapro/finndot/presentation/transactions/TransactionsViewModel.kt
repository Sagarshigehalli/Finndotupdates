package com.anomapro.finndot.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.database.entity.CategoryEntity
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.CategoryRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.presentation.common.TimePeriod
import com.anomapro.finndot.presentation.common.TransactionTypeFilter
import com.anomapro.finndot.presentation.common.getDateRangeForPeriod
import com.anomapro.finndot.presentation.common.CurrencyGroupedTotals
import com.anomapro.finndot.presentation.common.CurrencyTotals
import com.anomapro.finndot.core.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject
import com.anomapro.finndot.ui.components.CategorySpendingData
import com.anomapro.finndot.ui.icons.CategoryMapping
import com.anomapro.finndot.domain.analytics.SpendingAnalyticsFilter

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: com.anomapro.finndot.data.preferences.UserPreferencesRepository,
    private val merchantNameMappingRepository: com.anomapro.finndot.data.repository.MerchantNameMappingRepository,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedPeriod = MutableStateFlow(TimePeriod.LAST_12_MONTHS)
    val selectedPeriod: StateFlow<TimePeriod> = _selectedPeriod.asStateFlow()
    
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()
    
    private val _transactionTypeFilter = MutableStateFlow(TransactionTypeFilter.ALL)
    val transactionTypeFilter: StateFlow<TransactionTypeFilter> = _transactionTypeFilter.asStateFlow()
    
    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    private val _selectedCurrency = MutableStateFlow("INR") // Default to INR
    val selectedCurrency: StateFlow<String> = _selectedCurrency.asStateFlow()

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    
    private val _currencyGroupedTotals = MutableStateFlow(CurrencyGroupedTotals())
    val currencyGroupedTotals: StateFlow<CurrencyGroupedTotals> = _currencyGroupedTotals.asStateFlow()

    // Available currencies for the selected time period
    val availableCurrencies: StateFlow<List<String>> = selectedPeriod.flatMapLatest { period ->
        if (period == TimePeriod.ALL) {
            transactionRepository.getAllCurrencies()
        } else {
            val (startDate, endDate) = getDateRangeForPeriod(period)
            val startDateTime = startDate.atStartOfDay()
            val endDateTime = endDate.atTime(23, 59, 59)
            transactionRepository.getCurrenciesForPeriod(startDateTime, endDateTime)
        }
    }
        .map { currencies ->
            currencies.sortedWith { a, b ->
                when {
                    a == "INR" -> -1 // INR first
                    b == "INR" -> 1
                    else -> a.compareTo(b) // Alphabetical for others
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Computed property for current selected currency totals
    val filteredTotals: StateFlow<FilteredTotals> = combine(
        _currencyGroupedTotals,
        _selectedCurrency
    ) { groupedTotals, currency ->
        val currencyTotals = groupedTotals.getTotalsForCurrency(currency)
        FilteredTotals(
            income = currencyTotals.income,
            expenses = currencyTotals.expenses,
            credit = currencyTotals.credit,
            transfer = currencyTotals.transfer,
            investment = currencyTotals.investment,
            netBalance = currencyTotals.netBalance,
            transactionCount = currencyTotals.transactionCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilteredTotals()
    )
    
    private val _deletedTransaction = MutableStateFlow<TransactionEntity?>(null)
    val deletedTransaction: StateFlow<TransactionEntity?> = _deletedTransaction.asStateFlow()
    
    // Track if initial filters have been applied to prevent resetting on back navigation
    private var hasAppliedInitialFilters = false
    
    // Categories flow - will be used to map category names to colors
    val categories: StateFlow<Map<String, CategoryEntity>> = categoryRepository.getAllCategories()
        .map { categoryList ->
            categoryList.associateBy { it.name }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )
    
    // SMS scan period for info banner
    val smsScanMonths: StateFlow<Int> = userPreferencesRepository.smsScanMonths
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 3
        )
    
    fun isShowingLimitedData(): Boolean {
        val currentPeriod = _selectedPeriod.value
        val scanMonthsValue = smsScanMonths.value
        
        return when (currentPeriod) {
            TimePeriod.ALL -> true  // Always show for "All Time"
            TimePeriod.CURRENT_FY -> {
                // Check if FY start is before scan period
                val (fyStart, _) = getDateRangeForPeriod(TimePeriod.CURRENT_FY)
                val scanStart = LocalDate.now().minusMonths(scanMonthsValue.toLong())
                fyStart.isBefore(scanStart)
            }
            else -> false
        }
    }
    
    init {
        // Manually combine all flows using transformLatest
        merge(
            searchQuery.debounce(300).map { "search" },
            selectedPeriod.map { "period" },
            categoryFilter.map { "category" },
            transactionTypeFilter.map { "typeFilter" },
            selectedCurrency.map { "currency" },
            sortOption.map { "sort" }
        )
            .transformLatest { trigger ->
                // Get current values from all StateFlows
                val query = searchQuery.value
                val period = selectedPeriod.value
                val category = categoryFilter.value
                val typeFilter = transactionTypeFilter.value
                val currency = selectedCurrency.value
                val sort = sortOption.value

                // Get filtered transactions
                getFilteredTransactions(query, period, category, typeFilter)
                    .collect { transactions ->
                        // Filter by currency
                        val currencyFilteredTransactions = transactions.filter {
                            it.currency.equals(currency, ignoreCase = true)
                        }
                        emit(sortTransactions(currencyFilteredTransactions, sort))
                    }
            }
            .onEach { transactions ->
                // Calculate totals for filtered transactions
                _currencyGroupedTotals.value = calculateCurrencyGroupedTotals(transactions)

                // Auto-select primary currency if not already selected or if current currency no longer exists
                val currentCurrency = selectedCurrency.value
                if (!_currencyGroupedTotals.value.availableCurrencies.contains(currentCurrency) && _currencyGroupedTotals.value.hasAnyCurrency()) {
                    _selectedCurrency.value = _currencyGroupedTotals.value.getPrimaryCurrency()
                }
                
                // Calculate category distribution for selected currency
                val currencyTransactions = transactions.filter { it.currency == currentCurrency }
                val expenses = currencyTransactions.filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                val totalExpenses = expenses.sumOf { it.amount.abs() }
                
                val categoryData = if (totalExpenses > BigDecimal.ZERO) {
                    expenses
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
                } else {
                    emptyList()
                }
                
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    groupedTransactions = groupTransactionsByDate(transactions),
                    isLoading = false,
                    categoryDistributionData = categoryData
                )
            }
            .launchIn(viewModelScope)
    }
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectPeriod(period: TimePeriod) {
        _selectedPeriod.value = period
    }
    
    fun setCategoryFilter(category: String) {
        println("DEBUG: Setting category filter to: '$category'")
        _categoryFilter.value = category
    }
    
    fun clearCategoryFilter() {
        _categoryFilter.value = null
    }
    
    fun setTransactionTypeFilter(filter: TransactionTypeFilter) {
        _transactionTypeFilter.value = filter
    }
    
    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun selectCurrency(currency: String) {
        _selectedCurrency.value = currency
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

    fun resetFilters() {
        hasAppliedInitialFilters = false
        clearCategoryFilter()
        updateSearchQuery("")
        selectPeriod(TimePeriod.LAST_12_MONTHS)
        setTransactionTypeFilter(TransactionTypeFilter.ALL)
        setSortOption(SortOption.DATE_NEWEST)
        // Don't reset currency as it might be user preference
    }
    
    fun applyInitialFilters(
        category: String?,
        merchant: String?,
        period: String?,
        currency: String?,
        transactionType: String? = null
    ) {
        if (!hasAppliedInitialFilters) {
            // Only apply filters once, when first navigating to the screen
            clearCategoryFilter()
            updateSearchQuery("")
            selectPeriod(TimePeriod.LAST_12_MONTHS)
            setTransactionTypeFilter(TransactionTypeFilter.ALL)
            setSortOption(SortOption.DATE_NEWEST)

            transactionType?.let { typeName ->
                val typeFilter = when (typeName) {
                    "INCOME" -> TransactionTypeFilter.INCOME
                    "EXPENSE" -> TransactionTypeFilter.EXPENSE
                    "SPEND" -> TransactionTypeFilter.SPEND
                    "CREDIT" -> TransactionTypeFilter.CREDIT
                    "TRANSFER" -> TransactionTypeFilter.TRANSFER
                    "INVESTMENT" -> TransactionTypeFilter.INVESTMENT
                    else -> null
                }
                typeFilter?.let { setTransactionTypeFilter(it) }
            }

            category?.let {
                val decoded = if (it.contains("+") || it.contains("%")) {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } else it
                setCategoryFilter(decoded)
            }

            merchant?.let {
                val decoded = if (it.contains("+") || it.contains("%")) {
                    java.net.URLDecoder.decode(it, "UTF-8")
                } else it
                updateSearchQuery(decoded)
            }

            period?.let { periodName ->
                val timePeriod = when (periodName) {
                    "THIS_MONTH" -> TimePeriod.THIS_MONTH
                    "LAST_MONTH" -> TimePeriod.LAST_MONTH
                    "LAST_3_MONTHS" -> TimePeriod.LAST_3_MONTHS
                    "LAST_6_MONTHS" -> TimePeriod.LAST_6_MONTHS
                    "LAST_12_MONTHS" -> TimePeriod.LAST_12_MONTHS
                    "CURRENT_FY" -> TimePeriod.CURRENT_FY
                    "ALL" -> TimePeriod.ALL
                    else -> null
                }
                timePeriod?.let { selectPeriod(it) }
            }

            // Only set currency if it's provided (from navigation)
            currency?.let { selectCurrency(it) }

            hasAppliedInitialFilters = true
        }
    }

    fun applyNavigationFilters(
        category: String?,
        merchant: String?,
        period: String?,
        currency: String?,
        transactionType: String? = null
    ) {
        // This function can be called multiple times for navigation updates
        clearCategoryFilter()
        updateSearchQuery("")
        selectPeriod(TimePeriod.LAST_12_MONTHS)
        setTransactionTypeFilter(TransactionTypeFilter.ALL)
        setSortOption(SortOption.DATE_NEWEST)

        transactionType?.let { typeName ->
            val typeFilter = when (typeName) {
                "INCOME" -> TransactionTypeFilter.INCOME
                "EXPENSE" -> TransactionTypeFilter.EXPENSE
                "SPEND" -> TransactionTypeFilter.SPEND
                "CREDIT" -> TransactionTypeFilter.CREDIT
                "TRANSFER" -> TransactionTypeFilter.TRANSFER
                "INVESTMENT" -> TransactionTypeFilter.INVESTMENT
                else -> null
            }
            typeFilter?.let { setTransactionTypeFilter(it) }
        }

        category?.let {
            val decoded = if (it.contains("+") || it.contains("%")) {
                java.net.URLDecoder.decode(it, "UTF-8")
            } else it
            setCategoryFilter(decoded)
        }

        merchant?.let {
            val decoded = if (it.contains("+") || it.contains("%")) {
                java.net.URLDecoder.decode(it, "UTF-8")
            } else it
            updateSearchQuery(decoded)
        }

        period?.let { periodName ->
            val timePeriod = when (periodName) {
                "THIS_MONTH" -> TimePeriod.THIS_MONTH
                "LAST_MONTH" -> TimePeriod.LAST_MONTH
                "LAST_3_MONTHS" -> TimePeriod.LAST_3_MONTHS
                "LAST_6_MONTHS" -> TimePeriod.LAST_6_MONTHS
                "LAST_12_MONTHS" -> TimePeriod.LAST_12_MONTHS
                "CURRENT_FY" -> TimePeriod.CURRENT_FY
                "ALL" -> TimePeriod.ALL
                else -> null
            }
            timePeriod?.let { selectPeriod(it) }
        }

        // Only set currency if it's provided (from navigation)
        currency?.let { selectCurrency(it) }
    }
    
    private fun getFilteredTransactions(
        searchQuery: String,
        period: TimePeriod,
        category: String?,
        typeFilter: TransactionTypeFilter
    ): Flow<List<TransactionEntity>> {
        // Start with the base flow based on category filter
        val baseFlow = if (category != null) {
            println("DEBUG: Filtering by category: '$category'")
            transactionRepository.getTransactionsByCategory(category)
        } else {
            transactionRepository.getAllTransactions()
        }
        
        // Apply period filter
        val periodFilteredFlow = when (period) {
            TimePeriod.ALL -> baseFlow
            else -> {
                val (startDate, endDate) = getDateRangeForPeriod(period)
                val startDateTime = startDate.atStartOfDay()
                val endDateTime = endDate.atTime(23, 59, 59)
                
                baseFlow.map { transactions ->
                    transactions.filter { it.dateTime in startDateTime..endDateTime }
                }
            }
        }
        
        // Apply transaction type filter
        val typeFilteredFlow = periodFilteredFlow.map { transactions ->
            when (typeFilter) {
                TransactionTypeFilter.ALL -> transactions
                TransactionTypeFilter.INCOME -> transactions.filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) }
                TransactionTypeFilter.EXPENSE -> transactions.filter {
                    it.transactionType == TransactionType.EXPENSE && SpendingAnalyticsFilter.countsAsTrueSpending(it)
                }
                TransactionTypeFilter.SPEND -> transactions.filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                TransactionTypeFilter.CREDIT -> transactions.filter {
                    it.transactionType == TransactionType.CREDIT && SpendingAnalyticsFilter.countsAsTrueSpending(it)
                }
                TransactionTypeFilter.TRANSFER -> transactions.filter { it.transactionType == TransactionType.TRANSFER }
                TransactionTypeFilter.INVESTMENT -> transactions.filter { it.transactionType == TransactionType.INVESTMENT }
            }
        }
        
        // Apply search filter
        return if (searchQuery.isBlank()) {
            typeFilteredFlow
        } else {
            // Combine with merchant name mappings to check both original and normalized names
            combine(
                typeFilteredFlow,
                merchantNameMappingRepository.getAllMappings()
            ) { transactions, mappings ->
                val mappingMap = mappings.associateBy { it.originalName }
                // Find all original names that map to the search query (if it's a normalized name)
                val originalNamesMatchingQuery = mappings
                    .filter { it.normalizedName.contains(searchQuery, ignoreCase = true) }
                    .map { it.originalName }
                    .toSet()
                
                transactions.filter { transaction ->
                    // Check original merchant name
                    val matchesMerchant = transaction.merchantName.contains(searchQuery, ignoreCase = true)
                    
                    // Check normalized merchant name
                    val matchesNormalizedMerchant = transaction.normalizedMerchantName?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check if this transaction's original name maps to a normalized name that matches the query
                    val matchesMappedMerchant = transaction.merchantName in originalNamesMatchingQuery
                    
                    // Check if this transaction's original name has a mapping whose normalized name matches
                    val mapping = mappingMap[transaction.merchantName]
                    val matchesViaMapping = mapping?.normalizedName?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check description
                    val matchesDescription = transaction.description?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check SMS body (full text search)
                    val matchesSmsBody = transaction.smsBody?.contains(searchQuery, ignoreCase = true) == true
                    
                    // Check if search query matches amount
                    val matchesAmount = try {
                        // Remove commas and spaces from search query for number parsing
                        val cleanedQuery = searchQuery.replace(",", "").replace(" ", "").trim()
                        
                        // Check if it's a valid number and matches the amount
                        if (cleanedQuery.isNotEmpty() && cleanedQuery.all { it.isDigit() || it == '.' }) {
                            val amountString = transaction.amount.toPlainString()
                            // Support both exact and partial matches
                            amountString.contains(cleanedQuery) || 
                            // Also match formatted amount (e.g., "1,000" matches "1000")
                            amountString.replace(",", "").contains(cleanedQuery)
                        } else {
                            false
                        }
                    } catch (e: Exception) {
                        false
                    }
                    
                    matchesMerchant || matchesNormalizedMerchant || matchesMappedMerchant || matchesViaMapping || matchesDescription || matchesSmsBody || matchesAmount
                }
            }
        }
    }
    
    private fun sortTransactions(transactions: List<TransactionEntity>, sortOption: SortOption): List<TransactionEntity> {
        return when (sortOption) {
            SortOption.DATE_NEWEST -> transactions.sortedByDescending { it.dateTime }
            SortOption.DATE_OLDEST -> transactions.sortedBy { it.dateTime }
            SortOption.AMOUNT_HIGHEST -> transactions.sortedByDescending { it.amount }
            SortOption.AMOUNT_LOWEST -> transactions.sortedBy { it.amount }
            SortOption.MERCHANT_AZ -> transactions.sortedBy { it.merchantName.lowercase() }
            SortOption.MERCHANT_ZA -> transactions.sortedByDescending { it.merchantName.lowercase() }
        }
    }
    
    private fun groupTransactionsByDate(
        transactions: List<TransactionEntity>
    ): Map<DateGroup, List<TransactionEntity>> {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val weekStart = today.minusWeeks(1)
        
        return transactions.groupBy { transaction ->
            val transactionDate = transaction.dateTime.toLocalDate()
            when {
                transactionDate == today -> DateGroup.TODAY
                transactionDate == yesterday -> DateGroup.YESTERDAY
                transactionDate > weekStart -> DateGroup.THIS_WEEK
                else -> DateGroup.EARLIER
            }
        }
    }
    
    private fun calculateCurrencyGroupedTotals(transactions: List<TransactionEntity>): CurrencyGroupedTotals {
        // Group transactions by currency
        val transactionsByCurrency = transactions.groupBy { it.currency }

        val totalsByCurrency = transactionsByCurrency.mapValues { (currency, currencyTransactions) ->
            val income = currencyTransactions
                .filter { SpendingAnalyticsFilter.countsAsTrueIncome(it) }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()

            val expenses = currencyTransactions
                .filter { it.transactionType == TransactionType.EXPENSE && SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()

            val credit = currencyTransactions
                .filter { it.transactionType == TransactionType.CREDIT && SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()

            val transfer = currencyTransactions
                .filter { it.transactionType == TransactionType.TRANSFER }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()

            val investment = currencyTransactions
                .filter { it.transactionType == TransactionType.INVESTMENT }
                .sumOf { it.amount.toDouble() }
                .toBigDecimal()

            CurrencyTotals(
                currency = currency,
                income = income,
                expenses = expenses,
                credit = credit,
                transfer = transfer,
                investment = investment,
                transactionCount = currencyTransactions.size
            )
        }

        // Note: availableCurrencies are now provided by the separate availableCurrencies StateFlow
        // We'll keep the old behavior for compatibility but the UI should use availableCurrencies property
        val filteredAvailableCurrencies = totalsByCurrency.keys.toList().sortedWith { a, b ->
            when {
                a == "INR" -> -1 // INR first
                b == "INR" -> 1
                else -> a.compareTo(b) // Alphabetical for others
            }
        }

        return CurrencyGroupedTotals(
            totalsByCurrency = totalsByCurrency,
            availableCurrencies = filteredAvailableCurrencies,
            transactionCount = transactions.size
        )
    }
    
    fun getReportUrl(transaction: TransactionEntity): String {
        // If we have the original SMS body, create report URL
        val smsBody = transaction.smsBody ?: ""
        // Use the original SMS sender if available
        val sender = transaction.smsSender ?: ""
        
        // URL encode the parameters
        val encodedMessage = java.net.URLEncoder.encode(smsBody, "UTF-8")
        val encodedSender = java.net.URLEncoder.encode(sender, "UTF-8")
        
        // Encrypt device data for verification
        val encryptedDeviceData = com.anomapro.finndot.utils.DeviceEncryption.encryptDeviceData(context)
        val encodedDeviceData = if (encryptedDeviceData != null) {
            java.net.URLEncoder.encode(encryptedDeviceData, "UTF-8")
        } else {
            ""
        }
        
        // Create the report URL using hash fragment for privacy
        return "${Constants.Links.WEB_PARSER_URL}/#message=$encodedMessage&sender=$encodedSender&device=$encodedDeviceData&autoparse=true"
    }
    
    fun toggleRegretStatus(transactionId: Long, isRegret: Boolean) {
        viewModelScope.launch {
            try {
                transactionRepository.updateRegretStatus(transactionId, isRegret)
            } catch (e: Exception) {
                android.util.Log.e("TransactionsViewModel", "Error toggling regret status", e)
            }
        }
    }
    
}

data class TransactionsUiState(
    val transactions: List<TransactionEntity> = emptyList(),
    val groupedTransactions: Map<DateGroup, List<TransactionEntity>> = emptyMap(),
    val isLoading: Boolean = true,
    val categoryDistributionData: List<CategorySpendingData> = emptyList()
)

data class FilterParams(
    val query: String,
    val period: TimePeriod,
    val category: String?,
    val typeFilter: TransactionTypeFilter
)

enum class DateGroup(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    THIS_WEEK("This Week"),
    EARLIER("Earlier")
}

enum class SortOption(val label: String) {
    DATE_NEWEST("Newest First"),
    DATE_OLDEST("Oldest First"),
    AMOUNT_HIGHEST("Highest Amount"),
    AMOUNT_LOWEST("Lowest Amount"),
    MERCHANT_AZ("Merchant (A-Z)"),
    MERCHANT_ZA("Merchant (Z-A)")
}

data class FilteredTotals(
    val income: BigDecimal = BigDecimal.ZERO,
    val expenses: BigDecimal = BigDecimal.ZERO,
    val credit: BigDecimal = BigDecimal.ZERO,
    val transfer: BigDecimal = BigDecimal.ZERO,
    val investment: BigDecimal = BigDecimal.ZERO,
    val netBalance: BigDecimal = BigDecimal.ZERO,
    val transactionCount: Int = 0
)
