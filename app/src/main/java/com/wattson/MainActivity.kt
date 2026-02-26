package com.wattson

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wattson.data.repository.AuthRepository
import com.wattson.ui.components.BottomNavigationBar
import com.wattson.ui.navigation.WattsonNavHost
import com.wattson.ui.navigation.WattsonRoute
import com.wattson.ui.theme.WattsonTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for Wattson application.
 * Sets up edge-to-edge display, navigation, and the main composable content.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WattsonTheme {
                WattsonApp(authRepository = authRepository)
            }
        }
    }
}

/**
 * Root composable for the Wattson application.
 * Manages navigation state and bottom navigation visibility.
 */
@Composable
fun WattsonApp(authRepository: AuthRepository) {
    val navController = rememberNavController()

    // Observe login state from AuthRepository
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()

    // Determine start destination based on login state
    val startDestination: WattsonRoute = if (isLoggedIn && currentUser != null) {
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

    // Navigate when login state changes
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            // User logged out, navigate to Auth
            navController.navigate(WattsonRoute.Auth) {
                popUpTo(0) { inclusive = true }
            }
        }
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
            onLogout = {
                // Logout is handled by AuthRepository, which updates isLoggedIn state
                // The LaunchedEffect above will handle navigation
            }
        )
    }
}
