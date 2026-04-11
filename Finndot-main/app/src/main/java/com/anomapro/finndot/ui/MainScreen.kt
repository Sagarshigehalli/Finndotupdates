package com.anomapro.finndot.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.anomapro.finndot.presentation.home.HomeScreen
import com.anomapro.finndot.presentation.subscriptions.SubscriptionsScreen
import com.anomapro.finndot.presentation.transactions.TransactionsScreen
import com.anomapro.finndot.ui.components.FinndotBottomNavigation
import com.anomapro.finndot.ui.components.SpotlightTutorial
import com.anomapro.finndot.ui.screens.settings.SettingsScreen
import com.anomapro.finndot.ui.viewmodel.ThemeViewModel
import com.anomapro.finndot.ui.viewmodel.SpotlightViewModel
import com.anomapro.finndot.ui.viewmodel.ScreenAnalyticsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    rootNavController: NavHostController? = null,
    navController: NavHostController = rememberNavController(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
    spotlightViewModel: SpotlightViewModel = hiltViewModel(),
    screenAnalyticsViewModel: ScreenAnalyticsViewModel = hiltViewModel()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val spotlightState by spotlightViewModel.spotlightState.collectAsState()

    LaunchedEffect(currentRoute) {
        currentRoute?.let { route ->
            val screenId = route.substringBefore("?")
            if (screenId.isNotBlank()) {
                screenAnalyticsViewModel.recordScreenVisit(screenId)
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
            topBar = {
            FinndotTopAppBar(
                title = when (currentRoute) {
                    "home" -> "Finndot"
                    "merchants" -> "Merchants"
                    "transactions" -> "Transactions"
                    "subscriptions" -> "Subscriptions"
                    "analytics" -> "Analytics"
                    "loans" -> "Loans"
//                    "investments" -> "Investments"
                    "chat" -> "FinnDot AI"
                    "settings" -> "Settings"
                    "categories" -> "Categories"
                    "unrecognized_sms" -> "Unrecognized Messages"
                    "manage_accounts" -> "Manage Accounts"
                    "merchant_mapping" -> "Merchant Mapping"
                    "add_account" -> "Add Account"
                    "faq" -> "Help & FAQ"
                    "budget_settings" -> "Budget Settings"
                    else -> "Finndot"
                },
                showBackButton = currentRoute in listOf("chat", "settings", "subscriptions", "transactions", "categories", "unrecognized_sms", "manage_accounts", "add_account", "faq", "budget_settings", "user_profile", "merchant_mapping"),
                showSettingsButton = currentRoute !in listOf("settings", "categories", "unrecognized_sms", "manage_accounts", "add_account", "faq", "user_profile"),
                onBackClick = { navController.popBackStack() },
                onSettingsClick = { navController.navigate("settings") }
            )
        },
        bottomBar = {
            // Show bottom navigation only for main screens
            if (currentRoute in listOf("home", "merchants", "analytics", "loans", "chat")) {
                FinndotBottomNavigation(navController = navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("home") {
                val homeViewModel: com.anomapro.finndot.presentation.home.HomeViewModel = hiltViewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    navController = rootNavController ?: navController,
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToTransactions = {
                        navController.navigate("transactions")
                    },
                    onNavigateToFilteredTransactions = { category, transactionType, period, currency ->
                        val route = buildString {
                            append("transactions")
                            val params = mutableListOf<String>()
                            category?.let {
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("category=$encoded")
                            }
                            transactionType?.let { params.add("transactionType=$it") }
                            period?.let { params.add("period=$it") }
                            currency?.let { params.add("currency=$it") }
                            if (params.isNotEmpty()) {
                                append("?")
                                append(params.joinToString("&"))
                            }
                        }
                        navController.navigate(route)
                    },
                    onNavigateToTransactionsWithSearch = {
                        navController.navigate("transactions?focusSearch=true")
                    },
                    onNavigateToSubscriptions = {
                        navController.navigate("subscriptions")
                    },
                    onNavigateToAddScreen = {
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.AddTransaction
                        )
                    },
                    onNavigateToBudgetSettings = {
                        navController.navigate("budget_settings")
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.TransactionDetail(transactionId)
                        )
                    },
                    onFabPositioned = { position ->
                        spotlightViewModel.updateFabPosition(position)
                    }
                )
            }
            
            composable(
                route = "transactions?category={category}&merchant={merchant}&period={period}&currency={currency}&transactionType={transactionType}&focusSearch={focusSearch}",
                arguments = listOf(
                    navArgument("category") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("merchant") { 
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("period") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("currency") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("transactionType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("focusSearch") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category")
                val merchant = backStackEntry.arguments?.getString("merchant")
                val period = backStackEntry.arguments?.getString("period")
                val currency = backStackEntry.arguments?.getString("currency")
                val transactionType = backStackEntry.arguments?.getString("transactionType")
                val focusSearch = backStackEntry.arguments?.getBoolean("focusSearch") ?: false
                
                TransactionsScreen(
                    initialCategory = category,
                    initialMerchant = merchant,
                    initialPeriod = period,
                    initialCurrency = currency,
                    initialTransactionType = transactionType,
                    focusSearch = focusSearch,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onTransactionClick = { transactionId ->
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.TransactionDetail(transactionId)
                        )
                    },
                    onAddTransactionClick = {
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.AddTransaction
                        )
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("subscriptions") {
                SubscriptionsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onAddSubscriptionClick = {
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.AddTransaction
                        )
                    }
                )
            }
            
            composable("merchants") {
                com.anomapro.finndot.presentation.merchants.MerchantsScreen(
                    onNavigateToTransactions = { merchant ->
                        val encoded = java.net.URLEncoder.encode(merchant, "UTF-8")
                        navController.navigate("transactions?merchant=$encoded")
                    },
                    onNavigateToMapping = {
                        navController.navigate("merchant_mapping")
                    }
                )
            }
            
            composable("merchant_mapping") {
                com.anomapro.finndot.presentation.merchants.MerchantMappingScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("analytics") {
                com.anomapro.finndot.ui.screens.analytics.AnalyticsScreen(
                    onNavigateToChat = { navController.navigate("chat") },
                    onNavigateToSubscriptions = { navController.navigate("subscriptions") },
                    onNavigateToTransactions = { category, merchant, period, currency ->
                        val route = buildString {
                            append("transactions")
                            val params = mutableListOf<String>()
                            category?.let {
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("category=$encoded")
                            }
                            merchant?.let {
                                val encoded = java.net.URLEncoder.encode(it, "UTF-8")
                                params.add("merchant=$encoded")
                            }
                            period?.let {
                                params.add("period=$it")
                            }
                            currency?.let {
                                params.add("currency=$it")
                            }
                            if (params.isNotEmpty()) {
                                append("?")
                                append(params.joinToString("&"))
                            }
                        }
                        navController.navigate(route)
                    }
                )
            }
            
            composable("loans") {
                com.anomapro.finndot.ui.screens.analytics.LoansScreen()
            }

//            composable("investments") {
//                com.anomapro.finndot.ui.screens.investments.InvestmentsScreen()
//            }
            
            composable("chat") {
                com.anomapro.finndot.ui.screens.chat.ChatScreen(
                    modifier = Modifier.imePadding(),
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToCategories = {
                        navController.navigate("categories")
                    },
                    onNavigateToUnrecognizedSms = {
                        navController.navigate("unrecognized_sms")
                    },
                    onNavigateToManageAccounts = {
                        navController.navigate("manage_accounts")
                    },
                    onNavigateToRules = {
                        rootNavController?.navigate(
                            com.anomapro.finndot.navigation.Rules
                        )
                    },
                    onNavigateToProfile = {
                        navController.navigate("user_profile")
                    },
                    onLogout = {
                        rootNavController?.navigate(com.anomapro.finndot.navigation.Onboarding) {
                            popUpTo(com.anomapro.finndot.navigation.Home) { inclusive = true }
                        }
                    }
                )
            }
            
            composable("user_profile") {
                com.anomapro.finndot.ui.screens.settings.UserProfileScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("categories") {
                com.anomapro.finndot.presentation.categories.CategoriesScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("unrecognized_sms") {
                com.anomapro.finndot.ui.screens.unrecognized.UnrecognizedSmsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("faq") {
                com.anomapro.finndot.ui.screens.settings.FAQScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("manage_accounts") {
                com.anomapro.finndot.presentation.accounts.ManageAccountsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onNavigateToAddAccount = {
                        navController.navigate("add_account")
                    }
                )
            }
            
            composable("add_account") {
                com.anomapro.finndot.presentation.accounts.AddAccountScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable("budget_settings") {
                com.anomapro.finndot.presentation.budget.BudgetSettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
    
    // Spotlight Tutorial overlay - outside Scaffold to overlay everything
    if (currentRoute == "home" && spotlightState.showTutorial && spotlightState.fabPosition != null) {
        val homeViewModel: com.anomapro.finndot.presentation.home.HomeViewModel? = 
            navController.currentBackStackEntry?.let { hiltViewModel(it) }
        
        SpotlightTutorial(
            isVisible = true,
            targetPosition = spotlightState.fabPosition,
            message = "Tap here to scan your SMS messages for transactions",
            onDismiss = {
                spotlightViewModel.dismissTutorial()
            },
            onTargetClick = {
                homeViewModel?.scanSmsMessages()
            }
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FinndotTopAppBar(
    title: String,
    showBackButton: Boolean = false,
    showSettingsButton: Boolean = true,
    onBackClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    Column {
        TopAppBar(
            title = { Text(title) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
            ),
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            },
            actions = {
                if (showSettingsButton) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            }
        )
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    }
}