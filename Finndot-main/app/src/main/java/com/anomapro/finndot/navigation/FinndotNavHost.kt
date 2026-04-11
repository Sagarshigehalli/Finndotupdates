package com.anomapro.finndot.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.anomapro.finndot.ui.MainScreen
import com.anomapro.finndot.BuildConfig
import com.anomapro.finndot.ui.screens.onboarding.OnboardingScreen
import com.anomapro.finndot.ui.viewmodel.OnboardingViewModel
import com.anomapro.finndot.ui.viewmodel.ThemeViewModel
import androidx.compose.ui.platform.LocalContext

@Composable
fun FinndotNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    startDestination: Any = Home,
    onEditComplete: () -> Unit = {}
) {
    // Use a stable start destination
    val stableStartDestination = remember { startDestination }
    
    NavHost(
        navController = navController,
        startDestination = stableStartDestination,
        modifier = modifier,
        enterTransition = { EnterTransition.None },
        exitTransition = { ExitTransition.None },
        popEnterTransition = { EnterTransition.None },
        popExitTransition = { ExitTransition.None }
    ) {
        composable<Onboarding>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val onboardingViewModel: OnboardingViewModel = hiltViewModel()
            val context = LocalContext.current
            val hasSmsPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED

            LaunchedEffect(Unit) {
                onboardingViewModel.navigateNext.collect {
                    kotlinx.coroutines.delay(100)
                    val nextDestination = if (hasSmsPermission) Home else Permission
                    navController.navigate(nextDestination) {
                        popUpTo(Onboarding) { inclusive = true }
                    }
                }
            }

            val onboardingUiState by onboardingViewModel.uiState.collectAsStateWithLifecycle()
            OnboardingScreen(
                onSignInWithGoogle = { activity -> onboardingViewModel.onSignInWithGoogle(activity) },
                onSkipSignIn = { onboardingViewModel.onSkipSignIn() },
                isGoogleSignInAvailable = BuildConfig.GOOGLE_SIGNIN_AVAILABLE,
                isSigningIn = onboardingUiState.isLoading,
                signInError = onboardingUiState.error
            )
        }
        composable<Permission>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.ui.screens.PermissionScreen(
                onPermissionGranted = {
                    navController.navigate(Home) {
                        popUpTo(Permission) { inclusive = true }
                    }
                }
            )
        }
        composable<Home>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            MainScreen(
                rootNavController = navController
            )
        }
        
        composable<Settings>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.ui.screens.settings.SettingsScreen(
                themeViewModel = themeViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCategories = { navController.navigate(Categories) },
                onNavigateToUnrecognizedSms = { navController.navigate(UnrecognizedSms) },
                onNavigateToManageAccounts = { },
                onNavigateToRules = { navController.navigate(Rules) },
                onNavigateToProfile = { },
                onLogout = {
                    navController.navigate(Onboarding) {
                        popUpTo(Home) { inclusive = true }
                    }
                }
            )
        }
        
        composable<Categories>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.presentation.categories.CategoriesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<TransactionDetail>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val transactionDetail = backStackEntry.toRoute<TransactionDetail>()
            com.anomapro.finndot.presentation.transactions.TransactionDetailScreen(
                transactionId = transactionDetail.transactionId,
                onNavigateBack = {
                    onEditComplete()
                    navController.popBackStack()
                }
            )
        }
        
        composable<AddTransaction>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.presentation.add.AddScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<UnrecognizedSms>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.ui.screens.unrecognized.UnrecognizedSmsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Faq>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.ui.screens.settings.FAQScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<Rules>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            com.anomapro.finndot.ui.screens.rules.RulesScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToCreateRule = {
                    navController.navigate(CreateRule)
                }
            )
        }

        composable<CreateRule>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            val rulesViewModel: com.anomapro.finndot.ui.viewmodel.RulesViewModel = hiltViewModel()
            com.anomapro.finndot.ui.screens.rules.CreateRuleScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveRule = { rule ->
                    rulesViewModel.createRule(rule)
                    navController.popBackStack()
                }
            )
        }
        
        composable<AccountDetail>(
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) { backStackEntry ->
            val accountDetail = backStackEntry.toRoute<AccountDetail>()
            com.anomapro.finndot.presentation.accounts.AccountDetailScreen(
                navController = navController
            )
        }
        
    }
}