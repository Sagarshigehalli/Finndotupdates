package com.anomapro.finndot.presentation.home

import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.work.WorkInfo
import com.anomapro.finndot.ui.components.SmsParsingProgressDialog
import kotlinx.coroutines.launch
import com.anomapro.finndot.data.database.entity.SubscriptionEntity
import com.anomapro.finndot.data.database.entity.TransactionEntity
import com.anomapro.finndot.data.database.entity.TransactionType
import com.anomapro.finndot.ui.components.BrandIcon
import com.anomapro.finndot.ui.theme.*
import com.anomapro.finndot.ui.theme.rememberContentPadding
import com.anomapro.finndot.ui.components.SummaryCard
import com.anomapro.finndot.ui.components.ListItemCard
import com.anomapro.finndot.ui.components.SectionHeader
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.components.BudgetCard
import com.anomapro.finndot.ui.components.AccountBalancesCard
import com.anomapro.finndot.ui.components.UnifiedAccountsCard
import com.anomapro.finndot.ui.components.SpotlightTutorial
import com.anomapro.finndot.ui.components.spotlightTarget
import com.anomapro.finndot.ui.components.SpendingTrendChart
import com.anomapro.finndot.ui.components.CategoryDistributionChart
import com.anomapro.finndot.ui.components.MonthlyComparisonChart
import com.anomapro.finndot.ui.components.DailySummaryCard
import com.anomapro.finndot.presentation.home.BudgetHeroCard
import com.anomapro.finndot.presentation.home.SetBudgetNudgeCard
import com.anomapro.finndot.presentation.home.IncomeVsExpenseBars
import com.anomapro.finndot.presentation.home.TopCategoriesBarChart
import com.anomapro.finndot.presentation.home.Last7DaysSparkline
import com.anomapro.finndot.presentation.home.HomeTimeRange
import com.anomapro.finndot.presentation.home.RegretDonutCard
import com.anomapro.finndot.presentation.home.UpcomingBillsTimeline
import com.anomapro.finndot.presentation.home.UpcomingBillItem
import com.anomapro.finndot.utils.CurrencyFormatter
import com.anomapro.finndot.utils.formatAmount
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    navController: NavController,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToFilteredTransactions: (category: String?, transactionType: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToTransactionsWithSearch: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToRecurringCommitments: () -> Unit = {},
    onNavigateToAddScreen: () -> Unit = {},
    onNavigateToBudgetSettings: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onFabPositioned: (Rect) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val deletedTransaction by viewModel.deletedTransaction.collectAsState()
    val smsScanWorkInfo by viewModel.smsScanWorkInfo.collectAsState()
    val activity = LocalActivity.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Currency dropdown state
      
    // Check for app updates and reviews when the screen is first displayed
    LaunchedEffect(Unit) {
        // Refresh account balances to ensure proper currency conversion
        viewModel.refreshAccountBalances()

        activity?.let {
            val componentActivity = it as ComponentActivity
            
            // Check for app updates
            viewModel.checkForAppUpdate(
                activity = componentActivity,
                snackbarHostState = snackbarHostState,
                scope = scope
            )
            
            // Check for in-app review eligibility
            viewModel.checkForInAppReview(componentActivity)
        }
    }
    
    // Refresh hidden accounts whenever this screen becomes visible
    // This ensures changes from ManageAccountsScreen are reflected immediately
    DisposableEffect(Unit) {
        viewModel.refreshHiddenAccounts()
        onDispose { }
    }
    
    // Refresh budget data when returning from budget settings
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // Check if we just returned from budget settings
        val currentRoute = navBackStackEntry?.destination?.route
        if (currentRoute == "home") {
            viewModel.refreshBudgetData()
        }
    }
    
    // Handle delete undo snackbar
    LaunchedEffect(deletedTransaction) {
        deletedTransaction?.let { transaction ->
            // Clear the state immediately to prevent re-triggering
            viewModel.clearDeletedTransaction()
            
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Transaction deleted",
                    actionLabel = "Undo",
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    // Pass the transaction directly since state is already cleared
                    viewModel.undoDeleteTransaction(transaction)
                }
            }
        }
    }
    
    // Clear snackbar when navigating away
    DisposableEffect(Unit) {
        onDispose {
            snackbarHostState.currentSnackbarData?.dismiss()
        }
    }
    
    val contentPadding = rememberContentPadding()
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)) {
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshHomeData() },
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = contentPadding,
                end = contentPadding,
                top = contentPadding,
                bottom = contentPadding + 80.dp // Space for FAB
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Currency selector at top (if multiple)
            if (uiState.availableCurrencies.size > 1) {
                item {
                    EnhancedCurrencySelector(
                        selectedCurrency = uiState.selectedCurrency,
                        availableCurrencies = uiState.availableCurrencies,
                        onCurrencySelected = { viewModel.selectCurrency(it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Income & Spending Trend — heading directly above chart
            if (uiState.monthlySpendingData.isNotEmpty()) {
                item {
                    HomeSectionLabel(
                        title = "Income & Spending Trend",
                        subtitle = null,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                }
                item {
                    SpendingTrendChart(
                        spendingData = uiState.monthlySpendingData,
                        currency = uiState.selectedCurrency,
                        modifier = Modifier.fillMaxWidth(),
                        height = 220,
                        showTitle = false
                    )
                }
            }

            // Income vs Spent — heading directly above chart
            item {
                HomeSectionLabel(
                    title = "Income vs Spent",
                    subtitle = null,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
            item {
                val periodStr = when (uiState.selectedTimeRange) {
                    HomeTimeRange.MONTHS_3 -> "LAST_3_MONTHS"
                    HomeTimeRange.MONTHS_6 -> "LAST_6_MONTHS"
                    HomeTimeRange.MONTHS_12 -> "LAST_12_MONTHS"
                }
                IncomeVsExpenseBars(
                    income = uiState.incomeLast12Months,
                    expenses = uiState.spendLast12Months,
                    currency = uiState.selectedCurrency,
                    showTitle = false,
                    onIncomeClick = { onNavigateToFilteredTransactions(null, "INCOME", periodStr, uiState.selectedCurrency) },
                    onSpendClick = { onNavigateToFilteredTransactions(null, "SPEND", periodStr, uiState.selectedCurrency) }
                )
            }

            // Top Categories — heading directly above chart
            if (uiState.categoryDistributionData.isNotEmpty()) {
                item {
                    HomeSectionLabel(
                        title = "Top Categories",
                        subtitle = null,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                }
                item {
                    val periodStr = when (uiState.selectedTimeRange) {
                        HomeTimeRange.MONTHS_3 -> "LAST_3_MONTHS"
                        HomeTimeRange.MONTHS_6 -> "LAST_6_MONTHS"
                        HomeTimeRange.MONTHS_12 -> "LAST_12_MONTHS"
                    }
                    TopCategoriesBarChart(
                        categories = uiState.categoryDistributionData,
                        currency = uiState.selectedCurrency,
                        showTitle = false,
                        onCategoryClick = { category ->
                            onNavigateToFilteredTransactions(
                                category,
                                "EXPENSE",
                                periodStr,
                                uiState.selectedCurrency
                            )
                        }
                    )
                }
            }

            // Time range filter — below charts, applies to trend, income vs spent, top categories
            item {
                TimeRangeFilterRow(
                    selected = uiState.selectedTimeRange,
                    onSelect = { viewModel.selectTimeRange(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recurring & upcoming monthly commitments
            item {
                HomeSectionLabel(
                    title = "Recurring & upcoming monthly commitments",
                    subtitle = null,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
            item {
                UpcomingMonthlyCommitmentsCard(
                    totalPredicted = uiState.recurringBillsPredictedTotalThisMonth,
                    expectedCount = uiState.recurringBillsExpectedCountThisMonth,
                    nextMerchant = uiState.recurringBillsNextMerchant,
                    nextAmount = uiState.recurringBillsNextAmount,
                    nextDaysUntilDue = uiState.recurringBillsNextDaysUntilDue,
                    progress = uiState.recurringBillsProgressThisMonth,
                    currency = uiState.selectedCurrency,
                    intervalRecurringCount = uiState.intervalRecurringCount,
                    intervalNextMerchant = uiState.intervalRecurringNextMerchant,
                    intervalNextAmount = uiState.intervalRecurringNextAmount,
                    intervalNextDaysUntilDue = uiState.intervalRecurringNextDaysUntilDue,
                    onClick = onNavigateToRecurringCommitments
                )
            }
            
            // Budget — heading directly above content
            item {
                HomeSectionLabel(
                    title = "Budget",
                    subtitle = "Based on your spending, edit anytime",
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }

            if (uiState.monthlyBudget > BigDecimal.ZERO) {
                item {
                    BudgetCard(
                        budget = uiState.monthlyBudget,
                        actual = uiState.monthlyActualSpending,
                        remaining = uiState.monthlyBudgetRemaining,
                        percentageUsed = uiState.monthlyBudgetPercentageUsed,
                        budgetScore = uiState.budgetScore,
                        currency = uiState.selectedCurrency,
                        onClick = onNavigateToBudgetSettings
                    )
                }
            } else {
                item {
                    SetBudgetNudgeCard(
                        recommendedBudget = uiState.recommendedMonthlyBudget,
                        currency = uiState.selectedCurrency,
                        onClick = onNavigateToBudgetSettings
                    )
                }
            }
            
            item {
                FilledTonalButton(
                    onClick = onNavigateToBudgetSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(Dimensions.CornerRadius.large)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.xs))
                    Text(if (uiState.hasCustomBudget) "Edit Budget" else "Set Budget")
                }
            }
            
            // Today — heading directly above summary
            item {
                HomeSectionLabel(
                    title = "Today",
                    subtitle = null,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
            item {
                DailySummaryCard(
                    dailySummary = uiState.dailySummary,
                    modifier = Modifier.fillMaxWidth(),
                    showTitle = false
                )
            }

            // Last 7 days — heading directly above chart
            if (uiState.last7DaysDailySpend.size == 7) {
                item {
                    HomeSectionLabel(
                        title = "Last 7 Days",
                        subtitle = null,
                        modifier = Modifier.padding(bottom = Spacing.xs)
                    )
                }
                item {
                    Last7DaysSparkline(
                        dailySpend = uiState.last7DaysDailySpend,
                        dailyBudget = uiState.dailyBudgetAmount,
                        currency = uiState.selectedCurrency,
                        showTitle = false
                    )
                }
            }
            
            // Recent Transactions Section
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    action = {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search button
                            IconButton(
                                onClick = onNavigateToTransactionsWithSearch,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search transactions",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // View All button
                            TextButton(onClick = onNavigateToTransactions) {
                                Text("View All")
                            }
                        }
                    }
                )
            }
            
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Dimensions.Component.minTouchTarget * 2),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            CircularProgressIndicator()
                            uiState.loadingMessage?.let { message ->
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            } else if (uiState.recentTransactions.isEmpty()) {
                item {
                    FinndotCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Dimensions.Padding.empty),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                OutlinedButton(onClick = { viewModel.scanSmsMessages() }) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Sync SMS")
                                }
                                Button(onClick = onNavigateToAddScreen) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(Spacing.xs))
                                    Text("Add")
                                }
                            }
                        }
                    }
                }
            } else {
                items(
                    items = uiState.recentTransactions,
                    key = { it.id }
                ) { transaction ->
                    SimpleTransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) },
                        onRegretClick = { transactionId, isRegret ->
                            scope.launch {
                                viewModel.toggleRegretStatus(transactionId, isRegret)
                            }
                        }
                    )
                }
            }

            // Account overview at the end
            if (uiState.creditCards.isNotEmpty() || uiState.accountBalances.isNotEmpty()) {
                item {
                    HomeSectionLabel(
                        title = "Account Overview",
                        subtitle = "Balances and credit in one place"
                    )
                }
                item {
                    UnifiedAccountsCard(
                        creditCards = uiState.creditCards,
                        bankAccounts = uiState.accountBalances,
                        totalBalance = uiState.totalBalance,
                        totalAvailableCredit = uiState.totalAvailableCredit,
                        averageIncome = uiState.averageIncome12Months,
                        selectedCurrency = uiState.selectedCurrency,
                        onAccountClick = { bankName, accountLast4 ->
                            navController.navigate(
                                com.anomapro.finndot.navigation.AccountDetail(
                                    bankName = bankName,
                                    accountLast4 = accountLast4
                                )
                            )
                        }
                    )
                }
            }
        }
        }
        
        // FABs - Direct access (no speed dial)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Add FAB (top, small)
            SmallFloatingActionButton(
                onClick = onNavigateToAddScreen,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { contentDescription = "Add transaction or subscription" }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Transaction or Subscription"
                )
            }
            
            // Sync FAB (bottom, primary)
            FloatingActionButton(
                onClick = { viewModel.scanSmsMessages() },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .spotlightTarget(onFabPositioned)
                    .semantics { contentDescription = "Sync SMS to import transactions" }
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sync SMS"
                )
            }
        }
        
        // SMS Parsing Progress Dialog
        SmsParsingProgressDialog(
            isVisible = uiState.isScanning,
            workInfo = smsScanWorkInfo,
            onDismiss = { viewModel.cancelSmsScan() },
            onCancel = { viewModel.cancelSmsScan() }
        )
        
    }
    }
}

