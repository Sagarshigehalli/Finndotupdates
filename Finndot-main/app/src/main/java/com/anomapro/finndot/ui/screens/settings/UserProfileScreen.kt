package com.anomapro.finndot.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.components.SectionHeader
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.rememberContentPadding
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER")
@Composable
fun UserProfileScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf(uiState.name ?: "") }
    var occupation by remember { mutableStateOf(uiState.occupation ?: "") }
    var hoursWorked by remember { mutableStateOf(uiState.hoursWorkedPerMonth?.toString() ?: "") }
    var avgIncome by remember { mutableStateOf(uiState.avgIncome?.toString() ?: "") }
    var mobileNumber by remember { mutableStateOf(uiState.mobileNumber ?: "") }
    
    // Update local state when UI state changes
    LaunchedEffect(uiState) {
        name = uiState.name ?: ""
        occupation = uiState.occupation ?: ""
        hoursWorked = uiState.hoursWorkedPerMonth?.toString() ?: ""
        avgIncome = uiState.avgIncome?.toString() ?: ""
        mobileNumber = uiState.mobileNumber ?: ""
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState.saveMessage) {
        uiState.saveMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSaveMessage()
        }
    }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
        val contentPadding = rememberContentPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            SectionHeader(title = "User Profile")

            uiState.signedInUser?.let { signedInUser ->
                FinndotCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        signedInUser.photoUrl?.let { photoUrl ->
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                            )
                        } ?: Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Column {
                            Text(
                                text = signedInUser.displayName ?: "Signed in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            signedInUser.email?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xs))
            }
            
            FinndotCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        placeholder = { Text("Enter your name") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        )
                    )
                    
                    // Occupation Input
                    OutlinedTextField(
                        value = occupation,
                        onValueChange = { occupation = it },
                        label = { Text("Occupation") },
                        placeholder = { Text("Enter your occupation") },
                        leadingIcon = {
                            Icon(Icons.Default.Business, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        )
                    )
                    
                    // Hours Worked Per Month Input
                    OutlinedTextField(
                        value = hoursWorked,
                        onValueChange = { text ->
                            if (text.isEmpty() || text.all { it.isDigit() }) {
                                hoursWorked = text
                            }
                        },
                        label = { Text("Hours Worked Per Month") },
                        placeholder = { Text("40") },
                        leadingIcon = {
                            Icon(Icons.Default.Schedule, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    
                    // Average Income Input
                    OutlinedTextField(
                        value = avgIncome,
                        onValueChange = { text ->
                            if (text.isEmpty() || text.all { it.isDigit() }) {
                                avgIncome = text
                            }
                        },
                        label = { Text("Average Income") },
                        placeholder = { Text("30000") },
                        leadingIcon = {
                            Icon(Icons.Default.CurrencyRupee, contentDescription = null)
                        },
                        supportingText = {
                            Text("Monthly income in your base currency")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    
                    // Mobile Number Input
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = { Text("Mobile Number") },
                        placeholder = { Text("Enter your mobile number") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    
                    // Save Button
                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                name = name.takeIf { it.isNotBlank() },
                                occupation = occupation.takeIf { it.isNotBlank() },
                                hoursWorkedPerMonth = hoursWorked.toIntOrNull(),
                                avgIncome = avgIncome.toLongOrNull(),
                                mobileNumber = mobileNumber.takeIf { it.isNotBlank() }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(Spacing.xs))
                        Text("Save Profile")
                    }
                }
            }
        }
        }
    }
}

