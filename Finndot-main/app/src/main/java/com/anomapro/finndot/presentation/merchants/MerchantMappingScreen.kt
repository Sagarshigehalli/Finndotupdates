package com.anomapro.finndot.presentation.merchants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantMappingScreen(
    viewModel: MerchantMappingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Merchant Name Mapping") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // Show help or info
                        }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Info")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(
                        horizontal = Dimensions.Padding.content,
                        vertical = Spacing.md
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Info Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(Spacing.md)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "Map different merchant name variations to a single name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    
                    // Mappings List
                    if (uiState.mappings.isNotEmpty()) {
                        item {
                            Text(
                                "Current Mappings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = Spacing.sm)
                            )
                        }
                        
                        // Group by normalized name
                        val groupedMappings = uiState.mappings.groupBy { it.normalizedName }
                        groupedMappings.forEach { (normalizedName, mappings) ->
                            item {
                                MerchantMappingGroupCard(
                                    normalizedName = normalizedName,
                                    mappings = mappings,
                                    onDelete = { originalName ->
                                        viewModel.deleteMapping(originalName)
                                    }
                                )
                            }
                        }
                    }
                    
                    // All Merchants Section
                    item {
                        Text(
                            "All Merchants",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = Spacing.sm)
                        )
                    }
                    
                    // Group merchants by normalized name or show individually
                    val merchantsToShow = if (uiState.mappings.isNotEmpty()) {
                        val mappedOriginals = uiState.mappings.map { it.originalName }.toSet()
                        uiState.allMerchants.filter { it !in mappedOriginals }
                    } else {
                        uiState.allMerchants
                    }
                    
                    items(merchantsToShow.take(50)) { merchant ->
                        MerchantItemCard(
                            merchantName = merchant,
                            suggestedName = viewModel.getSuggestedNormalizedName(merchant),
                            onMap = { originalName ->
                                // Show dialog to set normalized name
                                viewModel.showMapDialog(originalName)
                            },
                            onMerge = { merchantName ->
                                // Find similar merchants using enhanced algorithm
                                val similar = viewModel.findSimilarMerchants(merchantName)
                                if (similar.isNotEmpty()) {
                                    viewModel.showMergeDialog(listOf(merchantName) + similar)
                                } else {
                                    // Show message that no similar merchants found
                                }
                            }
                        )
                    }
                }
            }
            
            // Error Snackbar
            uiState.errorMessage?.let { error ->
                LaunchedEffect(error) {
                    // Show snackbar
                    viewModel.clearError()
                }
            }
            
            // Map Dialog
            if (uiState.showMapDialog) {
                MapMerchantDialog(
                    originalName = uiState.mapOriginalName,
                    targetName = uiState.mapTargetName,
                    allMerchants = uiState.allMerchants,
                    onTargetNameChange = viewModel::updateMapTargetName,
                    onConfirm = { updateExisting ->
                        viewModel.createMapping(
                            uiState.mapOriginalName,
                            uiState.mapTargetName.trim(),
                            updateExisting
                        )
                    },
                    onDismiss = viewModel::hideMapDialog
                )
            }
            
            // Merge Dialog
            if (uiState.showMergeDialog) {
                MergeMerchantsDialog(
                    merchants = uiState.selectedMerchants,
                    targetName = uiState.mergeTargetName,
                    onTargetNameChange = viewModel::updateMergeTargetName,
                    onConfirm = { updateExisting ->
                        viewModel.mergeMerchants(updateExisting)
                    },
                    onDismiss = viewModel::hideMergeDialog
                )
            }
        }
    }
}

@Composable
private fun MerchantMappingGroupCard(
    normalizedName: String,
    mappings: List<com.anomapro.finndot.data.database.entity.MerchantNameMappingEntity>,
    onDelete: (String) -> Unit
) {
    FinndotCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.Padding.card)
        ) {
            Text(
                text = normalizedName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Maps to:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            mappings.forEach { mapping ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = mapping.originalName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    IconButton(
                        onClick = { onDelete(mapping.originalName) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete mapping",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MerchantItemCard(
    merchantName: String,
    suggestedName: String = "",
    onMap: (String) -> Unit,
    onMerge: (String) -> Unit
) {
    FinndotCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.card),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = merchantName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (suggestedName.isNotEmpty() && suggestedName != merchantName) {
                            Text(
                                text = "Suggested: $suggestedName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        IconButton(
                            onClick = { onMap(merchantName) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Map merchant",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMerge(merchantName) },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.MergeType,
                                contentDescription = "Merge merchants",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
    }
}

@Composable
private fun MapMerchantDialog(
    originalName: String,
    targetName: String,
    allMerchants: List<String>,
    onTargetNameChange: (String) -> Unit,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var updateExisting by remember { mutableStateOf(true) }
    var showSuggestions by remember { mutableStateOf(false) }
    val filteredMerchants = remember(targetName) {
        if (targetName.length >= 2) {
            allMerchants.filter { 
                it.lowercase().contains(targetName.lowercase()) && 
                it != originalName 
            }.take(5)
        } else {
            emptyList()
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Map Merchant Name") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Map \"$originalName\" to:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                OutlinedTextField(
                    value = targetName,
                    onValueChange = { 
                        onTargetNameChange(it)
                        showSuggestions = it.isNotEmpty()
                    },
                    label = { Text("Normalized Name") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("Enter any name to map this merchant to (can be completely different)")
                    },
                    placeholder = {
                        Text("Enter merchant name or select from suggestions")
                    },
                    trailingIcon = {
                        if (targetName.isNotEmpty()) {
                            IconButton(onClick = { onTargetNameChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
                
                // Show suggestions if available
                if (showSuggestions && filteredMerchants.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.sm)
                        ) {
                            Text(
                                text = "Suggestions (optional - you can type any name):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = Spacing.xs)
                            )
                            filteredMerchants.forEach { merchant ->
                                TextButton(
                                    onClick = {
                                        onTargetNameChange(merchant)
                                        showSuggestions = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = merchant,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Start
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Checkbox(
                        checked = updateExisting,
                        onCheckedChange = { updateExisting = it }
                    )
                    Text(
                        text = "Update existing transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(updateExisting) },
                enabled = targetName.trim().isNotEmpty()
            ) {
                Text("Map")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MergeMerchantsDialog(
    merchants: List<String>,
    targetName: String,
    onTargetNameChange: (String) -> Unit,
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var updateExisting by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Merge Merchants") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text("Merge these merchants into one:")
                merchants.forEach { merchant ->
                    Text(
                        text = "• $merchant",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = targetName,
                    onValueChange = onTargetNameChange,
                    label = { Text("Normalized Name") },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        Text("This name will be used for all merged merchants")
                    }
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Checkbox(
                        checked = updateExisting,
                        onCheckedChange = { updateExisting = it }
                    )
                    Text(
                        text = "Update existing transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(updateExisting) }) {
                Text("Merge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
