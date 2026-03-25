package com.wattson.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.wattson.data.auth.GoogleAuthException
import com.wattson.data.auth.GoogleAuthManager
import com.wattson.domain.model.AuthProvider
import com.wattson.ui.i18n.asString
import com.wattson.ui.screens.account.AccountScreen
import com.wattson.ui.screens.account.AccountViewModel
import com.wattson.ui.screens.auth.AuthEvent
import com.wattson.ui.screens.auth.AuthIntent
import com.wattson.ui.screens.auth.AuthScreen
import com.wattson.ui.screens.auth.AuthViewModel
import com.wattson.ui.screens.auth.LoginScreen
import com.wattson.ui.screens.auth.RegisterScreen
import com.wattson.ui.screens.documents.DocumentsScreen
import com.wattson.ui.screens.documents.DocumentsViewModel
import com.wattson.ui.screens.documents.detail.DocumentDetailScreen
import com.wattson.ui.screens.history.HistoryIntent
import com.wattson.ui.screens.history.HistoryScreen
import com.wattson.ui.screens.history.HistoryViewModel
import com.wattson.ui.screens.premium.PremiumScreen
import com.wattson.ui.screens.premium.PremiumViewModel
import com.wattson.ui.screens.product.ProductDetailScreen
import com.wattson.ui.screens.repair.RepairScreen
import com.wattson.ui.screens.repair.chat.RepairChatScreen
import com.wattson.ui.screens.scan.ScanScreen
import com.wattson.ui.screens.scan.ScanViewModel

private const val TRANSITION_DURATION = 300

/**
 * Main navigation host for the Wattson application.
 *
 * @param navController The navigation controller.
 * @param startDestination The starting route chosen from the current authentication state.
 * @param innerPadding Padding propagated from the parent scaffold.
 * @param isLoggedIn Whether the user is currently authenticated.
 */
