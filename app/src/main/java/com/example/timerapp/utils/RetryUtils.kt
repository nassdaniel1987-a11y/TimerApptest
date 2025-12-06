package com.example.timerapp.utils

import android.util.Log
import com.example.timerapp.models.Result
import kotlinx.coroutines.delay
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.math.pow

/**
 * Utility für Retry-Logik mit Exponential Backoff
 */
object RetryUtils {

    /**
     * Führt eine Operation mit Retry-Logik aus
     *
     * @param times Maximale Anzahl an Versuchen
     * @param initialDelayMillis Initiale Verzögerung in Millisekunden
     * @param maxDelayMillis Maximale Verzögerung in Millisekunden
     * @param factor Multiplikator für exponential backoff
     * @param block Die auszuführende Operation
     * @return Result mit Erfolg oder Fehler
     */
    suspend fun <T> retryWithExponentialBackoff(
        times: Int = 3,
        initialDelayMillis: Long = 1000,
        maxDelayMillis: Long = 10000,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMillis
        var lastException: Exception? = null

        repeat(times) { attempt ->
            try {
                val result = block()
                if (attempt > 0) {
                    Log.d("RetryUtils", "✅ Erfolg nach ${attempt + 1} Versuchen")
                }
                return Result.Success(result)
            } catch (e: Exception) {
                lastException = e

                // Prüfe ob Retry sinnvoll ist
                if (!isRetryable(e)) {
                    Log.e("RetryUtils", "❌ Nicht-retry-barer Fehler: ${e.message}")
                    return Result.Error(
                        exception = e,
                        retryable = false,
                        userMessage = getUserMessage(e)
                    )
                }

                // Letzter Versuch?
                if (attempt == times - 1) {
                    Log.e("RetryUtils", "❌ Alle $times Versuche fehlgeschlagen")
                    return Result.Error(
                        exception = e,
                        retryable = true,
                        userMessage = getUserMessage(e)
                    )
                }

                // Warte vor nächstem Versuch
                Log.w("RetryUtils", "⚠️ Versuch ${attempt + 1}/$times fehlgeschlagen, warte ${currentDelay}ms...")
                delay(currentDelay)

                // Berechne nächste Verzögerung (Exponential Backoff)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelayMillis)
            }
        }

        // Sollte nie erreicht werden, aber zur Sicherheit
        return Result.Error(
            exception = lastException ?: Exception("Unknown error"),
            retryable = true,
            userMessage = "Vorgang fehlgeschlagen nach $times Versuchen"
        )
    }

    /**
     * Prüft ob eine Exception retry-bar ist
     */
    private fun isRetryable(exception: Exception): Boolean = when (exception) {
        // Netzwerk-Fehler → Retry
        is IOException,
        is SocketTimeoutException,
        is UnknownHostException -> true

        // Validierungs-Fehler → Kein Retry
        is IllegalArgumentException,
        is IllegalStateException -> false

        // Andere Fehler → Kein Retry (zur Sicherheit)
        else -> false
    }

    /**
     * Generiert benutzerfreundliche Fehlermeldung
     */
    private fun getUserMessage(exception: Exception): String = when (exception) {
        is IOException -> "Netzwerkfehler. Bitte prüfe deine Internetverbindung."
        is SocketTimeoutException -> "Zeitüberschreitung. Der Server antwortet nicht."
        is UnknownHostException -> "Keine Verbindung zum Server möglich."
        is IllegalArgumentException -> "Ungültige Eingabe: ${exception.message}"
        else -> "Ein Fehler ist aufgetreten: ${exception.message ?: "Unbekannter Fehler"}"
    }
}

/**
 * Extension Function für einfachere Verwendung
 */
suspend fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): Result<T> = RetryUtils.retryWithExponentialBackoff(
    times = times,
    initialDelayMillis = initialDelay,
    maxDelayMillis = maxDelay,
    factor = factor,
    block = block
)
