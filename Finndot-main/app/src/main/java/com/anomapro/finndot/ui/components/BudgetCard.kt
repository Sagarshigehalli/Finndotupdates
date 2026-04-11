package com.anomapro.finndot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun BudgetCard(
    budget: BigDecimal,
    actual: BigDecimal,
    remaining: BigDecimal,
    percentageUsed: Float,
    budgetScore: Float = 0f,
    currency: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val isOverBudget = remaining < BigDecimal.ZERO
    val progressColor = when {
        percentageUsed >= 100f -> MaterialTheme.colorScheme.error
        percentageUsed >= 80f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        percentageUsed >= 50f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val statusColor = when {
        isOverBudget -> MaterialTheme.colorScheme.error
        percentageUsed >= 80f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        percentageUsed >= 50f -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.primary
    }
    
    val statusText = when {
        isOverBudget -> "Over Budget"
        percentageUsed >= 100f -> "Budget Exceeded"
        percentageUsed >= 80f -> "Warning"
        percentageUsed >= 50f -> "On Track"
        else -> "Good"
    }
    
    FinndotCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            // Header with Budget Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget vs Actual",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (budget > BigDecimal.ZERO && budgetScore > 0f) {
                    // Budget Score Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${budgetScore.toInt()}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                budgetScore >= 80f -> MaterialTheme.colorScheme.primary
                                budgetScore >= 60f -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = statusColor
                    )
                }
            }
            
            // Progress Bar
            if (budget > BigDecimal.ZERO) {
                LinearProgressIndicator(
                    progress = { (percentageUsed / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            
            // Budget and Actual Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Budget Column
                Column {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = CurrencyFormatter.formatCurrency(budget, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Actual Column
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Actual",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = CurrencyFormatter.formatCurrency(actual, currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Remaining/Over Budget and Budget Score
            if (budget > BigDecimal.ZERO) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.xs),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                
                // Budget Score Row (if score is available)
                if (budgetScore > 0f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Budget Score",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            // Score indicator bar
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(6.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(3.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(budgetScore / 100f)
                                        .background(
                                            color = when {
                                                budgetScore >= 80f -> MaterialTheme.colorScheme.primary
                                                budgetScore >= 60f -> MaterialTheme.colorScheme.secondary
                                                else -> MaterialTheme.colorScheme.error
                                            },
                                            shape = RoundedCornerShape(3.dp)
                                        )
                                )
                            }
                        }
                        Text(
                            text = "${budgetScore.toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                budgetScore >= 80f -> MaterialTheme.colorScheme.primary
                                budgetScore >= 60f -> MaterialTheme.colorScheme.secondary
                                else -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(Spacing.xs))
                }
                
                // Remaining/Over Budget Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isOverBudget) "Over Budget" else "Remaining",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isOverBudget) {
                            "-${CurrencyFormatter.formatCurrency(remaining.abs(), currency)}"
                        } else {
                            CurrencyFormatter.formatCurrency(remaining, currency)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                // No budget set
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = Spacing.xs),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "Tap to set a monthly budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

