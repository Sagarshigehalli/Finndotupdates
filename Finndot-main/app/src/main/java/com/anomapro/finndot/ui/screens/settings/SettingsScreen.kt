package com.anomapro.finndot.ui.screens.settings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.LaunchedEffect
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.anomapro.finndot.BuildConfig
import com.anomapro.finndot.core.Constants
import com.anomapro.finndot.ui.components.FinndotCard
import com.anomapro.finndot.ui.components.FinndotScaffold
import com.anomapro.finndot.ui.components.SectionHeader
import com.anomapro.finndot.ui.theme.Dimensions
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.rememberContentPadding
import com.anomapro.finndot.ui.viewmodel.ThemeViewModel
import coil.compose.AsyncImage

private fun formatNotificationTime(hour: Int, minute: Int): String {
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val amPm = if (hour < 12) "AM" else "PM"
    return "$displayHour:${minute.toString().padStart(2, '0')} $amPm"
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("UNUSED_PARAMETER", "UNUSED")
@Composable
fun SettingsScreen(
    themeViewModel: ThemeViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToUnrecognizedSms: () -> Unit = {},
    onNavigateToManageAccounts: () -> Unit = {},
    onNavigateToRules: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {},
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val downloadState by settingsViewModel.downloadState.collectAsStateWithLifecycle()
    val downloadProgress by settingsViewModel.downloadProgress.collectAsStateWithLifecycle()
    val downloadedMB by settingsViewModel.downloadedMB.collectAsStateWithLifecycle()
    val totalMB by settingsViewModel.totalMB.collectAsStateWithLifecycle()
    val smsScanMonths by settingsViewModel.smsScanMonths.collectAsStateWithLifecycle(initialValue = 12)
    val smsScanAllTime by settingsViewModel.smsScanAllTime.collectAsStateWithLifecycle(initialValue = false)
    val importExportMessage by settingsViewModel.importExportMessage.collectAsStateWithLifecycle()
    val exportedBackupFile by settingsViewModel.exportedBackupFile.collectAsStateWithLifecycle()
    var showSmsScanDialog by remember { mutableStateOf(false) }
    var showExportOptionsDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmation by remember { mutableStateOf(false) }
    var showDeleteAllDataConfirmation by remember { mutableStateOf(false) }
    var showSmsExportDialog by remember { mutableStateOf(false) }
    var smsExportProgress by remember { mutableStateOf(0f) }
    var smsExportMessage by remember { mutableStateOf<String?>(null) }
    var smsExportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var smsExportFileName by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val isSigningIn by settingsViewModel.isSigningIn.collectAsStateWithLifecycle()
    val signInError by settingsViewModel.signInError.collectAsStateWithLifecycle()
    val signInSuccessTrigger by settingsViewModel.signInSuccessTrigger.collectAsStateWithLifecycle()
    
    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.importBackup(it)
            }
        }
    )
    
    // File saver for export
    val exportSaveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
        onResult = { uri ->
            uri?.let {
                settingsViewModel.saveBackupToFile(it)
            }
        }
    )
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val contentPadding = rememberContentPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md)
        ) {
            val currentUser = settingsViewModel.getCurrentUser()

            // Sign in with Google - prominent at top when not signed in (logged out or anonymous)
            if ((currentUser == null || currentUser.isAnonymous) && BuildConfig.GOOGLE_SIGNIN_AVAILABLE) {
                FinndotCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isSigningIn) {
                            (context as? android.app.Activity)?.let { activity ->
                                settingsViewModel.signInWithGoogle(activity)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Login,
                                contentDescription = "Sign in",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isSigningIn) "Signing in..." else "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Sync your profile and data across devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!isSigningIn) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // User Profile Section
            SectionHeader(title = "Profile")
            FinndotCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToProfile() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        currentUser?.photoUrl?.let { photoUrl ->
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile photo",
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                            )
                        } ?: Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Column {
                            Text(
                                text = currentUser?.displayName ?: "User Profile",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when {
                                    currentUser?.email != null -> currentUser.email
                                    currentUser?.isAnonymous == true -> "Sign in with Google to sync your profile"
                                    else -> "Name, occupation, income, mobile"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Notifications Section
            SectionHeader(title = "Notifications")
            
            FinndotCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                val dailySummaryNotificationEnabled by settingsViewModel.dailySummaryNotificationEnabled.collectAsStateWithLifecycle(initialValue = true)
                val dailySummaryNotificationHour by settingsViewModel.dailySummaryNotificationHour.collectAsStateWithLifecycle(initialValue = 21)
                val dailySummaryNotificationMinute by settingsViewModel.dailySummaryNotificationMinute.collectAsStateWithLifecycle(initialValue = 0)
                var showTimePicker by remember { mutableStateOf(false) }
                
                Column(
                    modifier = Modifier.padding(Dimensions.Padding.content)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Daily Summary Notification",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Get end-of-day financial summary",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = dailySummaryNotificationEnabled,
                            onCheckedChange = { enabled ->
                                settingsViewModel.updateDailySummaryNotificationEnabled(enabled)
                            }
                        )
                    }
                    AnimatedVisibility(visible = dailySummaryNotificationEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Spacing.md)
                                .clickable { showTimePicker = true },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Notification time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.xs)
                            ) {
                                Text(
                                    text = formatNotificationTime(dailySummaryNotificationHour, dailySummaryNotificationMinute),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (showTimePicker) {
                    val timePickerState = rememberTimePickerState(
                        initialHour = dailySummaryNotificationHour,
                        initialMinute = dailySummaryNotificationMinute
                    )
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        title = { Text("Daily Summary Time") },
                        text = {
                            TimePicker(state = timePickerState)
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                settingsViewModel.updateDailySummaryNotificationTime(
                                    timePickerState.hour,
                                    timePickerState.minute
                                )
                                showTimePicker = false
                            }) {
                                Text("OK")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
            
            // Theme Settings Section
            SectionHeader(title = "Appearance")
        
        FinndotCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Theme Mode Selection
                Column {
                    Text(
                        text = "Theme",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    FilterChip(
                        selected = themeUiState.isDarkTheme == null,
                        onClick = { themeViewModel.updateDarkTheme(null) },
                        label = { Text("System") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == false,
                        onClick = { themeViewModel.updateDarkTheme(false) },
                        label = { Text("Light") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = themeUiState.isDarkTheme == true,
                        onClick = { themeViewModel.updateDarkTheme(true) },
                        label = { Text("Dark") },
                        modifier = Modifier.weight(1f)
                    )
                }
                }
            }
        }
        
        // Data Management Section
        SectionHeader(title = "Data Management")

        // SMS Scan Period (grouped with SMS settings)
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSmsScanDialog = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "SMS Scan Period",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (smsScanAllTime) "Scan all SMS messages" else "Scan last $smsScanMonths months of messages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = if (smsScanAllTime) "All Time" else "$smsScanMonths months",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Permission launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted ->
                // Permission state will be updated by the lifecycle observer
            }
        )

        // Check permission state
        var hasReceiveSmsPermission by remember {
            mutableStateOf(
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECEIVE_SMS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            )
        }

        // Update permission state when resuming
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    hasReceiveSmsPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECEIVE_SMS
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        // Real-time SMS Scanning
        FinndotCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Real-time SMS Scanning",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (hasReceiveSmsPermission) "Enabled" else "Enable to track transactions as they arrive",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = hasReceiveSmsPermission,
                    onCheckedChange = { checked ->
                        if (checked) {
                            permissionLauncher.launch(android.Manifest.permission.RECEIVE_SMS)
                        } else {
                            // Cannot disable permission programmatically, guide user to settings
                            // For now, we can't do much, maybe show a toast
                        }
                    }
                )
            }
        }
        
        // Manage Accounts
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToManageAccounts() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Manage Accounts",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Add manual accounts and update balances",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Categories
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToCategories() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Manage expense and income categories",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Smart Rules
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToRules() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Smart Rules",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Automatic transaction categorization",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Export Data
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { settingsViewModel.exportBackup() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Upload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Export Data",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Backup all data to a file",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Import Data
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    importLauncher.launch("*/*")
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Import Data",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Restore data from backup",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Export SMS Database
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    showSmsExportDialog = true
                    smsExportProgress = 0f
                    smsExportMessage = "Starting export..."
                    smsExportUri = null
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Sms,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Export SMS Database",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Export all SMS data to CSV",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // AI Features Section
        SectionHeader(title = "AI Features")
        
        FinndotCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Chat Assistant",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = when (downloadState) {
                                DownloadState.NOT_DOWNLOADED -> "Download FinnDot model (${Constants.ModelDownload.MODEL_SIZE_MB} MB)"
                                DownloadState.DOWNLOADING -> "Downloading FinnDot model..."
                                DownloadState.PAUSED -> "Download interrupted"
                                DownloadState.COMPLETED -> "Finndot ready for chat"
                                DownloadState.FAILED -> "Download failed"
                                DownloadState.ERROR_INSUFFICIENT_SPACE -> "Not enough storage space"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    // Action area based on state
                    when (downloadState) {
                        DownloadState.NOT_DOWNLOADED -> {
                            Button(
                                onClick = { settingsViewModel.startModelDownload() }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Download")
                            }
                        }
                        DownloadState.DOWNLOADING -> {
                            Text(
                                text = "$downloadProgress%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        DownloadState.PAUSED -> {
                            Button(
                                onClick = { settingsViewModel.startModelDownload() }
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Retry")
                            }
                        }
                        DownloadState.COMPLETED -> {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                TextButton(
                                    onClick = { settingsViewModel.deleteModel() }
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                        DownloadState.FAILED -> {
                            Button(
                                onClick = { settingsViewModel.startModelDownload() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(Spacing.xs))
                                Text("Retry")
                            }
                        }
                        DownloadState.ERROR_INSUFFICIENT_SPACE -> {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                
                // Progress details during download
                AnimatedVisibility(
                    visible = downloadState == DownloadState.DOWNLOADING,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        LinearProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$downloadedMB MB / $totalMB MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { settingsViewModel.cancelDownload() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Cancel, contentDescription = null)
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text("Cancel Download")
                        }
                    }
                }
                
                // Info about AI features
                if (downloadState == DownloadState.NOT_DOWNLOADED || 
                    downloadState == DownloadState.ERROR_INSUFFICIENT_SPACE) {
                    HorizontalDivider()
                    Text(
                        text = "Chat with Finndot AI about your expenses and get financial insights. " +
                              "All conversations stay private on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Help Improve Finndot Section
        SectionHeader(title = "Help Improve Finndot")
        
        // Send Feedback
        FinndotCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { 
                    settingsViewModel.sendFeedback(context)
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimensions.Padding.content),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Feedback,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "Send Feedback",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Share your thoughts and suggestions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Unrecognized Messages Section (only show if count > 0)
        val unreportedCount by settingsViewModel.unreportedSmsCount.collectAsStateWithLifecycle()
        
        if (unreportedCount > 0) {
            FinndotCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    Log.d("SettingsScreen", "Navigating to UnrecognizedSms screen")
                    onNavigateToUnrecognizedSms() 
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unrecognized Bank Messages",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$unreportedCount message${if (unreportedCount > 1) "s" else ""} from potential banks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primary
                        ) {
                            Text(unreportedCount.toString())
                        }
                        
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "View Messages",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Contact for Partnerships Section
        SectionHeader(title = "Contact for Partnerships")
        
        FinndotCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(Dimensions.Padding.content),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                // Akshay
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            settingsViewModel.sendPartnershipEmail(context, "akshay@anomapro.com")
                        },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "Akshay",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "akshay@anomapro.com",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "Seeking partners in investment products and unsecured lending.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
            // Account Section - Log out and Delete all data at end
            SectionHeader(title = "Account")

            if (currentUser != null && !currentUser.isAnonymous) {
                FinndotCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLogoutConfirmation = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimensions.Padding.content),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log out",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Log out",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            FinndotCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDeleteAllDataConfirmation = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimensions.Padding.content),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.DeleteForever,
                        contentDescription = "Delete all data",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Delete all data",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Clear app data. If signed in, account information saved will be deleted and can't be retrieved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
    }

    // Delete all data confirmation dialog
    if (showDeleteAllDataConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDataConfirmation = false },
            title = { Text("Delete all data") },
            text = {
                Text(
                    "This will permanently delete all your transactions, accounts, and app data. " +
                    "If signed in, your account information saved will be deleted and can't be retrieved. " +
                    "You will be signed out. This cannot be undone."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.deleteAllDataAndMarkAccountDeleted(onComplete = onLogout)
                        showDeleteAllDataConfirmation = false
                    }
                ) {
                    Text("Delete all", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDataConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Sign-in error dialog
    signInError?.let { error ->
        AlertDialog(
            onDismissRequest = { settingsViewModel.clearSignInError() },
            title = { Text("Sign in failed") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { settingsViewModel.clearSignInError() }) {
                    Text("OK")
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutConfirmation) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirmation = false },
            title = { Text("Log out") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.signOut(onComplete = onLogout)
                        showLogoutConfirmation = false
                    }
                ) {
                    Text("Log out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // SMS Scan Period Dialog
    if (showSmsScanDialog) {
        AlertDialog(
            onDismissRequest = { showSmsScanDialog = false },
            title = { Text("SMS Scan Period") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    Text(
                        text = "Choose how many months of SMS history to scan for transactions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    // All Time option first, then period options including 24 months for 2 years coverage
                    val options = listOf(-1) + listOf(1, 2, 3, 6, 12, 24)
                    options.forEach { months ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (months == -1) {
                                        settingsViewModel.updateSmsScanAllTime(true)
                                        showSmsScanDialog = false
                                    } else {
                                        settingsViewModel.updateSmsScanMonths(months)
                                        settingsViewModel.updateSmsScanAllTime(false)
                                        showSmsScanDialog = false
                                    }
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isSelected = if (months == -1) smsScanAllTime else smsScanMonths == months && !smsScanAllTime
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (months == -1) {
                                        settingsViewModel.updateSmsScanAllTime(true)
                                        showSmsScanDialog = false
                                    } else {
                                        settingsViewModel.updateSmsScanMonths(months)
                                        settingsViewModel.updateSmsScanAllTime(false)
                                        showSmsScanDialog = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(Spacing.md))
                            Text(
                                text = when(months) {
                                    -1 -> "All Time"
                                    1 -> "1 month"
                                    24 -> "2 years"
                                    else -> "$months months"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSmsScanDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Show import/export message
    importExportMessage?.let { message ->
        // Check if we have an exported file ready
        if (exportedBackupFile != null && message.contains("successfully! Choose")) {
            showExportOptionsDialog = true
        } else {
            LaunchedEffect(message) {
                // Auto-clear message after 5 seconds
                kotlinx.coroutines.delay(5000)
                settingsViewModel.clearImportExportMessage()
            }
            
            AlertDialog(
                onDismissRequest = { settingsViewModel.clearImportExportMessage() },
                title = { Text("Backup Status") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.clearImportExportMessage() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
    
    // Export options dialog
    if (showExportOptionsDialog && exportedBackupFile != null) {
        val timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HHmmss")
        )
        val fileName = "Finndot_Backup_$timestamp.finndotbackup"
        
        AlertDialog(
            onDismissRequest = { 
                showExportOptionsDialog = false
                settingsViewModel.clearImportExportMessage()
            },
            title = { Text("Save Backup") },
            text = { 
                Column {
                    Text("Backup created successfully!")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Choose how you want to save it:", style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = { 
                            exportSaveLauncher.launch(fileName)
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save to Files")
                    }
                    
                    TextButton(
                        onClick = { 
                            settingsViewModel.shareBackup()
                            showExportOptionsDialog = false
                            settingsViewModel.clearImportExportMessage()
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showExportOptionsDialog = false
                        settingsViewModel.clearImportExportMessage()
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // SMS Export Dialog and Flow Collection
    LaunchedEffect(showSmsExportDialog) {
        if (showSmsExportDialog) {
            settingsViewModel.exportSmsDatabase().collect { result ->
                when (result) {
                    is com.anomapro.finndot.data.export.ExportResult.Progress -> {
                        smsExportProgress = result.progress
                        smsExportMessage = result.message
                    }
                    is com.anomapro.finndot.data.export.ExportResult.Success -> {
                        smsExportProgress = 1.0f
                        smsExportMessage = "Export complete! ${result.transactionCount} SMS entries exported."
                        smsExportUri = result.uri
                        smsExportFileName = result.fileName
                    }
                    is com.anomapro.finndot.data.export.ExportResult.Error -> {
                        smsExportProgress = 0f
                        smsExportMessage = "Export failed: ${result.message}"
                        smsExportUri = null
                    }
                }
            }
        }
    }
    
    // SMS Export Progress/Success Dialog
    if (showSmsExportDialog) {
        AlertDialog(
            onDismissRequest = { 
                if (smsExportUri != null) {
                    // Don't dismiss if export is successful, show share options
                } else {
                    showSmsExportDialog = false
                    smsExportMessage = null
                    smsExportProgress = 0f
                }
            },
            title = { Text("Export SMS Database") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    if (smsExportMessage != null) {
                        Text(smsExportMessage!!)
                    }
                    if (smsExportProgress < 1.0f && smsExportUri == null) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        LinearProgressIndicator(
                            progress = { smsExportProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (smsExportUri != null) {
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            "File: $smsExportFileName",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                if (smsExportUri != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        TextButton(
                            onClick = {
                                // Share the file
                                smsExportUri?.let { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Finndot SMS Database Export")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share SMS Database"))
                                }
                                showSmsExportDialog = false
                                smsExportMessage = null
                                smsExportProgress = 0f
                                smsExportUri = null
                            }
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        TextButton(
                            onClick = {
                                showSmsExportDialog = false
                                smsExportMessage = null
                                smsExportProgress = 0f
                                smsExportUri = null
                            }
                        ) {
                            Text("Done")
                        }
                    }
                } else {
                    TextButton(
                        onClick = {
                            showSmsExportDialog = false
                            smsExportMessage = null
                            smsExportProgress = 0f
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}