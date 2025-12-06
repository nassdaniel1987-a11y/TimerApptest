package com.example.timerapp.models

/**
 * Sealed class für typsicheres Error Handling
 *
 * Vorteile:
 * - Typsicher: Compiler erzwingt Handling aller Cases
 * - Explizit: Klar ob Operation erfolgreich oder fehlgeschlagen
 * - Retry-fähig: Unterscheidet zwischen retry-baren und permanenten Errors
 */
sealed class Result<out T> {
    /**
     * Erfolgreiche Operation
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Fehler während der Operation
     *
     * @param exception Die ursprüngliche Exception
     * @param retryable True wenn Retry sinnvoll ist (z.B. Netzwerk-Fehler)
     * @param userMessage Benutzerfreundliche Fehlermeldung
     */
    data class Error<T>(
        val exception: Exception,
        val retryable: Boolean = false,
        val userMessage: String? = null
    ) : Result<T>()

    /**
     * Operation läuft noch
     *
     * @param progress Optional: Fortschritt von 0.0 bis 1.0
     */
    data class Loading<T>(val progress: Float? = null) : Result<T>()

    /**
     * Helper: Prüft ob Result erfolgreich ist
     */
    val isSuccess: Boolean
        get() = this is Success

    /**
     * Helper: Prüft ob Result ein Error ist
     */
    val isError: Boolean
        get() = this is Error

    /**
     * Helper: Prüft ob Result Loading ist
     */
    val isLoading: Boolean
        get() = this is Loading

    /**
     * Helper: Holt Daten falls Success, sonst null
     */
    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    /**
     * Helper: Holt Daten falls Success, sonst default
     */
    fun getOrDefault(default: @UnsafeVariance T): T = when (this) {
        is Success -> data
        else -> default
    }

    /**
     * Helper: Map Transformation
     */
    fun <R> map(transform: (T) -> R): Result<R> = when (this) {
        is Success -> Success(transform(data))
        is Error -> Error(exception, retryable, userMessage)
        is Loading -> Loading(progress)
    }
}

/**
 * Extension: Führt Block aus falls Success
 */
inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) {
        block(data)
    }
    return this
}

/**
 * Extension: Führt Block aus falls Error
 */
inline fun <T> Result<T>.onError(block: (Exception, Boolean) -> Unit): Result<T> {
    if (this is Result.Error) {
        block(exception, retryable)
    }
    return this
}

/**
 * Extension: Führt Block aus falls Loading
 */
inline fun <T> Result<T>.onLoading(block: (Float?) -> Unit): Result<T> {
    if (this is Result.Loading) {
        block(progress)
    }
    return this
}
