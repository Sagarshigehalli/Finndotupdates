package com.anomapro.finndot.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.anomapro.finndot.ui.theme.rememberDialogPadding
import com.anomapro.finndot.ui.theme.rememberHeroIconSize
import com.anomapro.finndot.worker.OptimizedSmsReaderWorker
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val scanMessages = listOf(
    Pair("No data leaves your device", Icons.Default.Lock),
    Pair("100% local processing", Icons.Default.Shield),
    Pair("Your data never leaves your phone", Icons.Default.Shield),
    Pair("All processing on device", Icons.Default.Lock),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsParsingProgressDialog(
    isVisible: Boolean,
    workInfo: WorkInfo?,
    onDismiss: () -> Unit,
    onCancel: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var messageIndex by remember { mutableStateOf(0) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            while (true) {
                delay(4500)
                messageIndex = (messageIndex + 1) % scanMessages.size
            }
        }
    }

    val slowTransition = remember {
        fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 0.95f, animationSpec = tween(800)) togetherWith
            fadeOut(animationSpec = tween(600)) + scaleOut(targetScale = 0.95f, animationSpec = tween(600))
    }

    val dialogPadding = rememberDialogPadding()
    val heroIconSize = rememberHeroIconSize()
    if (isVisible) {
        Dialog(
            onDismissRequest = { },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = false,
                dismissOnClickOutside = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(dialogPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier.weight(2f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        AnimatedContent(
                            targetState = messageIndex,
                            transitionSpec = { slowTransition },
                            label = "scanMessage"
                        ) { index ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = scanMessages[index].second,
                                    contentDescription = scanMessages[index].first,
                                    modifier = Modifier.size(heroIconSize),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    text = scanMessages[index].first,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "SMS is read and processed locally. Nothing is sent to any server.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (workInfo != null) {
                                ProgressDetails(workInfo = workInfo)
                                if (workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0) > 0) {
                                    val progress = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0).toFloat() /
                                            workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 1).toFloat()
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier.fillMaxWidth(0.8f).height(8.dp),
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            } else {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                Text(
                                    text = "Preparing local scan...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        // Action button always visible at bottom (not inside scroll)
                        if (onCancel != null && (workInfo == null || workInfo.state == WorkInfo.State.RUNNING)) {
                            OutlinedButton(
                                onClick = onCancel,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Cancel scan", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Scan")
                            }
                        } else if (workInfo != null && workInfo.state == WorkInfo.State.SUCCEEDED) {
                            Button(onClick = onDismiss, modifier = Modifier.padding(top = 8.dp)) { Text("Done") }
                        } else if (workInfo != null && workInfo.state == WorkInfo.State.FAILED) {
                            TextButton(
                                onClick = onDismiss,
                                modifier = Modifier.padding(top = 8.dp),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) { Text("Close") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanMessageCard(
    message: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = message,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun ProgressDetails(workInfo: WorkInfo) {
    val totalMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0)
    val processedMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0)
    val parsedTransactions = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PARSED, 0)
    val savedTransactions = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_SAVED, 0)
    val timeElapsed = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_TIME_ELAPSED, 0L)
    val estimatedTimeRemaining = workInfo.progress.getLong(OptimizedSmsReaderWorker.PROGRESS_ESTIMATED_TIME_REMAINING, 0L)
    val currentBatch = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_CURRENT_BATCH, 0)
    val totalBatches = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL_BATCHES, 0)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Main progress text
        if (totalMessages > 0) {
            val progressText = if (processedMessages == totalMessages) {
                "All messages processed!"
            } else {
                "Processed $processedMessages of $totalMessages messages"
            }

            Text(
                text = progressText,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }

        // Transaction details
        if (parsedTransactions > 0 || savedTransactions > 0) {
            val detailsText = buildAnnotatedString {
                if (parsedTransactions > 0) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append("$parsedTransactions transactions parsed")
                    }
                }
                if (parsedTransactions > 0 && savedTransactions > 0) {
                    append(" • ")
                }
                if (savedTransactions > 0) {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Medium)) {
                        append("$savedTransactions saved")
                    }
                }
            }

            Text(
                text = detailsText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }

        // Time information
        if (timeElapsed > 0) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time elapsed
                Text(
                    text = formatDuration(timeElapsed),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Estimated time remaining
                if (estimatedTimeRemaining > 0 && workInfo.state == WorkInfo.State.RUNNING) {
                    Text(
                        text = "~${formatDuration(estimatedTimeRemaining)} left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Batch information (for parallel processing)
        if (totalBatches > 1 && workInfo.state == WorkInfo.State.RUNNING) {
            Text(
                text = "Batch $currentBatch of $totalBatches",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Status based on work state
        when (workInfo.state) {
            WorkInfo.State.RUNNING -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Scan completed",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scan completed successfully!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            WorkInfo.State.FAILED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Scan failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scan failed. Please try again.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            WorkInfo.State.CANCELLED -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Scan cancelled",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scan cancelled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                // For ENQUEUED or BLOCKED states
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "Local processing...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}

@Composable
fun SmsParsingProgressIndicator(
    workInfo: WorkInfo?,
    modifier: Modifier = Modifier
) {
    if (workInfo != null && workInfo.state == WorkInfo.State.RUNNING) {
        val totalMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_TOTAL, 0)
        val processedMessages = workInfo.progress.getInt(OptimizedSmsReaderWorker.PROGRESS_PROCESSED, 0)

        if (totalMessages > 0) {
            Column(
                modifier = modifier,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                LinearProgressIndicator(
                    progress = { processedMessages.toFloat() / totalMessages.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "Scanning SMS: $processedMessages/$totalMessages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}