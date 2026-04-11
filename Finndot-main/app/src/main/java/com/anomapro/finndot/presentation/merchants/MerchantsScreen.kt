package com.anomapro.finndot.presentation.merchants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantsScreen(
    viewModel: MerchantsViewModel = hiltViewModel(),
    onNavigateToTransactions: (merchant: String) -> Unit = {},
    onNavigateToMapping: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sortType by viewModel.sortType.collectAsStateWithLifecycle()
    val periodFilter by viewModel.periodFilter.collectAsStateWithLifecycle()
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.merchants.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No merchants found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
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
                // Header with Mapping Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Merchants",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onNavigateToMapping) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Manage Merchant Mappings"
                            )
                        }
                    }
                }
                
                // Compact Filters
                item {
                    var sortExpanded by remember { mutableStateOf(false) }
                    var periodExpanded by remember { mutableStateOf(false) }

                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Text(
                            text = "Filters",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.sm),
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FilledTonalButton(
                                        onClick = { sortExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.Sort,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sort • " + when (sortType) {
                                                MerchantSortType.AMOUNT -> "Amount"
                                                MerchantSortType.TRANSACTION_COUNT -> "Transactions"
                                                MerchantSortType.NAME -> "Name"
                                            }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = sortExpanded,
                                        onDismissRequest = { sortExpanded = false }
                                    ) {
                                        MerchantSortType.values().forEach { sort ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        when (sort) {
                                                            MerchantSortType.AMOUNT -> "Amount"
                                                            MerchantSortType.TRANSACTION_COUNT -> "Transactions"
                                                            MerchantSortType.NAME -> "Name"
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setSortType(sort)
                                                    sortExpanded = false
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        when (sort) {
                                                            MerchantSortType.AMOUNT -> Icons.Default.CurrencyRupee
                                                            MerchantSortType.TRANSACTION_COUNT -> Icons.Default.List
                                                            MerchantSortType.NAME -> Icons.Default.SortByAlpha
                                                        },
                                                        contentDescription = null
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    FilledTonalButton(
                                        onClick = { periodExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Icon(
                                            Icons.Default.DateRange,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Period • " + when (periodFilter) {
                                                MerchantPeriodFilter.LAST_3_MONTHS -> "3 months"
                                                MerchantPeriodFilter.LAST_6_MONTHS -> "6 months"
                                                MerchantPeriodFilter.LAST_12_MONTHS -> "12 months"
                                                MerchantPeriodFilter.ALL -> "All time"
                                            }
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = periodExpanded,
                                        onDismissRequest = { periodExpanded = false }
                                    ) {
                                        MerchantPeriodFilter.values().forEach { filter ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        when (filter) {
                                                            MerchantPeriodFilter.LAST_3_MONTHS -> "Last 3 months"
                                                            MerchantPeriodFilter.LAST_6_MONTHS -> "Last 6 months"
                                                            MerchantPeriodFilter.LAST_12_MONTHS -> "Last 12 months"
                                                            MerchantPeriodFilter.ALL -> "All time"
                                                        }
                                                    )
                                                },
                                                onClick = {
                                                    viewModel.setPeriodFilter(filter)
                                                    periodExpanded = false
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.DateRange, contentDescription = null)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Merchants List
                items(
                    items = uiState.merchants,
                    key = { it.name }
                ) { merchant ->
                    MerchantCard(
                        merchant = merchant,
                        onClick = {
                            onNavigateToTransactions(merchant.name)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MerchantCard(
    merchant: MerchantInfo,
    onClick: () -> Unit
) {
    FinndotCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card)
        ) {
            // Merchant Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = merchant.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // Total Amount
                Text(
                    text = CurrencyFormatter.formatCurrency(merchant.totalAmount, "INR"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(modifier = Modifier.height(Spacing.md))
            
            // Debits and Credits Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Debits Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Debits",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCurrency(merchant.debits, "INR"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                
                // Credits Card
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Credits",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.formatCurrency(merchant.credits, "INR"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(Spacing.sm))
            
            // Transaction Count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${merchant.transactionCount} transaction${if (merchant.transactionCount != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Net Amount
                if (merchant.netAmount != BigDecimal.ZERO) {
                    Text(
                        text = "Net: ${CurrencyFormatter.formatCurrency(merchant.netAmount.abs(), "INR")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = if (merchant.netAmount > BigDecimal.ZERO) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            }
        }
    }
}
