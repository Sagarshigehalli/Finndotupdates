package com.anomapro.finndot.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.income_light
import com.anomapro.finndot.ui.theme.income_dark
import androidx.compose.foundation.isSystemInDarkTheme
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal

@Composable
fun InvestmentChart(
    totalInvestment: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 120
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Investments (12M)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = CurrencyFormatter.formatCurrency(totalInvestment, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun AverageIncomeChart(
    averageIncome: BigDecimal,
    currency: String,
    modifier: Modifier = Modifier,
    height: Int = 120
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Avg Income (12M)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = CurrencyFormatter.formatCurrency(averageIncome, currency),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = if (!isSystemInDarkTheme()) income_light else income_dark
            )
        }
    }
}

@Composable
fun FinnDotScoreChart(
    score: Float,
    modifier: Modifier = Modifier,
    height: Int = 120
) {
    val scorePercentage = (score * 100).toInt()
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Finn Dot Score",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "$scorePercentage%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

