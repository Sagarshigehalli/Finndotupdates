package com.anomapro.finndot.presentation.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.ui.components.CategorySpendingData
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.expense_light
import com.anomapro.finndot.ui.theme.income_light
import com.anomapro.finndot.ui.theme.income_dark
import com.anomapro.finndot.ui.theme.expense_dark
import androidx.compose.foundation.isSystemInDarkTheme
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.math.min

@Composable
fun BudgetHeroCard(
    totalBudget: BigDecimal,
    spent: BigDecimal,
    budgetUsedPercent: Float,
    monthElapsedPercent: Float,
    remaining: BigDecimal,
    projectedSpend: BigDecimal,
    regretAmount: BigDecimal,
    currency: String,
    daysLeft: Int,
    dailyBudget: BigDecimal,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isOver = budgetUsedPercent >= 100f
    val projectedOver = projectedSpend > totalBudget && totalBudget > BigDecimal.ZERO
    val progressColor = when {
        isOver -> MaterialTheme.colorScheme.error
        budgetUsedPercent >= 80f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    val monthTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val statusText = if (isOver) "Over budget" else "On track"
    val remainingText = if (isOver) {
        "-${CurrencyFormatter.formatCurrency(remaining.abs(), currency)}"
    } else {
        CurrencyFormatter.formatCurrency(remaining.abs(), currency)
    }
    val paceDelta = budgetUsedPercent - monthElapsedPercent
    val paceMessage = when {
        totalBudget <= BigDecimal.ZERO -> null
        isOver -> "Budget exceeded"
        paceDelta > 8f -> "Spending faster than the month is moving"
        paceDelta < -8f -> "Running comfortably below your monthly pace"
        else -> "Spending is tracking close to your monthly pace"
    }
    val projectedMessage = when {
        totalBudget <= BigDecimal.ZERO || projectedSpend <= BigDecimal.ZERO -> null
        projectedOver -> "Projected month-end: ${CurrencyFormatter.formatCurrency(projectedSpend, currency)}"
        else -> "Projected month-end: ${CurrencyFormatter.formatCurrency(projectedSpend, currency)}"
    }

    FinndotCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Budget status: $statusText. $remainingText remaining. $daysLeft days left in month." },
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(96.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = min(size.width, size.height) / 2f - 8.dp.toPx()
                        val strokeWidth = 11.dp.toPx()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val sweepMonth = (monthElapsedPercent / 100f).coerceIn(0f, 1f) * 360f
                        val sweepUsed = (budgetUsedPercent / 100f).coerceIn(0f, 1f) * 360f
                        drawArc(
                            color = trackColor,
                            startAngle = 90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = monthTrackColor,
                            startAngle = 90f,
                            sweepAngle = -sweepMonth,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = progressColor,
                            startAngle = 90f,
                            sweepAngle = -sweepUsed,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${budgetUsedPercent.toInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "used",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    Text(
                        text = if (isOver) "Over budget" else "Budget status",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = remainingText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isOver) {
                            "Exceeded from ${CurrencyFormatter.formatCurrency(totalBudget, currency)} budget"
                        } else {
                            "Left from ${CurrencyFormatter.formatCurrency(totalBudget, currency)} budget"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MetricRow(
                    label = "Spent",
                    value = CurrencyFormatter.formatCurrency(spent, currency)
                )
                MetricRow(
                    label = "Daily safe spend",
                    value = if (dailyBudget > BigDecimal.ZERO) {
                        CurrencyFormatter.formatCurrency(dailyBudget, currency)
                    } else {
                        "Not available"
                    }
                )
                MetricRow(
                    label = "Days left",
                    value = "$daysLeft"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                BudgetProgressRow(
                    label = "Budget used",
                    valueText = "${budgetUsedPercent.toInt()}%",
                    progress = budgetUsedPercent / 100f,
                    color = progressColor
                )
                BudgetProgressRow(
                    label = "Month gone",
                    valueText = "${monthElapsedPercent.toInt()}%",
                    progress = monthElapsedPercent / 100f,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            paceMessage?.let {
                AssistChip(
                    onClick = onClick,
                    label = { Text(it) }
                )
            }

            projectedMessage?.let {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (projectedOver) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (projectedOver) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                }
                            )
                            if (regretAmount > BigDecimal.ZERO) {
                                Text(
                                    text = "Regret spend: ${CurrencyFormatter.formatCurrency(regretAmount, currency)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (projectedOver) {
                                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    }
                                )
                            }
                        }
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (projectedOver) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun BudgetProgressRow(
    label: String,
    valueText: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun SetBudgetNudgeCard(
    recommendedBudget: BigDecimal = BigDecimal.ZERO,
    currency: String = "INR",
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    FinndotCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Set a budget to see daily limit and stay on track" },
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Set a budget",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (recommendedBudget > BigDecimal.ZERO) {
                        "Start with ${CurrencyFormatter.formatCurrency(recommendedBudget, currency)} from your last 12 months"
                    } else {
                        "See daily limit and stay on track"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Set",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun IncomeVsExpenseBars(
    income: BigDecimal,
    expenses: BigDecimal,
    currency: String,
    title: String = "Income vs Spent",
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    onIncomeClick: () -> Unit = {},
    onSpendClick: () -> Unit = {}
) {
    val maxVal = maxOf(income, expenses).coerceAtLeast(BigDecimal.ONE)
    val incomeColor = if (!isSystemInDarkTheme()) income_light else income_dark
    val expenseColor = if (!isSystemInDarkTheme()) expense_light else expense_dark
    val incomeRatio = (income.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
    val expenseRatio = (expenses.toFloat() / maxVal.toFloat()).coerceIn(0f, 1f)
    val net = income - expenses

    FinndotCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Income ${CurrencyFormatter.formatCurrency(income, currency)}, Spent ${CurrencyFormatter.formatCurrency(expenses, currency)}" },
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (showTitle) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onIncomeClick)
                ) {
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(income, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = incomeColor
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onSpendClick),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(expenses, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = expenseColor
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onIncomeClick)
            ) {
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { incomeRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = incomeColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSpendClick)
            ) {
                Text(
                    text = "Spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { expenseRatio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = expenseColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            }

            Text(
                text = if (net >= BigDecimal.ZERO) {
                    "Net left: ${CurrencyFormatter.formatCurrency(net, currency)}"
                } else {
                    "Overspent by ${CurrencyFormatter.formatCurrency(net.abs(), currency)}"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (net >= BigDecimal.ZERO) incomeColor else expenseColor
            )
        }
    }
}

@Composable
fun TopCategoriesBarChart(
    categories: List<CategorySpendingData>,
    currency: String,
    title: String = "Where you spent",
    modifier: Modifier = Modifier,
    maxItems: Int = 5,
    showTitle: Boolean = true,
    onCategoryClick: (String) -> Unit = {}
) {
    val top = categories.take(maxItems)
    if (top.isEmpty()) return
    val maxAmount = top.maxOfOrNull { it.amount } ?: BigDecimal.ONE

    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (showTitle) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            top.forEach { data ->
                val ratio = (data.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(data.category) },
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.category,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${data.percentage.toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Text(
                            text = CurrencyFormatter.formatCurrency(data.amount, currency),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(6.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(data.color, RoundedCornerShape(6.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Last7DaysSparkline(
    dailySpend: List<BigDecimal>,
    dailyBudget: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    if (dailySpend.size != 7) return
    val maxSpend = dailySpend.maxOrNull() ?: BigDecimal.ONE
    val totalSpend = dailySpend.fold(BigDecimal.ZERO) { acc, value -> acc + value }
    val dayLabels = (0..6).map { index ->
        LocalDate.now()
            .minusDays((6 - index).toLong())
            .dayOfWeek
            .name
            .take(3)
    }

    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            if (showTitle) {
                Text(
                    text = "Last 7 Days",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Total ${CurrencyFormatter.formatCurrency(totalSpend, currency)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailySpend.forEachIndexed { index, amount ->
                    val h = (amount.toFloat() / maxSpend.toFloat()).coerceIn(0.05f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .height(52.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(4.dp)
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .width(22.dp)
                                    .fillMaxHeight(h)
                                    .background(
                                        if (dailyBudget > BigDecimal.ZERO && amount > dailyBudget)
                                            MaterialTheme.colorScheme.error
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                        Text(
                            text = dayLabels[index],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RegretDonutCard(
    regretAmount: BigDecimal,
    totalSpend: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    if (totalSpend <= BigDecimal.ZERO) return
    val regretPercent = (regretAmount.toFloat() / totalSpend.toFloat() * 100f).coerceIn(0f, 100f)

    FinndotCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Regret spend ${regretPercent.toInt()}%, ${CurrencyFormatter.formatCurrency(regretAmount, currency)}. Tap to review." },
        onClick = onTap
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
                    val errorColor = MaterialTheme.colorScheme.error
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = min(size.width, size.height) / 2f - 4.dp.toPx()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawArc(
                            color = surfaceVariantColor.copy(alpha = 0.6f),
                            startAngle = 90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = errorColor,
                            startAngle = 90f,
                            sweepAngle = -(regretPercent / 100f * 360f),
                            useCenter = false,
                            topLeft = Offset(center.x - radius, center.y - radius),
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                Column {
                    Text(
                        text = "Regret spend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${regretPercent.toInt()}% · ${CurrencyFormatter.formatCurrency(regretAmount, currency)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "Review",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

data class UpcomingBillItem(
    val name: String,
    val amount: BigDecimal,
    val daysUntil: Int,
    val currency: String
)

@Composable
fun UpcomingBillsTimeline(
    items: List<UpcomingBillItem>,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    if (items.isEmpty()) return
    FinndotCard(modifier = modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            Text(
                text = "Upcoming",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items.take(3).forEach { bill ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = bill.name.take(8).let { if (bill.name.length > 8) "$it…" else it },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = CurrencyFormatter.formatCurrency(bill.amount, bill.currency),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "in ${bill.daysUntil}d",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
