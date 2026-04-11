package com.anomapro.finndot.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.anomapro.finndot.ui.theme.Spacing
import com.anomapro.finndot.ui.theme.rememberContentPadding
import kotlinx.coroutines.delay

private val signInMessages = listOf(
    Pair("We are secure", Icons.Default.Shield),
    Pair("We help you maintain financial discipline", Icons.Default.TrendingUp),
    Pair("Your data stays on your device", Icons.Default.Lock),
)

@Composable
fun OnboardingScreen(
    onSignInWithGoogle: (android.app.Activity) -> Unit,
    onSkipSignIn: () -> Unit,
    isGoogleSignInAvailable: Boolean = true,
    isSigningIn: Boolean = false,
    signInError: String? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var messageIndex by remember { mutableStateOf(0) }

    LaunchedEffect(isSigningIn) {
        if (isSigningIn) {
            while (true) {
                delay(2500)
                messageIndex = (messageIndex + 1) % signInMessages.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Privacy-themed background - abstract shapes conveying "data stays on device"
        PrivacyBackground()

        val contentPadding = rememberContentPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(maxOf(contentPadding, Spacing.md)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(Spacing.md))

            // Hero section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isSigningIn) {
                    SignInProgressContent(
                        message = signInMessages[messageIndex].first,
                        icon = signInMessages[messageIndex].second
                    )
                } else {
                    DevicePrivacyIcon()

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    Text(
                        text = "Your Data, Your Device",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "Your transaction data stays on your device. All processing happens on-device.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(Spacing.sm))

                    Text(
                        text = "Track expenses automatically with complete privacy.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Privacy assurance card
            if (!isSigningIn) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md)
                    ) {
                        Text(
                            text = "Privacy First",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(Spacing.xs))
                        Text(
                            text = "• Transaction data stays on your device\n" +
                                    "• No cloud servers process your data\n" +
                                    "• Complete control over your information",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            // Sign-in error
            if (signInError != null) {
                val error = signInError
                val context = LocalContext.current
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(
                            onClick = {
                                (context as? android.app.Activity)?.let { onSignInWithGoogle(it) }
                            }
                        ) {
                            Text("Retry", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Action buttons
            if (!isSigningIn) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                if (isGoogleSignInAvailable) {
                    val context = LocalContext.current
                    Button(
                        onClick = {
                            (context as? android.app.Activity)?.let { onSignInWithGoogle(it) }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Login,
                            contentDescription = "Sign in",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(Spacing.sm))
                        Text("Sign in with Google")
                    }
                }

                OutlinedButton(
                    onClick = onSkipSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Skip sign in")
                }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun SignInProgressContent(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = message,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Sign in progress",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DevicePrivacyIcon() {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer circle - device boundary
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = primaryContainer.copy(alpha = 0.5f),
                radius = size.minDimension / 2 - 4,
                style = Stroke(width = 4f)
            )
        }
        // Inner - phone + lock
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = "Device",
                modifier = Modifier.size(56.dp),
                tint = primary
            )
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Secure",
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 4.dp),
                tint = primary
            )
        }
    }
}

@Composable
private fun PrivacyBackground() {
    val primaryColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.15f)
    ) {
        val width = size.width
        val height = size.height

        // Abstract shapes suggesting data flow staying within device
        drawCircle(
            color = primaryColor.copy(alpha = 0.2f),
            radius = width * 0.3f,
            center = Offset(width * 0.2f, height * 0.2f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.15f),
            radius = width * 0.25f,
            center = Offset(width * 0.8f, height * 0.3f)
        )
        drawCircle(
            color = primaryColor.copy(alpha = 0.1f),
            radius = width * 0.2f,
            center = Offset(width * 0.5f, height * 0.7f)
        )

        // Subtle path suggesting contained data
        val path = Path().apply {
            moveTo(width * 0.1f, height * 0.5f)
            quadraticBezierTo(
                width * 0.5f, height * 0.3f,
                width * 0.9f, height * 0.5f
            )
            quadraticBezierTo(
                width * 0.5f, height * 0.7f,
                width * 0.1f, height * 0.5f
            )
            close()
        }
        drawPath(
            path = path,
            color = primaryColor.copy(alpha = 0.05f),
            style = Stroke(width = 2f)
        )
    }
}
