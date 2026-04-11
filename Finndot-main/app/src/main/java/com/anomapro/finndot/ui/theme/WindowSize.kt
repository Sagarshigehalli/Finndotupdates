package com.anomapro.finndot.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Breakpoints for responsive layouts.
 * COMPACT: phones (< 600dp)
 * MEDIUM: tablets (600-840dp)
 * EXPANDED: large tablets (> 840dp)
 */
enum class WindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

@Composable
fun rememberWindowSize(): WindowSize {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        when {
            configuration.screenWidthDp < 600 -> WindowSize.COMPACT
            configuration.screenWidthDp < 840 -> WindowSize.MEDIUM
            else -> WindowSize.EXPANDED
        }
    }
}

/**
 * Returns content padding appropriate for the current screen size.
 * Smaller screens get less padding to maximize content area.
 */
@Composable
fun rememberContentPadding(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        when {
            configuration.screenWidthDp < 360 -> 12.dp
            configuration.screenWidthDp < 600 -> 16.dp
            else -> 24.dp
        }
    }
}

/**
 * Returns icon size for hero/empty state illustrations.
 * Smaller on compact screens to prevent overflow.
 */
@Composable
fun rememberHeroIconSize(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        when {
            configuration.screenWidthDp < 360 -> 64.dp
            configuration.screenWidthDp < 600 -> 80.dp
            else -> 96.dp
        }
    }
}

/**
 * Returns horizontal padding for dialogs/full-screen overlays.
 */
@Composable
fun rememberDialogPadding(): Dp {
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        when {
            configuration.screenWidthDp < 360 -> 16.dp
            configuration.screenWidthDp < 600 -> 24.dp
            else -> 32.dp
        }
    }
}
