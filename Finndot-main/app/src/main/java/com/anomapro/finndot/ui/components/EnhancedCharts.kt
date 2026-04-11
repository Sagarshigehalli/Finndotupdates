package com.anomapro.finndot.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.income_light
import com.anomapro.finndot.ui.theme.income_dark
import androidx.compose.foundation.isSystemInDarkTheme
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class MonthlySpendingData(
    val month: YearMonth,
    val amount: BigDecimal,
    val income: BigDecimal = BigDecimal.ZERO,
    val regretAmount: BigDecimal = BigDecimal.ZERO,
    val currency: String = "INR"
)

data class CategorySpendingData(
    val category: String,
    val amount: BigDecimal,
    val percentage: Float,
    val color: Color
)

@Composable
fun SpendingTrendChart(
    spendingData: List<MonthlySpendingData>,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 180,
    showTitle: Boolean = true
) {
    if (spendingData.isEmpty()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No spending data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val sortedData = remember(spendingData) {
        spendingData.sortedBy { it.month }
    }

    val maxAmount = remember(sortedData) {
        val maxSpending = sortedData.maxOfOrNull { it.amount } ?: BigDecimal.ZERO
        val maxIncome = sortedData.maxOfOrNull { it.income } ?: BigDecimal.ZERO
        val maxRegret = sortedData.maxOfOrNull { it.regretAmount } ?: BigDecimal.ZERO
        maxOf(maxSpending, maxIncome, maxRegret).coerceAtLeast(BigDecimal.ONE)
    }

    val spendingLineColor = MaterialTheme.colorScheme.primary
    val incomeLineColor = if (!isSystemInDarkTheme()) income_light else income_dark
    val regretLineColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val backgroundColor = MaterialTheme.colorScheme.surface

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            if (showTitle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spending & Income Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(maxAmount, currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = CurrencyFormatter.formatCurrency(maxAmount, currency),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(spendingLineColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Spending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(incomeLineColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(regretLineColor, RoundedCornerShape(2.dp))
                    )
                    Text(
                        text = "Regret",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val padding = 8.dp.toPx()

                    // Draw grid lines
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val y = padding + (canvasHeight - padding * 2) * (i.toFloat() / gridLines)
                        drawLine(
                            color = gridColor,
                            start = Offset(padding, y),
                            end = Offset(canvasWidth - padding, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                        )
                    }

                    if (sortedData.size == 1) {
                        val point = sortedData.first()
                        // Draw spending point
                        val spendingY = canvasHeight - padding - ((point.amount.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2))
                        drawCircle(
                            color = spendingLineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(canvasWidth / 2 - 10.dp.toPx(), spendingY)
                        )
                        // Draw income point
                        val incomeY = canvasHeight - padding - ((point.income.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2))
                        drawCircle(
                            color = incomeLineColor,
                            radius = 4.dp.toPx(),
                            center = Offset(canvasWidth / 2 + 10.dp.toPx(), incomeY)
                        )
                    } else {
                        // Draw spending line
                        val spendingPath = Path()
                        val spendingPoints = mutableListOf<Offset>()
                        val incomePath = Path()
                        val incomePoints = mutableListOf<Offset>()
                        val regretPath = Path()
                        val regretPoints = mutableListOf<Offset>()

                        sortedData.forEachIndexed { index, data ->
                            val x = padding + (canvasWidth - padding * 2) * (index.toFloat() / (sortedData.size - 1).coerceAtLeast(1))
                            
                            // Spending line
                            val spendingY = canvasHeight - padding - ((data.amount.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2))
                            val spendingOffset = Offset(x, spendingY)
                            spendingPoints.add(spendingOffset)
                            
                            // Income line
                            val incomeY = canvasHeight - padding - ((data.income.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2))
                            val incomeOffset = Offset(x, incomeY)
                            incomePoints.add(incomeOffset)
                            
                            // Regret line
                            val regretY = canvasHeight - padding - ((data.regretAmount.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2))
                            val regretOffset = Offset(x, regretY)
                            regretPoints.add(regretOffset)

                            if (index == 0) {
                                spendingPath.moveTo(x, spendingY)
                                incomePath.moveTo(x, incomeY)
                                regretPath.moveTo(x, regretY)
                            } else {
                                spendingPath.lineTo(x, spendingY)
                                incomePath.lineTo(x, incomeY)
                                regretPath.lineTo(x, regretY)
                            }
                        }

                        // Draw spending gradient fill
                        val spendingFillPath = Path().apply {
                            addPath(spendingPath)
                            lineTo(canvasWidth - padding, canvasHeight - padding)
                            lineTo(padding, canvasHeight - padding)
                            close()
                        }

                        drawPath(
                            path = spendingFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    spendingLineColor.copy(alpha = 0.3f),
                                    spendingLineColor.copy(alpha = 0.05f)
                                ),
                                startY = padding,
                                endY = canvasHeight - padding
                            )
                        )

                        // Draw income gradient fill
                        val incomeFillPath = Path().apply {
                            addPath(incomePath)
                            lineTo(canvasWidth - padding, canvasHeight - padding)
                            lineTo(padding, canvasHeight - padding)
                            close()
                        }

                        drawPath(
                            path = incomeFillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    incomeLineColor.copy(alpha = 0.3f),
                                    incomeLineColor.copy(alpha = 0.05f)
                                ),
                                startY = padding,
                                endY = canvasHeight - padding
                            )
                        )

                        // Draw spending line
                        drawPath(
                            path = spendingPath,
                            color = spendingLineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw income line
                        drawPath(
                            path = incomePath,
                            color = incomeLineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                        
                        // Draw regret line (dashed)
                        drawPath(
                            path = regretPath,
                            color = regretLineColor,
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f))
                            )
                        )

                        // Draw spending points
                        spendingPoints.forEach { point ->
                            drawCircle(
                                color = backgroundColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = spendingLineColor,
                                radius = 4.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }

                        // Draw income points
                        incomePoints.forEach { point ->
                            drawCircle(
                                color = backgroundColor,
                                radius = 4.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = incomeLineColor,
                                radius = 4.dp.toPx(),
                                center = point,
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                    }
                }
            }

            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (sortedData.isNotEmpty()) {
                    Text(
                        text = sortedData.first().month.format(DateTimeFormatter.ofPattern("MMM yy")),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sortedData.size > 1) {
                        Text(
                            text = sortedData.last().month.format(DateTimeFormatter.ofPattern("MMM yy")),
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
fun CategoryDistributionChart(
    categoryData: List<CategorySpendingData>,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 200
) {
    if (categoryData.isEmpty()) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .height(height.dp),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No category data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val sortedData = remember(categoryData) {
        categoryData.sortedByDescending { it.amount }.take(6) // Top 6 categories
    }

    val totalAmount = remember(sortedData) {
        sortedData.sumOf { it.amount }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Distribution",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = CurrencyFormatter.formatCurrency(totalAmount, currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Donut chart
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = (size.minDimension / 2) * 0.8f
                        val innerRadius = radius * 0.5f

                        var startAngle = -90f

                        sortedData.forEach { data ->
                            val sweepAngle = (data.percentage / 100f) * 360f

                            // Draw segment
                            drawArc(
                                color = data.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = Offset(center.x - radius, center.y - radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = (radius - innerRadius), cap = StrokeCap.Round)
                            )

                            startAngle += sweepAngle
                        }
                    }
                }

                // Legend
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    sortedData.forEach { data ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(data.color, RoundedCornerShape(2.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = data.category,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${data.percentage.toInt()}%",
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
}

@Composable
fun MonthlyComparisonChart(
    currentMonth: BigDecimal,
    lastMonth: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 150
) {
    val maxAmount = remember(currentMonth, lastMonth) {
        maxOf(currentMonth, lastMonth).coerceAtLeast(BigDecimal.ONE)
    }

    val barColor = MaterialTheme.colorScheme.primary
    val lastMonthColor = MaterialTheme.colorScheme.secondary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(Spacing.md)
        ) {
            Text(
                text = "Monthly Comparison",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(Spacing.sm))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val padding = 16.dp.toPx()
                    val barWidth = (canvasWidth - padding * 2) / 2 - padding

                    val currentHeight = (currentMonth.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2)
                    val lastHeight = (lastMonth.toFloat() / maxAmount.toFloat()) * (canvasHeight - padding * 2)

                    // Draw last month bar
                    val lastMonthX = padding
                    drawRect(
                        color = lastMonthColor.copy(alpha = 0.6f),
                        topLeft = Offset(lastMonthX, canvasHeight - padding - lastHeight),
                        size = Size(barWidth, lastHeight)
                    )

                    // Draw current month bar
                    val currentX = padding + barWidth + padding
                    drawRect(
                        color = barColor.copy(alpha = 0.6f),
                        topLeft = Offset(currentX, canvasHeight - padding - currentHeight),
                        size = Size(barWidth, currentHeight)
                    )

                    // Draw outlines
                    drawRect(
                        color = lastMonthColor,
                        topLeft = Offset(lastMonthX, canvasHeight - padding - lastHeight),
                        size = Size(barWidth, lastHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )

                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentX, canvasHeight - padding - currentHeight),
                        size = Size(barWidth, currentHeight),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Last Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(lastMonth, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "This Month",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatCurrency(currentMonth, currency),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

