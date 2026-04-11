package com.anomapro.finndot

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.anomapro.finndot.navigation.Home
import com.anomapro.finndot.navigation.Onboarding
import com.anomapro.finndot.navigation.FinndotNavHost
import com.anomapro.finndot.ui.theme.FinndotTheme
import com.anomapro.finndot.ui.viewmodel.AppViewModel
import com.anomapro.finndot.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

@Suppress("IMPLICIT_CAST_TO_ANY")
@Composable
fun FinndotApp(
    themeViewModel: ThemeViewModel = hiltViewModel(),
    appViewModel: AppViewModel = hiltViewModel(),
    editTransactionId: Long? = null,
    onEditComplete: () -> Unit = {}
) {
    val themeUiState by themeViewModel.themeUiState.collectAsStateWithLifecycle()
    val startDestinationReady by appViewModel.startDestinationReady.collectAsStateWithLifecycle()

    val darkTheme = themeUiState.isDarkTheme ?: isSystemInDarkTheme()

    val navController = rememberNavController()

    FinndotTheme(
        darkTheme = darkTheme,
        dynamicColor = themeUiState.isDynamicColorEnabled
    ) {
        when (val ready = startDestinationReady) {
            null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> {
                val startDestination = if (ready) Home else Onboarding
                FinndotNavHost(
                    navController = navController,
                    themeViewModel = themeViewModel,
                    startDestination = startDestination,
                    onEditComplete = onEditComplete
                )

                // Wait until the nav graph has produced a back stack entry before navigating.
                LaunchedEffect(editTransactionId, ready) {
                    editTransactionId?.let { transactionId ->
                        navController.currentBackStackEntryFlow
                            .filterNotNull()
                            .first()

                        navController.navigate(com.anomapro.finndot.navigation.TransactionDetail(transactionId)) {
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}