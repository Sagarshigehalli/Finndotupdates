package com.anomapro.finndot.presentation.merchants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import javax.inject.Inject

enum class MerchantSortType {
    AMOUNT,
    TRANSACTION_COUNT,
    NAME
}

enum class MerchantPeriodFilter(val months: Long?) {
    LAST_3_MONTHS(3),
    LAST_6_MONTHS(6),
    LAST_12_MONTHS(12),
    ALL(null)
}

@HiltViewModel
class MerchantsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val merchantNameMappingRepository: com.anomapro.finndot.data.repository.MerchantNameMappingRepository
) : ViewModel() {
    
    private val _sortType = MutableStateFlow(MerchantSortType.AMOUNT)
    val sortType: StateFlow<MerchantSortType> = _sortType.asStateFlow()

    private val _periodFilter = MutableStateFlow(MerchantPeriodFilter.LAST_12_MONTHS)
    val periodFilter: StateFlow<MerchantPeriodFilter> = _periodFilter.asStateFlow()
    
    private val _uiState = MutableStateFlow(MerchantsUiState())
    val uiState: StateFlow<MerchantsUiState> = _uiState.asStateFlow()
    
    init {
        loadMerchants()
    }
    
    fun setSortType(sortType: MerchantSortType) {
        _sortType.value = sortType
        loadMerchants()
    }

    fun setPeriodFilter(filter: MerchantPeriodFilter) {
        _periodFilter.value = filter
        loadMerchants()
    }
    
    private fun loadMerchants() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            // Combine flows to get both transactions and mappings
            combine(
                transactionRepository.getAllTransactions(),
                merchantNameMappingRepository.getAllMappings()
            ) { transactions, mappings ->
                val filteredTransactions = filterTransactionsByPeriod(transactions, _periodFilter.value)
                val mappingMap = mappings.associateBy { it.originalName }

                // Group transactions by normalized merchant name (or original if no mapping)
                val merchantMap = filteredTransactions
                    .groupBy { transaction ->
                        val normalizedName = transaction.normalizedMerchantName
                            ?: mappingMap[transaction.merchantName]?.normalizedName
                            ?: transaction.merchantName
                        normalizeMerchantGroup(normalizedName)
                    }
                
                // Calculate merchant statistics
                val merchantData = merchantMap.map { (merchantName, merchantTransactions) ->
                    val debits = merchantTransactions
                        .filter { 
                            it.transactionType == TransactionType.EXPENSE || 
                            it.transactionType == TransactionType.CREDIT
                        }
                        .sumOf { it.amount.abs() }
                    
                    val credits = merchantTransactions
                        .filter { it.transactionType == TransactionType.INCOME }
                        .sumOf { it.amount.abs() }
                    
                    val totalAmount = debits + credits
                    val netAmount = credits - debits
                    
                    MerchantInfo(
                        name = merchantName,
                        totalAmount = totalAmount,
                        debits = debits,
                        credits = credits,
                        netAmount = netAmount,
                        transactionCount = merchantTransactions.size,
                        transactions = merchantTransactions
                    )
                }
                
                // Sort merchants based on selected sort type; keep unknown group at the end
                val sortedMerchants = when (_sortType.value) {
                    MerchantSortType.AMOUNT -> merchantData.sortedWith(
                        compareBy<MerchantInfo> { isUnknownGroup(it.name) }
                            .thenByDescending { it.totalAmount }
                    )
                    MerchantSortType.TRANSACTION_COUNT -> merchantData.sortedWith(
                        compareBy<MerchantInfo> { isUnknownGroup(it.name) }
                            .thenByDescending { it.transactionCount }
                    )
                    MerchantSortType.NAME -> merchantData.sortedWith(
                        compareBy<MerchantInfo> { isUnknownGroup(it.name) }
                            .thenBy { it.name }
                    )
                }
                
                MerchantsUiState(
                    merchants = sortedMerchants,
                    isLoading = false
                )
            }.collect { uiState ->
                _uiState.value = uiState
            }
        }
    }
}

private fun normalizeMerchantGroup(merchantName: String): String {
    val normalized = merchantName.trim().lowercase()
    return if (normalized.isBlank() || normalized in setOf(
        "unknown",
        "unknown merchant",
        "na",
        "n/a",
        "not available",
        "null",
        "-"
    )) {
        "Unknown Merchant"
    } else {
        merchantName
    }
}

private fun isUnknownGroup(merchantName: String): Boolean {
    return merchantName.trim().lowercase() == "unknown merchant"
}

private fun filterTransactionsByPeriod(
    transactions: List<com.anomapro.finndot.data.database.entity.TransactionEntity>,
    filter: MerchantPeriodFilter
): List<com.anomapro.finndot.data.database.entity.TransactionEntity> {
    val months = filter.months ?: return transactions
    val startDate = java.time.LocalDate.now().minusMonths(months).atStartOfDay()
    return transactions.filter { it.dateTime >= startDate }
}

data class MerchantsUiState(
    val merchants: List<MerchantInfo> = emptyList(),
    val isLoading: Boolean = true
)

data class MerchantInfo(
    val name: String,
    val totalAmount: BigDecimal,
    val debits: BigDecimal,
    val credits: BigDecimal,
    val netAmount: BigDecimal,
    val transactionCount: Int,
    val transactions: List<com.anomapro.finndot.data.database.entity.TransactionEntity>
)
