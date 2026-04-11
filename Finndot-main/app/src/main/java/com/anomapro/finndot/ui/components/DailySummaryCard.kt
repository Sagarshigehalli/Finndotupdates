package com.anomapro.finndot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.presentation.home.DailySummary
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.expense_light
import com.anomapro.finndot.ui.theme.income_light
import com.anomapro.finndot.ui.theme.income_dark
import com.anomapro.finndot.ui.theme.expense_dark
import androidx.compose.foundation.isSystemInDarkTheme
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun DailySummaryCard(
    dailySummary: DailySummary,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    val isPositive = dailySummary.netAmount >= BigDecimal.ZERO
    val netColor = if (isPositive) {
        if (!isSystemInDarkTheme()) income_light else income_dark
    } else {
        if (!isSystemInDarkTheme()) expense_light else expense_dark
    }
    
    val spendingColor = if (!isSystemInDarkTheme()) expense_light else expense_dark
    val regretColor = MaterialTheme.colorScheme.error
    val statContainerAlpha = if (isSystemInDarkTheme()) 0.22f else 0.1f
    
    // Motivational message based on spending
    val dailyInsight = when {
        dailySummary.earnings == BigDecimal.ZERO && dailySummary.spending == BigDecimal.ZERO ->
            "No activity today yet"
        dailySummary.regretSpending > BigDecimal.ZERO ->
            "Regret spend: ${CurrencyFormatter.formatCurrency(dailySummary.regretSpending, dailySummary.currency)}"
        dailySummary.netAmount >= BigDecimal.ZERO ->
            "You're positive today"
        else ->
            "You're negative today"
    }
    
    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        applyOuterPadding = false,
        contentPadding = PaddingValues(Dimensions.Padding.content)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showTitle) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Today,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Today's Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // Net amount badge
                Surface(
                    color = netColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isPositive) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = netColor
                        )
                        Text(
                            text = CurrencyFormatter.formatCurrency(dailySummary.netAmount.abs(), dailySummary.currency),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                    }
                }
            }
            
            // Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Earnings
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "Earnings",
                    amount = dailySummary.earnings,
                    currency = dailySummary.currency,
                    icon = Icons.Default.AccountBalanceWallet,
                    color = if (!isSystemInDarkTheme()) income_light else income_dark,
                    containerAlpha = statContainerAlpha
                )
                
                // Spending
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "Spending",
                    amount = dailySummary.spending,
                    currency = dailySummary.currency,
                    icon = Icons.Default.ShoppingCart,
                    color = spendingColor,
                    containerAlpha = statContainerAlpha
                )
                
                // Regret Spending
                StatBox(
                    modifier = Modifier.weight(1f),
                    label = "Regret",
                    amount = dailySummary.regretSpending,
                    currency = dailySummary.currency,
                    icon = Icons.Default.SentimentVeryDissatisfied,
                    color = regretColor,
                    containerAlpha = statContainerAlpha
                )
            }
            
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = dailyInsight,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    amount: BigDecimal,
    currency: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    containerAlpha: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = containerAlpha),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.xs)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Text(
                text = CurrencyFormatter.formatCurrency(amount, currency),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

