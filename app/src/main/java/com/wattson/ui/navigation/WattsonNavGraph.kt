package com.wattson.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.wattson.domain.model.AuthProvider
import com.wattson.ui.screens.account.AccountViewModel
import com.wattson.ui.screens.auth.AuthIntent
import com.wattson.ui.screens.auth.AuthViewModel
import com.wattson.ui.screens.documents.DocumentsViewModel
import com.wattson.ui.screens.history.HistoryIntent
import com.wattson.ui.screens.history.HistoryViewModel
import com.wattson.ui.screens.premium.PremiumViewModel
import com.wattson.ui.screens.scan.ScanIntent
import com.wattson.ui.screens.scan.ScanViewModel
import com.wattson.ui.screens.account.AccountScreen
import com.wattson.ui.screens.auth.AuthScreen
import com.wattson.ui.screens.auth.LoginScreen
import com.wattson.ui.screens.auth.RegisterScreen
import com.wattson.ui.screens.documents.DocumentsScreen
import com.wattson.ui.screens.documents.detail.DocumentDetailScreen
import com.wattson.ui.screens.history.HistoryScreen
import com.wattson.ui.screens.premium.PremiumScreen
import com.wattson.ui.screens.product.ProductDetailScreen
import com.wattson.ui.screens.repair.RepairScreen
import com.wattson.ui.screens.repair.chat.RepairChatScreen
import com.wattson.ui.screens.scan.ScanScreen

private const val TRANSITION_DURATION = 300

/**
 * Main navigation host for the Wattson application.
 *
 * @param navController The navigation controller
 * @param startDestination The starting route (Auth or History based on login state)
 * @param innerPadding Padding from scaffold
 * @param isLoggedIn Whether user is authenticated
 * @param onLogout Callback for logout action
 */
