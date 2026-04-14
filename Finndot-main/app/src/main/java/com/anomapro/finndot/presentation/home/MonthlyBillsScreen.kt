package com.anomapro.finndot.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.domain.analytics.RecurringBillCadence
import com.anomapro.finndot.domain.analytics.RecurringBillType
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.utils.CurrencyFormatter
import java.time.format.DateTimeFormatter

@Composable
fun MonthlyBillsScreen(
    selectedCurrency: String,
    viewModel: MonthlyBillsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var historyExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedCurrency) {
        viewModel.load(selectedCurrency)
    }

    when {
        uiState.isLoading -> {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) { CircularProgressIndicator() }
        }
        uiState.loadFailed -> {
            MonthlyBillsLoadError(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                onRetry = { viewModel.load(selectedCurrency) }
            )
        }
        !uiState.hasRecurringBillsForCurrency -> {
            MonthlyBillsEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            )
        }
        else -> {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                item {
                    HeaderCard(
                        uiState = uiState,
                        showIntervalFootnote = uiState.intervalRecurringItems.isNotEmpty(),
                    )
                }

                if (uiState.dueThisWeek.isNotEmpty()) {
                    item { GroupTitle("Due this week") }
                    items(uiState.dueThisWeek, key = { "${it.merchant}_${it.dueDate}_${it.expectedAmount}" }) { row ->
                        BillRow(row)
                    }
                }

                if (uiState.dueNext.isNotEmpty()) {
                    item { GroupTitle("Due next") }
                    items(uiState.dueNext, key = { "${it.merchant}_${it.dueDate}_${it.expectedAmount}" }) { row ->
                        BillRow(row)
                    }
                }

                if (uiState.dueLater.isNotEmpty()) {
                    item { GroupTitle("Later") }
                    items(uiState.dueLater, key = { "${it.merchant}_${it.dueDate}_${it.expectedAmount}" }) { row ->
                        BillRow(row)
                    }
                }

                val hasAnyDueGroup = uiState.dueThisWeek.isNotEmpty() ||
                    uiState.dueNext.isNotEmpty() ||
                    uiState.dueLater.isNotEmpty()
                if (!hasAnyDueGroup) {
                    item {
                        Text(
                            text = if (uiState.intervalRecurringItems.isNotEmpty()) {
                                "No monthly-pattern bills due this calendar month. Longer-cycle recurring is listed below."
                            } else {
                                "Nothing due this calendar month. Other recurring bills may be due next month."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                }

                if (uiState.intervalRecurringItems.isNotEmpty()) {
                    item { GroupTitle("Other recurring (every few months)") }
                    items(
                        uiState.intervalRecurringItems,
                        key = { "${it.merchant}_${it.dueDate}_${it.medianIntervalDays}_${it.expectedAmount}" }
                    ) { row ->
                        BillRow(row)
                    }
                }

                item {
                    PaymentHistorySectionHeader(
                        expanded = historyExpanded,
                        onToggle = { historyExpanded = !historyExpanded }
                    )
                }
                if (historyExpanded) {
                    if (uiState.paymentHistoryByMonth.isEmpty()) {
                        item {
                            Text(
                                text = "No payments to these merchants in the last 12 months.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.md)
                            )
                        }
                    } else {
                        items(uiState.paymentHistoryByMonth, key = { it.month.toString() }) { group ->
                            PaymentHistoryMonthGroup(group)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlyBillsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No recurring bills detected",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "We detect monthly patterns from your last six months of spending, and longer cycles (e.g. quarterly) using up to about 18 months of history and two or more similar payments. Keep logging expenses and check again after a few billing cycles.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun MonthlyBillsLoadError(
    modifier: Modifier = Modifier,
    onRetry: () -> Unit,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Could not load recurring bills",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = "Check your connection and try again.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        OutlinedButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@Composable
private fun HeaderCard(
    uiState: MonthlyBillsUiState,
    showIntervalFootnote: Boolean,
) {
    val total = uiState.totalThisMonth
    val fixedRatio = if (total > java.math.BigDecimal.ZERO) {
        (uiState.fixedAmountThisMonth / total).toFloat().coerceIn(0f, 1f)
    } else 0f

    FinndotCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Text(
                text = "Total this month (monthly pattern)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = CurrencyFormatter.formatCurrency(total, uiState.currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Fixed ${CurrencyFormatter.formatCurrency(uiState.fixedAmountThisMonth, uiState.currency)} • " +
                    "Variable ${CurrencyFormatter.formatCurrency(uiState.variableAmountThisMonth, uiState.currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { fixedRatio },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Paid ${uiState.paidCountThisMonth} of ${uiState.totalCountThisMonth}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showIntervalFootnote) {
                Text(
                    text = "Longer-cycle bills below are not included in this total.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BillRow(item: MonthlyBillUiItem) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM")
    val dueSubtitle = if (item.cadence == RecurringBillCadence.INTERVAL && item.medianIntervalDays != null) {
        val approxMonths = maxOf(1, (item.medianIntervalDays + 15) / 30)
        "Due ${item.dueDate.format(formatter)} · ~every ${item.medianIntervalDays} d (~$approxMonths mo.)"
    } else {
        "Due ${item.dueDate.format(formatter)}"
    }
    FinndotCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.merchant,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = dueSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = CurrencyFormatter.formatCurrency(item.expectedAmount, item.currency),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            when {
                                item.cadence == RecurringBillCadence.INTERVAL && item.medianIntervalDays != null ->
                                    "~${item.medianIntervalDays}d · " + if (item.type == RecurringBillType.FIXED) "Fixed" else "Variable"
                                item.type == RecurringBillType.FIXED -> "Fixed"
                                else -> "Variable"
                            }
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors()
                )
            }
        }
    }
}

@Composable
private fun PaymentHistorySectionHeader(
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Payment history",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onToggle) {
            Text(if (expanded) "Hide" else "View")
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }
    }
}

@Composable
private fun PaymentHistoryMonthGroup(group: MonthlyBillHistoryGroup) {
    val monthLabel = group.month.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${group.month.year}"
    FinndotCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            Text(
                text = monthLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            group.items.forEach { item ->
                PaymentHistoryRow(item)
            }
        }
    }
}

@Composable
private fun PaymentHistoryRow(item: MonthlyBillHistoryItem) {
    val fmt = DateTimeFormatter.ofPattern("dd MMM")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.merchant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Paid ${item.paidDate.format(fmt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = CurrencyFormatter.formatCurrency(item.amount, item.currency),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

