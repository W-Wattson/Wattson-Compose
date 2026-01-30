package com.wattson.domain.model

/**
 * Generic UI state wrapper following UDF principles
 * Represents the three main states of async data loading
 */
sealed interface UiState<out T> {
    /**
     * Initial loading state
     */
    data object Loading : UiState<Nothing>

    /**
     * Success state with data
     */
    data class Success<T>(val data: T) : UiState<T>

    /**
     * Error state with message and optional retry action
     */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : UiState<Nothing>

    /**
     * Empty state (success but no data)
     */
    data object Empty : UiState<Nothing>
}

/**
 * Extension to check if state is loading
 */
val UiState<*>.isLoading: Boolean
    get() = this is UiState.Loading

/**
 * Extension to check if state is successful
 */
val UiState<*>.isSuccess: Boolean
    get() = this is UiState.Success

/**
 * Extension to check if state is error
 */
val UiState<*>.isError: Boolean
    get() = this is UiState.Error

/**
 * Extension to get data or null
 */
fun <T> UiState<T>.getOrNull(): T? {
    return (this as? UiState.Success)?.data
}

/**
 * Result wrapper for domain operations
 * Alternative to Kotlin's Result for better sealed class support
 */
sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

/**
 * Application error types used for consistent error handling.
 */
sealed interface AppError {
    val message: String
    val code: String?

    data class Network(
        override val message: String = "Erreur de connexion",
        override val code: String? = null,
        val isTimeout: Boolean = false
    ) : AppError

    data class Authentication(
        override val message: String = "Session expirée",
        override val code: String? = null
    ) : AppError

    data class Validation(
        override val message: String,
        override val code: String? = null,
        val field: String? = null
    ) : AppError

    data class NotFound(
        override val message: String = "Ressource introuvable",
        override val code: String? = null
    ) : AppError

    data class Server(
        override val message: String = "Erreur serveur",
        override val code: String? = null,
        val statusCode: Int? = null
    ) : AppError

    data class Storage(
        override val message: String = "Erreur de stockage",
        override val code: String? = null
    ) : AppError

    data class Permission(
        override val message: String = "Permission refusée",
        override val code: String? = null,
        val permission: String? = null
    ) : AppError

    data class QuotaExceeded(
        override val message: String = "Limite atteinte",
        override val code: String? = null,
        val currentCount: Int? = null,
        val maxAllowed: Int? = null
    ) : AppError

    data class Unknown(
        override val message: String = "Erreur inattendue",
        override val code: String? = null,
        val throwable: Throwable? = null
    ) : AppError
}

/**
 * Extension to map Result to UiState
 */
fun <T> AppResult<T>.toUiState(): UiState<T> {
    return when (this) {
        is AppResult.Success -> UiState.Success(data)
        is AppResult.Failure -> UiState.Error(error.message)
    }
}

/**
 * Extension to map AppResult
 */
inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> {
    return when (this) {
        is AppResult.Success -> AppResult.Success(transform(data))
        is AppResult.Failure -> this
    }
}

/**
 * Extension to flatMap AppResult
 */
inline fun <T, R> AppResult<T>.flatMap(transform: (T) -> AppResult<R>): AppResult<R> {
    return when (this) {
        is AppResult.Success -> transform(data)
        is AppResult.Failure -> this
    }
}