@Composable
fun WattsonNavHost(
    navController: NavHostController,
    startDestination: WattsonRoute,
    innerPadding: PaddingValues,
    isLoggedIn: Boolean,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.padding(innerPadding),
        enterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(TRANSITION_DURATION)
                    )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_DURATION)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(TRANSITION_DURATION)
                    )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(TRANSITION_DURATION)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(TRANSITION_DURATION)
                    )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(TRANSITION_DURATION)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(TRANSITION_DURATION)
                    )
        }
    ) {
        // ===== AUTHENTICATION SCREENS =====
        composable<WattsonRoute.Auth> {
            AuthScreen(
                onNavigateToLogin = { 
                    navController.navigate(WattsonRoute.Login) 
                },
                onNavigateToRegister = { 
                    navController.navigate(WattsonRoute.Register) 
                },
                onLoginWithGoogle = { /* Handle Google OAuth */ },
                onLoginWithApple = { /* Handle Apple OAuth */ }
            )
        }
        
        composable<WattsonRoute.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Handle auth events
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is com.wattson.ui.screens.auth.AuthEvent.NavigateToMain -> {
                            navController.navigate(WattsonRoute.History) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        is com.wattson.ui.screens.auth.AuthEvent.NavigateToRegister -> {
                            navController.navigate(WattsonRoute.Register)
                        }
                        else -> { /* Handle other events */ }
                    }
                }
            }

            LoginScreen(
                uiState = uiState,
                onEmailChange = { viewModel.onIntent(AuthIntent.UpdateEmail(it)) },
                onPasswordChange = { viewModel.onIntent(AuthIntent.UpdatePassword(it)) },
                onTogglePasswordVisibility = { viewModel.onIntent(AuthIntent.TogglePasswordVisibility()) },
                onToggleRememberMe = { viewModel.onIntent(AuthIntent.ToggleRememberMe(it)) },
                onLogin = { viewModel.performLogin() },
                onNavigateBack = { navController.popBackStack() },
                onForgotPassword = { viewModel.handleForgotPassword() },
                onLoginWithGoogle = { viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.GOOGLE)) },
                onLoginWithApple = { viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.APPLE)) },
                onNavigateToRegister = {
                    navController.navigate(WattsonRoute.Register)
                },
                onAutoFillTestUser = { viewModel.onIntent(AuthIntent.AutoFillTestUser) }
            )
        }
        
        composable<WattsonRoute.Register> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Handle auth events
            androidx.compose.runtime.LaunchedEffect(Unit) {
                viewModel.events.collect { event ->
                    when (event) {
                        is com.wattson.ui.screens.auth.AuthEvent.NavigateToMain -> {
                            navController.navigate(WattsonRoute.History) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                        is com.wattson.ui.screens.auth.AuthEvent.NavigateToLogin -> {
                            navController.navigate(WattsonRoute.Login)
                        }
                        else -> { /* Handle other events */ }
                    }
                }
            }

            RegisterScreen(
                uiState = uiState,
                onEmailChange = { viewModel.onIntent(AuthIntent.UpdateEmail(it)) },
                onPasswordChange = { viewModel.onIntent(AuthIntent.UpdatePassword(it)) },
                onConfirmPasswordChange = { viewModel.onIntent(AuthIntent.UpdateConfirmPassword(it)) },
                onTogglePasswordVisibility = { viewModel.onIntent(AuthIntent.TogglePasswordVisibility()) },
                onToggleConfirmPasswordVisibility = { viewModel.onIntent(AuthIntent.TogglePasswordVisibility(true)) },
                onRegister = { viewModel.performRegister() },
                onNavigateBack = { navController.popBackStack() },
                onLoginWithGoogle = { viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.GOOGLE)) },
                onLoginWithApple = { viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.APPLE)) },
                onNavigateToLogin = {
                    navController.navigate(WattsonRoute.Login)
                }
            )
        }

        // ===== MAIN TABS (all require authentication) =====

        composable<WattsonRoute.History> {
            // Auth guard: redirect to Auth if not logged in
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val viewModel: HistoryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            // Refresh history when screen becomes active (e.g., after returning from ProductDetail)
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        viewModel.onIntent(HistoryIntent.RefreshHistory)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            HistoryScreen(
                uiState = uiState,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onClearSearch = { viewModel.updateSearchQuery("") },
                onProductClick = { productId ->
                    navController.navigate(WattsonRoute.ProductDetail(productId))
                },
                onScanClick = {
                    navController.navigate(WattsonRoute.Scan)
                }
            )
        }
        
        composable<WattsonRoute.Repair> {
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            RepairScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(WattsonRoute.RepairChat(conversationId))
                }
            )
        }
        
        composable<WattsonRoute.Scan> {
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val viewModel: ScanViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            ScanScreen(
                uiState = uiState,
                events = viewModel.events,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProduct = { productId ->
                    navController.navigate(WattsonRoute.ProductDetail(productId)) {
                        popUpTo(WattsonRoute.History)
                    }
                }
            )
        }
        
        composable<WattsonRoute.Documents> {
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val viewModel: DocumentsViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            DocumentsScreen(
                uiState = uiState,
                events = viewModel.events,
                onIntent = viewModel::onIntent,
                onNavigateToDocument = { documentId ->
                    navController.navigate(WattsonRoute.DocumentDetail(documentId))
                },
                onNavigateToPremium = {
                    navController.navigate(WattsonRoute.Premium)
                }
            )
        }
        
        composable<WattsonRoute.Account> {
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val viewModel: AccountViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            AccountScreen(
                uiState = uiState,
                events = viewModel.events,
                onIntent = viewModel::onIntent,
                onNavigateToPremium = {
                    navController.navigate(WattsonRoute.Premium)
                },
                onNavigateToLogin = {
                    onLogout()
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ===== DETAIL SCREENS =====
        
        composable<WattsonRoute.ProductDetail> { backStackEntry ->
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val productDetail: WattsonRoute.ProductDetail = backStackEntry.toRoute()
            ProductDetailScreen(
                productId = productDetail.productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<WattsonRoute.Premium> {
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val viewModel: PremiumViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            PremiumScreen(
                uiState = uiState,
                events = viewModel.events,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<WattsonRoute.DocumentDetail> { backStackEntry ->
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val documentDetail: WattsonRoute.DocumentDetail = backStackEntry.toRoute()
            DocumentDetailScreen(
                documentId = documentDetail.documentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<WattsonRoute.RepairChat> { backStackEntry ->
            if (!isLoggedIn) {
                LaunchedEffect(Unit) {
                    navController.navigate(WattsonRoute.Auth) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@composable
            }
            val repairChat: WattsonRoute.RepairChat = backStackEntry.toRoute()
            RepairChatScreen(
                conversationId = repairChat.conversationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Get current route from NavController
 */
fun NavHostController.currentRoute(): WattsonRoute? {
    return currentBackStackEntry?.destination?.route?.let { route ->
        when {
            route.contains("History") -> WattsonRoute.History
            route.contains("Repair") -> WattsonRoute.Repair
            route.contains("Documents") -> WattsonRoute.Documents
            route.contains("Account") -> WattsonRoute.Account
            route.contains("Scan") -> WattsonRoute.Scan
            route.contains("Auth") -> WattsonRoute.Auth
            else -> null
        }
    }
}
