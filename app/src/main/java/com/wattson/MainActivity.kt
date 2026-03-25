package com.wattson

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.wattson.data.repository.AuthRepository
import com.wattson.ui.components.BottomNavigationBar
import com.wattson.ui.navigation.isProtectedRoute
import com.wattson.ui.navigation.WattsonNavHost
import com.wattson.ui.navigation.WattsonRoute
import com.wattson.ui.navigation.toBottomBarRoute
import com.wattson.ui.theme.WattsonTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity for Wattson application.
 * Sets up edge-to-edge display, navigation, and the main composable content.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

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
 * Strict auth guard: unauthenticated users are always redirected to Auth.
 */
@Composable
fun WattsonApp(authRepository: AuthRepository) {
    val isLoggedIn by authRepository.isLoggedIn.collectAsState()
    val currentUser by authRepository.currentUser.collectAsState()

    val isAuthenticated = isLoggedIn && currentUser != null
    val startDestination: WattsonRoute = if (isAuthenticated) {
        WattsonRoute.History
    } else {
        WattsonRoute.Auth
    }

    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentBottomBarRoute = remember(currentRoute) { currentRoute.toBottomBarRoute() }
    val showBottomNav = isAuthenticated && currentBottomBarRoute != null

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) {
            navController.navigate(WattsonRoute.Auth) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentRoute, isAuthenticated) {
        if (!isAuthenticated && currentRoute.isProtectedRoute()) {
            navController.navigate(WattsonRoute.Auth) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
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
            isLoggedIn = isAuthenticated
        )
    }
}
