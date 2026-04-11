package com.anomapro.finndot.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home
    )
    
    data object Merchants : BottomNavItem(
        route = "merchants",
        title = "Merchants",
        icon = Icons.Default.Store
    )
    
    data object Analytics : BottomNavItem(
        route = "analytics",
        title = "Analytics",
        icon = Icons.Default.Analytics
    )
    
    data object Chat : BottomNavItem(
        route = "chat",
        title = "Chat",
        icon = Icons.AutoMirrored.Filled.Chat
    )
    
    data object Loans : BottomNavItem(
        route = "loans",
        title = "Loans",
        icon = Icons.Default.AccountBalance
    )

    data object Investments : BottomNavItem(
        route = "investments",
        title = "Investments",
        icon = Icons.Default.TrendingUp
    )
}