@Composable
private fun TimeRangeFilterRow(
    selected: HomeTimeRange,
    onSelect: (HomeTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(vertical = Spacing.xs),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        HomeTimeRange.entries.forEach { range ->
            FilterChip(
                selected = selected == range,
                onClick = { onSelect(range) },
                label = { Text(range.label) }
            )
        }
    }
}

@Composable
private fun HomeSectionLabel(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        subtitle?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UpcomingMonthlyCommitmentsCard(
    totalPredicted: BigDecimal,
    expectedCount: Int,
    nextMerchant: String?,
    nextAmount: BigDecimal?,
    nextDaysUntilDue: Int?,
    progress: Float,
    currency: String,
    intervalRecurringCount: Int,
    intervalNextMerchant: String?,
    intervalNextAmount: BigDecimal?,
    intervalNextDaysUntilDue: Int?,
    onClick: () -> Unit,
) {
    val nextLine = if (nextMerchant != null && nextAmount != null && nextDaysUntilDue != null) {
        val dayLabel = when {
            nextDaysUntilDue < 0 -> "${-nextDaysUntilDue} days overdue"
            nextDaysUntilDue == 0 -> "today"
            nextDaysUntilDue == 1 -> "in 1 day"
            else -> "in $nextDaysUntilDue days"
        }
        "Next: $nextMerchant ${CurrencyFormatter.formatCurrency(nextAmount, currency)} $dayLabel"
    } else {
        "No recurring monthly pattern detected yet"
    }
    val subtitle = when {
        expectedCount == 0 -> "None due this month"
        expectedCount == 1 -> "1 bill expected this month"
        else -> "$expectedCount bills expected this month"
    }

    val intervalNextLine = if (
        intervalNextMerchant != null &&
        intervalNextAmount != null &&
        intervalNextDaysUntilDue != null
    ) {
        val dayLabel = when {
            intervalNextDaysUntilDue < 0 -> "${-intervalNextDaysUntilDue} days overdue"
            intervalNextDaysUntilDue == 0 -> "today"
            intervalNextDaysUntilDue == 1 -> "in 1 day"
            else -> "in $intervalNextDaysUntilDue days"
        }
        "Next: $intervalNextMerchant ${CurrencyFormatter.formatCurrency(intervalNextAmount, currency)} $dayLabel"
    } else {
        "Open for full list"
    }
    val intervalCountLabel = when {
        intervalRecurringCount <= 0 -> null
        intervalRecurringCount == 1 -> "1 longer-cycle bill"
        else -> "$intervalRecurringCount longer-cycle bills"
    }

    FinndotCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (expectedCount > 0) {
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Text(
                text = "Monthly pattern (this month)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = CurrencyFormatter.formatCurrency(totalPredicted, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = nextLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )

            if (intervalRecurringCount > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.xs))
                Text(
                    text = "Longer-cycle recurring",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                intervalCountLabel?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = intervalNextLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTransactionItem(
    transaction: TransactionEntity,
    onClick: () -> Unit = {},
    onRegretClick: ((Long, Boolean) -> Unit)? = null
) {
    val amountColor = when (transaction.transactionType) {
        TransactionType.INCOME -> if (!isSystemInDarkTheme()) income_light else income_dark
        TransactionType.EXPENSE -> if (!isSystemInDarkTheme()) expense_light else expense_dark
        TransactionType.CREDIT -> if (!isSystemInDarkTheme()) credit_light else credit_dark
        TransactionType.TRANSFER -> if (!isSystemInDarkTheme()) transfer_light else transfer_dark
        TransactionType.INVESTMENT -> if (!isSystemInDarkTheme()) investment_light else investment_dark
    }
    
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d • h:mm a")
    val dateTimeText = transaction.dateTime.format(dateTimeFormatter)
    
    ListItemCard(
        title = transaction.merchantName,
        subtitle = dateTimeText,
        amount = transaction.formatAmount(),
        amountColor = amountColor,
        onClick = onClick,
        leadingContent = {
            BrandIcon(
                merchantName = transaction.merchantName,
                size = 40.dp,
                showBackground = true
            )
        },
        trailingContent = if (onRegretClick != null && transaction.transactionType in listOf(TransactionType.EXPENSE, TransactionType.CREDIT)) {
            {
                IconButton(
                    onClick = { onRegretClick(transaction.id, !transaction.isRegret) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (transaction.isRegret) Icons.Default.SentimentVeryDissatisfied else Icons.Default.SentimentSatisfied,
                        contentDescription = if (transaction.isRegret) "Mark as not regret" else "Mark as regret",
                        tint = if (transaction.isRegret) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
private fun MonthSummaryCard(
    monthTotal: BigDecimal,
    monthlyChange: BigDecimal,
    monthlyChangePercent: Int,
    currency: String,
    currentExpenses: BigDecimal = BigDecimal.ZERO,
    lastExpenses: BigDecimal = BigDecimal.ZERO,
    onShowBreakdown: () -> Unit = {}
) {
    val isPositive = monthTotal >= BigDecimal.ZERO
    val displayAmount = if (isPositive) {
        "+${CurrencyFormatter.formatCurrency(monthTotal, currency)}"
    } else {
        CurrencyFormatter.formatCurrency(monthTotal, currency)
    }
    val amountColor = if (isPositive) {
        if (!isSystemInDarkTheme()) income_light else income_dark
    } else {
        if (!isSystemInDarkTheme()) expense_light else expense_dark
    }
    
    val expenseChange = currentExpenses - lastExpenses
    val now = LocalDate.now()
    val lastMonth = now.minusMonths(1)
    val periodLabel = "vs ${lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    
    val subtitle = when {
        // No transactions yet
        currentExpenses == BigDecimal.ZERO && lastExpenses == BigDecimal.ZERO -> {
            "No transactions yet"
        }
        // Spent more than last period
        expenseChange > BigDecimal.ZERO -> {
            "😟 Spent ${CurrencyFormatter.formatCurrency(expenseChange.abs(), currency)} more $periodLabel"
        }
        // Spent less than last period
        expenseChange < BigDecimal.ZERO -> {
            "😊 Spent ${CurrencyFormatter.formatCurrency(expenseChange.abs(), currency)} less $periodLabel"
        }
        // Saved more (higher positive balance)
        monthlyChange > BigDecimal.ZERO && monthTotal > BigDecimal.ZERO -> {
            "🎉 Saved ${CurrencyFormatter.formatCurrency(monthlyChange.abs(), currency)} more $periodLabel"
        }
        // No change
        else -> {
            "Same as last period"
        }
    }
    
    val currentMonth = now.month.name.lowercase().replaceFirstChar { it.uppercase() }

    // Currency symbol mapping for display
    val currencySymbols = mapOf(
        "INR" to "₹",
        "USD" to "$",
        "AED" to "AED",
        "NPR" to "₨",
        "ETB" to "ብর"
    )
    val currencySymbol = currencySymbols[currency] ?: currency

    val titleText = "Overspending ($currencySymbol) • $currentMonth 1-${now.dayOfMonth}"
    
    SummaryCard(
        title = titleText,
        amount = displayAmount,
        subtitle = subtitle,
        amountColor = amountColor,
        onClick = onShowBreakdown
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BreakdownDialog(
    currentMonthIncome: BigDecimal,
    currentMonthExpenses: BigDecimal,
    currentMonthTotal: BigDecimal,
    lastMonthIncome: BigDecimal,
    lastMonthExpenses: BigDecimal,
    lastMonthTotal: BigDecimal,
    onDismiss: () -> Unit
) {
    val now = LocalDate.now()
    val currentPeriod = "${now.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    val lastMonth = now.minusMonths(1)
    val lastPeriod = "${lastMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} 1-${now.dayOfMonth}"
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md), // Reduced horizontal padding for wider modal
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Title
                Text(
                    text = "Calculation Breakdown",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Current Period Section
                Text(
                    text = currentPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                BreakdownRow(
                    label = "Income",
                    amount = currentMonthIncome,
                    isIncome = true
                )
                
                BreakdownRow(
                    label = "Expenses",
                    amount = currentMonthExpenses,
                    isIncome = false
                )
                
                HorizontalDivider()
                
                BreakdownRow(
                    label = "Net Balance",
                    amount = currentMonthTotal,
                    isIncome = currentMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                
                // Last Period Section
                Text(
                    text = lastPeriod,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                BreakdownRow(
                    label = "Income",
                    amount = lastMonthIncome,
                    isIncome = true
                )
                
                BreakdownRow(
                    label = "Expenses",
                    amount = lastMonthExpenses,
                    isIncome = false
                )
                
                HorizontalDivider()
                
                BreakdownRow(
                    label = "Net Balance",
                    amount = lastMonthTotal,
                    isIncome = lastMonthTotal >= BigDecimal.ZERO,
                    isBold = true
                )
                
                // Formula explanation
                Spacer(modifier = Modifier.height(Spacing.sm))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Formula: Income - Expenses = Net Balance\n" +
                               "Green (+) = Savings | Red (-) = Overspending",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(Spacing.sm),
                        textAlign = TextAlign.Center
                    )
                }
                
                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    amount: BigDecimal,
    isIncome: Boolean,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            text = "${if (isIncome) "+" else "-"}${CurrencyFormatter.formatCurrency(amount.abs())}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isIncome) {
                if (!isSystemInDarkTheme()) income_light else income_dark
            } else {
                if (!isSystemInDarkTheme()) expense_light else expense_dark
            }
        )
    }
}

@Composable
private fun UpcomingSubscriptionsCard(
    subscriptions: List<SubscriptionEntity>,
    totalAmount: BigDecimal,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(Dimensions.Icon.medium)
                )
                Column {
                    Text(
                        text = "${subscriptions.size} active subscriptions",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "Monthly total: ${CurrencyFormatter.formatCurrency(totalAmount)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = Dimensions.Alpha.subtitle)
                    )
                }
            }
            Text(
                text = "View",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionSummaryCards(
    uiState: HomeUiState,
    onCurrencySelected: (String) -> Unit = {},
    onShowBreakdown: () -> Unit = {}
) {
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = Spacing.md
        ) { page ->
            when (page) {
                0 -> {
                    // Net Balance Card (existing implementation)
                    MonthSummaryCard(
                        monthTotal = uiState.currentMonthTotal,
                        monthlyChange = uiState.monthlyChange,
                        monthlyChangePercent = uiState.monthlyChangePercent,
                        currency = uiState.selectedCurrency,
                        currentExpenses = uiState.currentMonthExpenses,
                        lastExpenses = uiState.lastMonthExpenses,
                        onShowBreakdown = onShowBreakdown
                    )
                }
                1 -> {
                    // Credit Card Summary
                    TransactionTypeCard(
                        title = "Credit Card",
                        amount = uiState.currentMonthCreditCard,
                        color = if (!isSystemInDarkTheme()) credit_light else credit_dark,
                        emoji = "💳",
                        currency = uiState.selectedCurrency
                    )
                }
                2 -> {
                    // Transfer Summary
                    TransactionTypeCard(
                        title = "Transfers",
                        amount = uiState.currentMonthTransfer,
                        color = if (!isSystemInDarkTheme()) transfer_light else transfer_dark,
                        emoji = "↔️",
                        currency = uiState.selectedCurrency
                    )
                }
                3 -> {
                    // Investment Summary
                    TransactionTypeCard(
                        title = "Investments",
                        amount = uiState.currentMonthInvestment,
                        color = if (!isSystemInDarkTheme()) investment_light else investment_dark,
                        emoji = "📈",
                        currency = uiState.selectedCurrency
                    )
                }
            }
        }
        
        // Page Indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.xs),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(4) { index ->
                val color = if (pagerState.currentPage == index) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                }
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(8.dp)
                        .background(
                            color = color,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeCard(
    title: String,
    amount: BigDecimal,
    color: Color,
    emoji: String,
    currency: String
) {
    val currentMonth = LocalDate.now().month.name.lowercase().replaceFirstChar { it.uppercase() }
    
    val subtitle = when {
        amount > BigDecimal.ZERO -> {
            when (title) {
                "Credit Card" -> "Spent on credit this month"
                "Transfers" -> "Moved between accounts"
                "Investments" -> "Invested this month"
                else -> "Total this month"
            }
        }
        else -> {
            when (title) {
                "Credit Card" -> "No credit card spending"
                "Transfers" -> "No transfers this month"
                "Investments" -> "No investments this month"
                else -> "No transactions"
            }
        }
    }
    
    SummaryCard(
        title = "$emoji $title • $currentMonth",
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(amount, currency),
        amountColor = color,
        onClick = { /* TODO: Navigate to filtered view */ }
    )
}

@Composable
private fun EnhancedCurrencySelector(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Currency symbol mapping
    val currencySymbols = mapOf(
        "INR" to "₹",
        "USD" to "$",
        "AED" to "AED",
        "NPR" to "₨",
        "ETB" to "ብር"
    )

    // Compact segmented button style
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xs, vertical = Spacing.xs),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableCurrencies.forEach { currency ->
                val isSelected = selectedCurrency == currency
                val symbol = currencySymbols[currency] ?: currency

                Surface(
                    onClick = { onCurrencySelected(currency) },
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize(),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.sm, horizontal = Spacing.xs),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        if (symbol != currency) {
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = currency,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

