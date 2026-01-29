package com.wattson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wattson.ui.components.BottomNavigationBar
import com.wattson.ui.navigation.WattsonNavHost
import com.wattson.ui.navigation.WattsonRoute
import com.wattson.ui.navigation.isMainTab
import com.wattson.ui.theme.WattsonTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for Wattson application.
 * Sets up edge-to-edge display, navigation, and the main composable content.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            WattsonTheme {
                WattsonApp()
            }
        }
    }
}

/**
 * Root composable for the Wattson application.
 * Manages navigation state and bottom navigation visibility.
 */
@Composable
fun WattsonApp() {
    val navController = rememberNavController()
    
    // Force login to bypass auth screen as requested
    var isLoggedIn by rememberSaveable { mutableStateOf(true) }
    
    // Determine start destination based on login state
    val startDestination: WattsonRoute = if (isLoggedIn) {
        WattsonRoute.History
    } else {
        WattsonRoute.Auth
    }
    
    // Get current route for bottom nav visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // Determine if bottom nav should be shown
    val showBottomNav = remember(currentRoute) {
        currentRoute?.let { route ->
            route.contains("History") ||
            route.contains("Repair") ||
            route.contains("Documents") ||
            route.contains("Account")
        } ?: false
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav && isLoggedIn) {
                BottomNavigationBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        WattsonNavHost(
            navController = navController,
            startDestination = startDestination,
            innerPadding = innerPadding,
            isLoggedIn = isLoggedIn,
            onLogout = { isLoggedIn = false }
        )
    }
    
    // Handle successful authentication
    // In a real app, you would observe an auth state flow
    // For now, we simulate auth success through navigation events
}
