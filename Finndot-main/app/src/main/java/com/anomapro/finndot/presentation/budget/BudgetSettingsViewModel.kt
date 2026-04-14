package com.anomapro.finndot.presentation.budget

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anomapro.finndot.data.database.entity.BudgetEntity
import com.anomapro.finndot.data.database.entity.SubscriptionEntity
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.repository.BudgetRepository
import com.anomapro.finndot.data.repository.CategoryRepository
import com.anomapro.finndot.data.preferences.UserPreferencesRepository
import com.anomapro.finndot.data.repository.SubscriptionRepository
import com.anomapro.finndot.data.repository.TransactionRepository
import com.anomapro.finndot.domain.analytics.SpendingAnalyticsFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.math.BigDecimal
import com.anomapro.finndot.utils.CurrencyFormatter
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CategoryBudgetProgress(
    val category: String,
    val budgetAmount: BigDecimal,
    val spentAmount: BigDecimal,
    val remaining: BigDecimal
)

@HiltViewModel
class BudgetSettingsViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val subscriptionRepository: SubscriptionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(BudgetSettingsUiState())
    val uiState: StateFlow<BudgetSettingsUiState> = _uiState.asStateFlow()
    
    private val currentMonth = YearMonth.from(LocalDate.now())
    private val sharedPrefs = context.getSharedPreferences("account_prefs", Context.MODE_PRIVATE)

    private fun manualBudgetKey(yearMonth: YearMonth, currency: String): String =
        "manual_budget_${yearMonth}_$currency"

    private fun setManualBudget(yearMonth: YearMonth, currency: String, isManual: Boolean) {
        sharedPrefs.edit().putBoolean(manualBudgetKey(yearMonth, currency), isManual).apply()
    }
    
    init {
        loadBudgets()
    }
    
    private fun loadBudgets() {
        viewModelScope.launch {
            try {
                userPreferencesRepository.baseCurrency
                    .catch { emit("INR") }
                    .distinctUntilChanged()
                    .flatMapLatest { currency ->
                        combine(
                            budgetRepository.getBudgetsForMonth(currentMonth, currency)
                                .catch { e ->
                                    android.util.Log.e("BudgetSettingsViewModel", "Error in budget flow", e)
                                    emit(emptyList())
                                },
                            categoryRepository.getExpenseCategories()
                                .catch { e ->
                                    android.util.Log.e("BudgetSettingsViewModel", "Error in category flow", e)
                                    emit(emptyList())
                                },
                            subscriptionRepository.getSubscriptionsDueThisMonth(currentMonth, currency)
                                .catch { emit(emptyList()) }
                        ) { budgets, categories, subscriptions ->
                            Triple(currency, budgets, Pair(categories, subscriptions))
                        }
                    }
                    .collect { (currency, budgets, data) ->
                        val (categories, subscriptions) = data
                        try {
                            val totalBudget = budgets.find { it.category == "All" }
                            val categoryBudgets = budgets.filter { it.category != "All" }

                            // Load current month transactions for category spend and recurring
                            val startDate = currentMonth.atDay(1).atStartOfDay()
                            val endDate = LocalDate.now().atTime(23, 59, 59)
                            val transactions = try {
                                transactionRepository.getTransactionsBetweenDates(startDate, endDate).first()
                                    .filter { it.currency == currency }
                                    .filter { SpendingAnalyticsFilter.countsAsTrueSpending(it) }
                            } catch (e: Exception) {
                                emptyList()
                            }
                            val categorySpend = transactions
                                .filter { !it.category.isNullOrBlank() }
                                .groupBy { it.category!! }
                                .mapValues { (_, txs) -> txs.sumOf { it.amount.abs() } }
                            val totalSpent = transactions.sumOf { it.amount.abs() }
                            val recurringThisMonth = transactions.filter { it.isRecurring }
                            val recurringTotal = recurringThisMonth.sumOf { it.amount.abs() }

                            val effectiveBudget = totalBudget?.amount ?: BigDecimal.ZERO
                            val remaining = effectiveBudget - totalSpent
                            val daysElapsed = LocalDate.now().dayOfMonth.coerceAtLeast(1)
                            val daysInMonth = currentMonth.lengthOfMonth()
                            val daysLeft = (daysInMonth - daysElapsed).coerceAtLeast(0)

                            // Runway: at current daily spend rate, when will budget run out?
                            val dailySpendRate = if (daysElapsed > 0 && totalSpent > BigDecimal.ZERO) {
                                totalSpent / BigDecimal(daysElapsed)
                            } else BigDecimal.ZERO
                            val runwayDays = if (remaining > BigDecimal.ZERO && dailySpendRate > BigDecimal.ZERO) {
                                (remaining / dailySpendRate).toInt().coerceAtLeast(0)
                            } else null
                            val runwayMessage = when {
                                effectiveBudget <= BigDecimal.ZERO -> null
                                remaining <= BigDecimal.ZERO -> "Over budget by ${CurrencyFormatter.formatCurrency(remaining.abs(), currency)}"
                                runwayDays != null && runwayDays < daysLeft -> "At current pace, budget runs out in ~$runwayDays days"
                                else -> "On track — $daysLeft days left in month"
                            }

                            val categoryProgress = categoryBudgets.map { budget ->
                                val spent = categorySpend[budget.category] ?: BigDecimal.ZERO
                                CategoryBudgetProgress(
                                    category = budget.category,
                                    budgetAmount = budget.amount,
                                    spentAmount = spent,
                                    remaining = budget.amount - spent
                                )
                            }

                            _uiState.value = _uiState.value.copy(
                                currency = currency,
                                totalBudget = effectiveBudget,
                                categoryBudgets = categoryBudgets,
                                availableCategories = categories.map { it.name },
                                categoryProgress = categoryProgress,
                                totalSpentThisMonth = totalSpent,
                                subscriptionsThisMonth = subscriptions,
                                recurringPaymentsThisMonth = recurringThisMonth,
                                recurringTotalThisMonth = recurringTotal,
                                runwayMessage = runwayMessage,
                                runwayDays = runwayDays,
                                daysLeftInMonth = daysLeft,
                                isLoading = false
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("BudgetSettingsViewModel", "Error processing budget data", e)
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "Error loading budgets: ${e.message}"
                            )
                        }
                    }
            } catch (e: Exception) {
                android.util.Log.e("BudgetSettingsViewModel", "Error loading budgets", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load budgets: ${e.message}"
                )
            }
        }
    }
    
    fun setTotalBudget(amount: BigDecimal) {
        viewModelScope.launch {
            try {
                val currency = uiState.value.currency
                budgetRepository.createOrUpdateBudget(
                    category = "All",
                    amount = amount,
                    yearMonth = currentMonth,
                    currency = currency
                )
                setManualBudget(currentMonth, currency, amount > BigDecimal.ZERO)
                _uiState.value = _uiState.value.copy(
                    totalBudget = amount,
                    message = "Budget updated successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update budget: ${e.message}"
                )
            }
        }
    }
    
    fun setCategoryBudget(category: String, amount: BigDecimal) {
        viewModelScope.launch {
            try {
                val currency = uiState.value.currency
                budgetRepository.createOrUpdateBudget(
                    category = category,
                    amount = amount,
                    yearMonth = currentMonth,
                    currency = currency
                )
                _uiState.value = _uiState.value.copy(
                    message = "Category budget updated successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to update category budget: ${e.message}"
                )
            }
        }
    }
    
    fun deleteCategoryBudget(category: String) {
        viewModelScope.launch {
            try {
                val currency = uiState.value.currency
                budgetRepository.deleteBudgetForCategory(currentMonth, category, currency)
                _uiState.value = _uiState.value.copy(
                    message = "Category budget deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete category budget: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(
            message = null,
            error = null
        )
    }
}

data class BudgetSettingsUiState(
    val currency: String = "INR",
    val totalBudget: BigDecimal = BigDecimal.ZERO,
    val categoryBudgets: List<BudgetEntity> = emptyList(),
    val availableCategories: List<String> = emptyList(),
    val categoryProgress: List<CategoryBudgetProgress> = emptyList(),
    val totalSpentThisMonth: BigDecimal = BigDecimal.ZERO,
    val subscriptionsThisMonth: List<SubscriptionEntity> = emptyList(),
    val recurringPaymentsThisMonth: List<TransactionEntity> = emptyList(),
    val recurringTotalThisMonth: BigDecimal = BigDecimal.ZERO,
    val runwayMessage: String? = null,
    val runwayDays: Int? = null,
    val daysLeftInMonth: Int = 0,
    val isLoading: Boolean = true,
    val message: String? = null,
    val error: String? = null
)