@Composable
fun WattsonNavHost(
    navController: NavHostController,
    startDestination: WattsonRoute,
    innerPadding: PaddingValues,
    isLoggedIn: Boolean
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
        composable<WattsonRoute.Auth> {
            val viewModel: AuthViewModel = hiltViewModel()
            val snackbarHostState = remember { SnackbarHostState() }

            HandleAuthEvents(
                viewModel = viewModel,
                navController = navController,
                snackbarHostState = snackbarHostState,
                onGoogleSignInFailure = { exception ->
                    viewModel.handleGoogleSignInError(exception.uiText)
                }
            )

            AuthScreen(
                onNavigateToLogin = {
                    navController.navigate(WattsonRoute.Login)
                },
                onNavigateToRegister = {
                    navController.navigate(WattsonRoute.Register)
                },
                onLoginWithGoogle = {
                    viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.GOOGLE))
                },
                snackbarHostState = snackbarHostState
            )
        }

        composable<WattsonRoute.Login> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            HandleAuthEvents(
                viewModel = viewModel,
                navController = navController,
                snackbarHostState = snackbarHostState,
                onGoogleSignInFailure = { exception ->
                    viewModel.handleGoogleSignInError(exception.uiText)
                },
                onUnhandledEvent = { event ->
                    if (event is AuthEvent.NavigateToRegister) {
                        navController.navigate(WattsonRoute.Register)
                    }
                }
            )

            LoginScreen(
                uiState = uiState,
                onEmailChange = { viewModel.onIntent(AuthIntent.UpdateEmail(it)) },
                onPasswordChange = { viewModel.onIntent(AuthIntent.UpdatePassword(it)) },
                onToggleRememberMe = {
                    viewModel.onIntent(AuthIntent.ToggleRememberMe(it))
                },
                onLogin = { viewModel.performLogin() },
                onForgotPassword = { viewModel.handleForgotPassword() },
                onLoginWithGoogle = {
                    viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.GOOGLE))
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRegister = {
                    navController.navigate(WattsonRoute.Register)
                },
                snackbarHostState = snackbarHostState
            )
        }

        composable<WattsonRoute.Register> {
            val viewModel: AuthViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
            val snackbarHostState = remember { SnackbarHostState() }

            HandleAuthEvents(
                viewModel = viewModel,
                navController = navController,
                snackbarHostState = snackbarHostState,
                onGoogleSignInFailure = { exception ->
                    viewModel.handleGoogleSignInError(exception.uiText)
                },
                onUnhandledEvent = { event ->
                    if (event is AuthEvent.NavigateToLogin) {
                        navController.navigate(WattsonRoute.Login)
                    }
                }
            )

            RegisterScreen(
                uiState = uiState,
                onEmailChange = { viewModel.onIntent(AuthIntent.UpdateEmail(it)) },
                onPasswordChange = { viewModel.onIntent(AuthIntent.UpdatePassword(it)) },
                onConfirmPasswordChange = {
                    viewModel.onIntent(AuthIntent.UpdateConfirmPassword(it))
                },
                onRegister = { viewModel.performRegister() },
                onLoginWithGoogle = {
                    viewModel.onIntent(AuthIntent.LoginWithProvider(AuthProvider.GOOGLE))
                },
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(WattsonRoute.Login)
                },
                snackbarHostState = snackbarHostState
            )
        }

        authenticatedComposable<WattsonRoute.History>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
            val viewModel: HistoryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()
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

        authenticatedComposable<WattsonRoute.Repair>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
            RepairScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(WattsonRoute.RepairChat(conversationId))
                }
            )
        }

        authenticatedComposable<WattsonRoute.Scan>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
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

        authenticatedComposable<WattsonRoute.Documents>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
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

        authenticatedComposable<WattsonRoute.Account>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
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
                    navController.navigateToAuthRoot()
                }
            )
        }

        authenticatedComposable<WattsonRoute.ProductDetail>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) { backStackEntry ->
            val productDetail: WattsonRoute.ProductDetail = backStackEntry.toRoute()

            ProductDetailScreen(
                productId = productDetail.productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        authenticatedComposable<WattsonRoute.Premium>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) {
            val viewModel: PremiumViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            PremiumScreen(
                uiState = uiState,
                events = viewModel.events,
                onIntent = viewModel::onIntent,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        authenticatedComposable<WattsonRoute.DocumentDetail>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) { backStackEntry ->
            val documentDetail: WattsonRoute.DocumentDetail = backStackEntry.toRoute()

            DocumentDetailScreen(
                documentId = documentDetail.documentId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        authenticatedComposable<WattsonRoute.RepairChat>(
            navController = navController,
            isLoggedIn = isLoggedIn
        ) { backStackEntry ->
            val repairChat: WattsonRoute.RepairChat = backStackEntry.toRoute()

            RepairChatScreen(
                conversationId = repairChat.conversationId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

/**
 * Handles shared authentication navigation events and delegates screen-specific events.
 */
@Composable
private fun HandleAuthEvents(
    viewModel: AuthViewModel,
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    onGoogleSignInFailure: suspend (GoogleAuthException) -> Unit,
    onUnhandledEvent: suspend (AuthEvent) -> Unit = {}
) {
    val context = LocalContext.current
    val googleAuthManager = remember { GoogleAuthManager() }

    LaunchedEffect(viewModel, navController, context) {
        viewModel.events.collect { event ->
            when (event) {
                AuthEvent.NavigateToMain -> navController.navigateToMainRoot()
                AuthEvent.RequestGoogleSignIn -> {
                    try {
                        val idToken = googleAuthManager.getGoogleIdToken(context)
                        viewModel.onIntent(AuthIntent.GoogleIdTokenReceived(idToken))
                    } catch (exception: GoogleAuthException) {
                        onGoogleSignInFailure(exception)
                    }
                }

                is AuthEvent.ShowError -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }

                is AuthEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message.asString(context))
                }

                else -> onUnhandledEvent(event)
            }
        }
    }
}

/**
 * Wraps a destination that must only be reachable when the user is authenticated.
 */
private inline fun <reified T : Any> NavGraphBuilder.authenticatedComposable(
    navController: NavHostController,
    isLoggedIn: Boolean,
    noinline content: @Composable (NavBackStackEntry) -> Unit
) {
    composable<T> { backStackEntry ->
        if (!isLoggedIn) {
            RedirectUnauthenticatedUser(navController)
            return@composable
        }

        content(backStackEntry)
    }
}

/**
 * Redirects the user to the authentication root when a protected destination is reached
 * without an active session.
 */
@Composable
private fun RedirectUnauthenticatedUser(navController: NavHostController) {
    LaunchedEffect(navController) {
        navController.navigateToAuthRoot()
    }
}

private fun NavHostController.navigateToAuthRoot() {
    navigate(WattsonRoute.Auth) {
        popUpTo(0) { inclusive = true }
    }
}

private fun NavHostController.navigateToMainRoot() {
    navigate(WattsonRoute.History) {
        popUpTo(0) { inclusive = true }
    }
}
