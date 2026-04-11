package com.anomapro.finndot.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.presentation.common.TimePeriod
import com.anomapro.finndot.presentation.common.TransactionTypeFilter
import com.anomapro.finndot.ui.components.*
import com.anomapro.finndot.ui.icons.CategoryMapping
import com.anomapro.finndot.data.repository.ModelState
import com.anomapro.finndot.ui.theme.*
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    onNavigateToChat: () -> Unit = {},
    onNavigateToTransactions: (category: String?, merchant: String?, period: String?, currency: String?) -> Unit = { _, _, _, _ -> },
    onNavigateToSubscriptions: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    val transactionTypeFilter by viewModel.transactionTypeFilter.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val availableCurrencies by viewModel.availableCurrencies.collectAsStateWithLifecycle()
    val modelDownloaded by viewModel.modelDownloaded.collectAsStateWithLifecycle()
    val modelState by viewModel.modelState.collectAsStateWithLifecycle()
    val aiAdviceState by viewModel.aiAdviceState.collectAsStateWithLifecycle()
    var showAdvancedFilters by remember { mutableStateOf(false) }
    
    // Calculate active filter count
    val activeFilterCount = if (transactionTypeFilter != TransactionTypeFilter.EXPENSE) 1 else 0
    
    Box(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = Dimensions.Padding.content,
            end = Dimensions.Padding.content,
            top = Spacing.md,
            bottom = Dimensions.Component.bottomBarHeight + Spacing.md
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        // Period Selector - Always visible
        item {
            Text(
                text = "Time range",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = Spacing.xs)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                LazyRow(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(TimePeriod.values().toList()) { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { viewModel.selectPeriod(period) },
                            label = { Text(period.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
                IconButton(
                    onClick = { viewModel.refresh() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh analytics"
                    )
                }
            }
        }

        // Currency Selector (if multiple currencies available)
        if (availableCurrencies.size > 1) {
            item {
                CurrencyFilterRow(
                    selectedCurrency = selectedCurrency,
                    availableCurrencies = availableCurrencies,
                    onCurrencySelected = { currency -> viewModel.selectCurrency(currency) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Collapsible Transaction Type Filter
        item {
            CollapsibleFilterRow(
                isExpanded = showAdvancedFilters,
                activeFilterCount = activeFilterCount,
                onToggle = { showAdvancedFilters = !showAdvancedFilters },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = "Filter by type",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                    items(TransactionTypeFilter.values().toList()) { typeFilter ->
                        FilterChip(
                            selected = transactionTypeFilter == typeFilter,
                            onClick = { viewModel.setTransactionTypeFilter(typeFilter) },
                            label = { Text(typeFilter.label) },
                            leadingIcon = if (transactionTypeFilter == typeFilter) {
                                {
                                    when (typeFilter) {
                                        TransactionTypeFilter.INCOME -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.EXPENSE -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.SPEND -> Icon(
                                            Icons.AutoMirrored.Filled.TrendingDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.CREDIT -> Icon(
                                            Icons.Default.CreditCard,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.TRANSFER -> Icon(
                                            Icons.Default.SwapHoriz,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        TransactionTypeFilter.INVESTMENT -> Icon(
                                            Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            modifier = Modifier.size(Dimensions.Icon.small)
                                        )
                                        else -> null
                                    }
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        )
                    }
                }
            }
        }
        }
        
        // Overview: Investments & FinnDot Score
        item {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs, bottom = Spacing.xs)
            )
        }
        // Investments & FinnDot Score row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                InvestmentChart(
                    totalInvestment = uiState.totalInvestment12Months,
                    currency = uiState.currency,
                    modifier = Modifier.weight(1f)
                )
                FinnDotScoreChart(
                    score = uiState.finnDotScore,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Loading state when no data yet
        if (uiState.isLoading && uiState.transactionCount == 0) {
            item {
                FinndotCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = "Loading analytics…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Empty state: no transactions in selected period
        if (!uiState.isLoading && uiState.transactionCount == 0) {
            item {
                EmptyAnalyticsState(
                    onViewTransactions = {
                        onNavigateToTransactions(null, null, selectedPeriod.name, selectedCurrency)
                    }
                )
            }
        }

        // Analytics Summary Card
        if (uiState.totalSpending > BigDecimal.ZERO || uiState.transactionCount > 0) {
            item {
                AnalyticsSummaryCard(
                    totalAmount = uiState.totalSpending,
                    transactionCount = uiState.transactionCount,
                    averageAmount = uiState.averageAmount,
                    topCategory = uiState.topCategory,
                    topCategoryPercentage = uiState.topCategoryPercentage,
                    currency = uiState.currency,
                    isLoading = uiState.isLoading
                )
            }
        }

        // Income vs expenses – see what's left (helps tight budgets)
        if ((uiState.periodIncome > BigDecimal.ZERO || uiState.periodExpenses > BigDecimal.ZERO) && uiState.transactionCount > 0) {
            item {
                IncomeVsExpensesCard(
                    income = uiState.periodIncome,
                    expenses = uiState.periodExpenses,
                    remaining = uiState.periodRemaining,
                    runwayDays = uiState.runwayDays,
                    currency = uiState.currency
                )
            }
        }

        // Improvement streak banner
        if (uiState.improvementStreak >= 1 && uiState.transactionCount > 0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Spending down ${uiState.improvementStreak} month${if (uiState.improvementStreak > 1) "s" else ""} in a row. Keep it up!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Spending Trend Chart (multi-month)
        if (uiState.monthlyTrendData.size > 1 && transactionTypeFilter == TransactionTypeFilter.EXPENSE) {
            item {
                SpendingTrendChart(
                    spendingData = uiState.monthlyTrendData,
                    currency = uiState.currency,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Category Distribution (Donut Chart)
        if (uiState.categoryDistributionData.isNotEmpty()) {
            item {
                CategoryDistributionChart(
                    categoryData = uiState.categoryDistributionData,
                    currency = uiState.currency,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        // Category Breakdown Section
        if (uiState.categoryBreakdown.isNotEmpty()) {
            item {
                CategoryBreakdownCard(
                    categories = uiState.categoryBreakdown,
                    currency = selectedCurrency,
                    onCategoryClick = { category ->
                        onNavigateToTransactions(category.name, null, selectedPeriod.name, selectedCurrency)
                    }
                )
            }
        }
        
        // Section divider before insights
        if (uiState.quickWins.isNotEmpty() || uiState.savingsInsights.isNotEmpty()) {
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.xs),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }
            item {
                Text(
                    text = "Insights",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Spacing.xs)
                )
            }
        }
        
        // Quick Wins - concrete actions
        if (uiState.quickWins.isNotEmpty()) {
            item {
                QuickWinsCard(
                    quickWins = uiState.quickWins,
                    currency = uiState.currency,
                    cutAmountToMatch = uiState.cutAmountToMatch,
                    onCategoryClick = { cat -> onNavigateToTransactions(cat, null, selectedPeriod.name, selectedCurrency) },
                    onMerchantClick = { m -> onNavigateToTransactions(null, m, selectedPeriod.name, selectedCurrency) }
                )
            }
        }

        // Where Your Money Is Leaking
        if (uiState.savingsInsights.isNotEmpty()) {
            item {
                MoneyLeaksCard(
                    insights = uiState.savingsInsights,
                    totalSavingsPotential = uiState.totalSavingsPotential,
                    currency = uiState.currency,
                    onCategoryClick = { category ->
                        onNavigateToTransactions(category, null, selectedPeriod.name, selectedCurrency)
                    },
                    onMerchantClick = { merchant ->
                        onNavigateToTransactions(null, merchant, selectedPeriod.name, selectedCurrency)
                    },
                    onSubscriptionsClick = onNavigateToSubscriptions
                )
            }
        }

        // AI Savings Advice (when model downloaded)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                if (uiState.activeSubscriptionsCount > 0) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToSubscriptions() },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Plan ahead for bills",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "You have ${uiState.activeSubscriptionsCount} subscription(s). View due dates so you're not caught off guard.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "View",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                AiSavingsAdviceCard(
                    modelDownloaded = modelDownloaded,
                    modelState = modelState,
                    aiAdviceState = aiAdviceState,
                    onRequestAdvice = { viewModel.requestAiSavingsAdvice() },
                    onDismiss = { viewModel.dismissAiAdvice() },
                    onNavigateToChat = onNavigateToChat
                )
            }
        }

        // Top Merchants Section
        if (uiState.topMerchants.isNotEmpty()) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SectionHeader(title = "Top Merchants")
                    Text(
                        text = "Tap a merchant to see transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            
            item {
                ExpandableList(
                    items = uiState.topMerchants,
                    visibleItemCount = 3,
                    modifier = Modifier.fillMaxWidth()
                ) { merchant ->
                    MerchantListItem(
                        merchant = merchant,
                        currency = selectedCurrency,
                        onClick = {
                            onNavigateToTransactions(null, merchant.name, selectedPeriod.name, selectedCurrency)
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
private fun CategoryListItem(
    category: CategoryData,
    currency: String
) {
    val categoryInfo = CategoryMapping.categories[category.name]
        ?: CategoryMapping.categories["Others"]!!
    
    ListItemCard(
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(categoryInfo.color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CategoryIcon(
                    category = category.name,
                    size = 24.dp,
                    tint = categoryInfo.color
                )
            }
        },
        title = category.name,
        subtitle = "${category.transactionCount} transactions",
        amount = CurrencyFormatter.formatCurrency(category.amount, currency),
        trailingContent = {
            Text(
                text = "${category.percentage.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun MerchantListItem(
    merchant: MerchantData,
    currency: String,
    onClick: () -> Unit = {}
) {
    val subtitle = buildString {
        append("${merchant.transactionCount} ")
        append(if (merchant.transactionCount == 1) "transaction" else "transactions")
        if (merchant.isSubscription) {
            append(" • Subscription")
        }
    }
    
    ListItemCard(
        leadingContent = {
            BrandIcon(
                merchantName = merchant.name,
                size = 40.dp,
                showBackground = true
            )
        },
        title = merchant.name,
        subtitle = subtitle,
        amount = CurrencyFormatter.formatCurrency(merchant.amount, currency),
        onClick = onClick
    )
}

@Composable
private fun EmptyAnalyticsState(
    onViewTransactions: () -> Unit = {}
) {
    FinndotCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.empty),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                contentDescription = "No data",
                modifier = Modifier.size(Dimensions.Icon.extraLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "No data for this period",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sync SMS or add transactions to see spending insights here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Every rupee counts. Tracking helps you stay in control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Button(
                onClick = onViewTransactions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text("View transactions")
            }
        }
    }
}

@Composable
private fun IncomeVsExpensesCard(
    income: BigDecimal,
    expenses: BigDecimal,
    remaining: BigDecimal,
    runwayDays: Int?,
    currency: String
) {
    FinndotCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Income vs expenses",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatCurrency(income, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatCurrency(expenses, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Left", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = CurrencyFormatter.formatCurrency(remaining, currency),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                        color = if (remaining >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
            if (runwayDays != null && runwayDays > 0) {
                Text(
                    text = "At your current spend rate, this could last about $runwayDays days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CurrencyFilterRow(
    selectedCurrency: String,
    availableCurrencies: List<String>,
    onCurrencySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        item {
            Text(
                text = "Currency:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    vertical = Spacing.sm,
                    horizontal = Spacing.xs
                )
            )
        }
        items(availableCurrencies) { currency ->
            FilterChip(
                selected = selectedCurrency == currency,
                onClick = { onCurrencySelected(currency) },
                label = { Text(currency) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    }
}

@Composable
private fun QuickWinsCard(
    quickWins: List<QuickWin>,
    currency: String,
    cutAmountToMatch: BigDecimal?,
    onCategoryClick: (String) -> Unit,
    onMerchantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Quick Wins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
            if (cutAmountToMatch != null && cutAmountToMatch > BigDecimal.ZERO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Get back on track", style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text("Cut ${CurrencyFormatter.formatCurrency(cutAmountToMatch, currency)}/day to match last month", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            quickWins.forEach { win ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (win.categoryName != null || win.merchantName != null) Modifier.clickable {
                                when {
                                    win.categoryName != null -> onCategoryClick(win.categoryName!!)
                                    win.merchantName != null -> onMerchantClick(win.merchantName!!)
                                    else -> { }
                                }
                            }
                            else Modifier
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(win.action, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                        Text("≈ ${CurrencyFormatter.formatCurrency(win.amount, currency)} saved", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun MoneyLeaksCard(
    insights: List<SavingsInsight>,
    totalSavingsPotential: BigDecimal,
    currency: String,
    onCategoryClick: (String) -> Unit,
    onMerchantClick: (String) -> Unit = {},
    onSubscriptionsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Ways to save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                    )
                }
                if (totalSavingsPotential > BigDecimal.ZERO) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Potential savings",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatCurrency(totalSavingsPotential, currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Text(
                text = "Tap an item to see transactions",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            insights.forEach { insight ->
                val isClickable = insight.categoryName != null || insight.merchantName != null || insight.type == InsightType.SUBSCRIPTIONS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isClickable) Modifier.clickable {
                                when {
                                    insight.type == InsightType.SUBSCRIPTIONS -> onSubscriptionsClick()
                                    insight.categoryName != null -> onCategoryClick(insight.categoryName!!)
                                    insight.merchantName != null -> onMerchantClick(insight.merchantName!!)
                                    else -> { }
                                }
                            }
                            else Modifier
                        )
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(Spacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = when (insight.type) {
                            InsightType.TOP_CATEGORY -> Icons.Default.Category
                            InsightType.CATEGORY_OVERSPEND -> Icons.Default.TrendingUp
                            InsightType.MONTH_COMPARISON -> Icons.AutoMirrored.Filled.TrendingUp
                            InsightType.SUBSCRIPTIONS -> Icons.Default.Subscriptions
                            InsightType.IMPULSE_SPENDING -> Icons.Default.Receipt
                            InsightType.REGRET_SPENDING -> Icons.Default.SentimentVeryDissatisfied
                            InsightType.AVERAGE_TRANSACTION -> Icons.Default.Receipt
                            InsightType.SPENDING_VELOCITY -> Icons.Default.Schedule
                            InsightType.GET_ON_TRACK -> Icons.Default.Schedule
                            InsightType.DAY_PATTERN -> Icons.Default.CalendarMonth
                            InsightType.BUDGET_OVERSPEND -> Icons.Default.AccountBalance
                            InsightType.MERCHANT_SPIKE -> Icons.Default.Store
                            InsightType.IMPROVEMENT_STREAK -> Icons.Default.Star
                            InsightType.WHAT_IF -> Icons.Default.ShowChart
                            InsightType.POTENTIAL_SAVINGS -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            insight.type == InsightType.REGRET_SPENDING -> MaterialTheme.colorScheme.error
                            insight.impactScore >= 80 -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                        )
                        Text(
                            text = insight.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        insight.amount?.let { amt ->
                            Text(
                                text = "→ Save ${CurrencyFormatter.formatCurrency(amt, currency)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                            )
                        }
                        insight.actionHint?.takeIf { isClickable }?.let { hint ->
                            Text(
                                text = hint,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSavingsAdviceCard(
    modelDownloaded: Boolean,
    modelState: ModelState,
    aiAdviceState: AiAdviceState,
    onRequestAdvice: () -> Unit,
    onDismiss: () -> Unit,
    onNavigateToChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDownloading = modelState == ModelState.DOWNLOADING
    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Savings Advice",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                )
            }
            when (aiAdviceState) {
                is AiAdviceState.Idle -> {
                    if (isDownloading) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text(
                                text = "Model is downloading… Wait for it to finish in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        TextButton(
                            onClick = onNavigateToChat,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open FinnDot AI")
                        }
                    } else if (modelDownloaded) {
                        Text(
                            text = "Find where you're losing money and get specific steps to save.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onRequestAdvice,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(Spacing.sm))
                            Text("Where am I losing money?")
                        }
                        TextButton(
                            onClick = onNavigateToChat,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open chat for more")
                        }
                    } else {
                        Text(
                            text = "Download the AI model in Settings for personalized savings tips based on your spending.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = onNavigateToChat,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to FinnDot AI")
                        }
                    }
                }
                is AiAdviceState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text("Generating advice...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is AiAdviceState.Content -> {
                    Text(
                        text = aiAdviceState.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onDismiss) { Text("Dismiss") }
                }
                is AiAdviceState.Error -> {
                    Text(
                        text = aiAdviceState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        TextButton(onClick = onRequestAdvice) { Text("Retry") }
                        TextButton(onClick = onDismiss) { Text("Dismiss") }
                    }
                }
            }
        }
    }
}
