package com.anomapro.finndot.presentation.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.components.SectionHeader
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.utils.CurrencyFormatter
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    viewModel: BudgetSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTotalBudgetDialog by remember { mutableStateOf(false) }
    var showCategoryBudgetDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingBudget by remember { mutableStateOf<com.anomapro.finndot.data.database.entity.BudgetEntity?>(null) }
    
    // Show snackbar for messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(uiState.message) {
        uiState.message?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error, duration = SnackbarDuration.Long)
            viewModel.clearMessage()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimensions.Padding.content),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            // Total Budget Section
            SectionHeader(title = "Monthly Budget")

            // Budget runway card
            if (uiState.runwayMessage != null && uiState.totalBudget > BigDecimal.ZERO) {
                FinndotCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                    ) {
                        Text(
                            text = "Budget Runway",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = uiState.runwayMessage!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (uiState.totalSpentThisMonth > BigDecimal.ZERO && uiState.totalBudget > BigDecimal.ZERO) {
                            val pct = (uiState.totalSpentThisMonth / uiState.totalBudget * BigDecimal(100)).toInt().coerceIn(0, 999)
                            Text(
                                text = "${uiState.totalSpentThisMonth.let { CurrencyFormatter.formatCurrency(it, uiState.currency) }} of budget used ($pct%)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            FinndotCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total Monthly Budget",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = if (uiState.totalBudget > BigDecimal.ZERO) {
                                    CurrencyFormatter.formatCurrency(uiState.totalBudget, uiState.currency)
                                } else {
                                    "Not set"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (uiState.totalBudget > BigDecimal.ZERO) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            if (uiState.totalBudget > BigDecimal.ZERO && uiState.daysLeftInMonth > 0) {
                                val daily = uiState.totalBudget / BigDecimal(LocalDate.now().lengthOfMonth())
                                Text(
                                    text = "~${CurrencyFormatter.formatCurrency(daily, uiState.currency)}/day",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(
                            onClick = { showTotalBudgetDialog = true }
                        ) {
                            Icon(
                                imageVector = if (uiState.totalBudget > BigDecimal.ZERO) Icons.Default.Edit else Icons.Default.Add,
                                contentDescription = if (uiState.totalBudget > BigDecimal.ZERO) "Edit budget" else "Set budget"
                            )
                        }
                    }
                }
            }

            // Pending subscriptions and repeat payments this month
            if (uiState.subscriptionsThisMonth.isNotEmpty() || uiState.recurringPaymentsThisMonth.isNotEmpty()) {
                SectionHeader(title = "Due This Month")
                FinndotCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        if (uiState.subscriptionsThisMonth.isNotEmpty()) {
                            Text(
                                text = "Subscriptions (${uiState.subscriptionsThisMonth.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            uiState.subscriptionsThisMonth.forEach { sub ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = sub.merchantName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = CurrencyFormatter.formatCurrency(sub.amount, sub.currency),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            val subTotal = uiState.subscriptionsThisMonth.sumOf { it.amount }
                            Text(
                                text = "Total: ${CurrencyFormatter.formatCurrency(subTotal, uiState.currency)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (uiState.recurringPaymentsThisMonth.isNotEmpty()) {
                            if (uiState.subscriptionsThisMonth.isNotEmpty()) Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = "Recurring / Repeat payments (${uiState.recurringPaymentsThisMonth.size})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total: ${CurrencyFormatter.formatCurrency(uiState.recurringTotalThisMonth, uiState.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Category Budgets Section
            SectionHeader(
                title = "Category Budgets",
                action = {
                    TextButton(
                        onClick = { showCategoryBudgetDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add category budget",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Add")
                    }
                }
            )
            
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.categoryBudgets.isEmpty()) {
                FinndotCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No category budgets set",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "Tap 'Add' to set budgets for specific categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                (uiState.categoryProgress.ifEmpty {
                    uiState.categoryBudgets.map { b ->
                        com.anomapro.finndot.presentation.budget.CategoryBudgetProgress(
                            category = b.category,
                            budgetAmount = b.amount,
                            spentAmount = BigDecimal.ZERO,
                            remaining = b.amount
                        )
                    }
                }).forEach { progress ->
                    CategoryBudgetItem(
                        category = progress.category,
                        currency = uiState.currency,
                        amount = progress.budgetAmount,
                        spent = progress.spentAmount,
                        remaining = progress.remaining,
                        onEdit = {
                            uiState.categoryBudgets.find { it.category == progress.category }?.let { b ->
                                editingBudget = b
                                selectedCategory = b.category
                                showCategoryBudgetDialog = true
                            }
                        },
                        onDelete = {
                            viewModel.deleteCategoryBudget(progress.category)
                        }
                    )
                }
            }
        }
    }
    
    // Total Budget Dialog
    if (showTotalBudgetDialog) {
        BudgetAmountDialog(
            title = if (uiState.totalBudget > BigDecimal.ZERO) "Edit Total Budget" else "Set Total Budget",
            initialAmount = if (uiState.totalBudget > BigDecimal.ZERO) uiState.totalBudget.toString() else "",
            onDismiss = { showTotalBudgetDialog = false },
            onConfirm = { amount ->
                if (amount.isNotBlank()) {
                    try {
                        val budgetAmount = BigDecimal(amount)
                        if (budgetAmount >= BigDecimal.ZERO) {
                            viewModel.setTotalBudget(budgetAmount)
                            showTotalBudgetDialog = false
                        }
                    } catch (e: Exception) {
                        // Invalid amount
                    }
                }
            }
        )
    }
    
    // Category Budget Dialog
    if (showCategoryBudgetDialog) {
        CategoryBudgetDialog(
            availableCategories = uiState.availableCategories,
            existingBudgets = uiState.categoryBudgets.map { it.category },
            selectedCategory = selectedCategory,
            initialAmount = editingBudget?.amount?.toString() ?: "",
            onDismiss = {
                showCategoryBudgetDialog = false
                selectedCategory = null
                editingBudget = null
            },
            onConfirm = { category, amount ->
                if (amount.isNotBlank()) {
                    try {
                        val budgetAmount = BigDecimal(amount)
                        if (budgetAmount >= BigDecimal.ZERO) {
                            viewModel.setCategoryBudget(category, budgetAmount)
                            showCategoryBudgetDialog = false
                            selectedCategory = null
                            editingBudget = null
                        }
                    } catch (e: Exception) {
                        // Invalid amount
                    }
                }
            }
        )
    }
}

@Composable
private fun CategoryBudgetItem(
    category: String,
    currency: String,
    amount: BigDecimal,
    spent: BigDecimal = BigDecimal.ZERO,
    remaining: BigDecimal = amount,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    FinndotCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = "Budget: ${CurrencyFormatter.formatCurrency(amount, currency)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (spent > BigDecimal.ZERO) {
                        val pct = if (amount > BigDecimal.ZERO) (spent / amount * BigDecimal(100)).toInt() else 0
                        Text(
                            text = "Spent: ${CurrencyFormatter.formatCurrency(spent, currency)} ($pct%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (remaining >= BigDecimal.ZERO) "Left: ${CurrencyFormatter.formatCurrency(remaining, currency)}" else "Over by ${CurrencyFormatter.formatCurrency(remaining.abs(), currency)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (remaining >= BigDecimal.ZERO) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit budget"
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete budget",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetAmountDialog(
    title: String,
    initialAmount: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var amountText by remember { mutableStateOf(initialAmount) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(amountText) },
                enabled = amountText.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryBudgetDialog(
    availableCategories: List<String>,
    existingBudgets: List<String>,
    selectedCategory: String?,
    initialAmount: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var selectedCat by remember { mutableStateOf(selectedCategory ?: "") }
    var amountText by remember { mutableStateOf(initialAmount) }
    
    // Filter out categories that already have budgets (unless editing)
    val availableCats = if (selectedCategory != null) {
        availableCategories
    } else {
        availableCategories.filter { it !in existingBudgets }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (selectedCategory != null) "Edit Category Budget" else "Add Category Budget") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Category Dropdown
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCats.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCat = category
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedCat, amountText) },
                enabled = selectedCat.isNotBlank() && amountText.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

