package com.example.timerapp.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.timerapp.SupabaseClient
import com.example.timerapp.models.Timer
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Cache für Widget-Daten.
 * Speichert die Timer-Daten lokal, damit das Widget sie ohne Netzwerkzugriff lesen kann.
 */
object WidgetDataCache {

    private const val TAG = "WidgetDataCache"
    private const val PREFS_NAME = "widget_data_cache"
    private const val KEY_TIMERS = "cached_timers"
    private const val KEY_LAST_UPDATE = "last_update"

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Speichert die Timer-Liste im Cache.
     * Sollte aufgerufen werden, wenn Timer in der App geladen/geändert werden.
     */
    fun cacheTimers(context: Context, timers: List<Timer>) {
        try {
            Log.d(TAG, "📝 cacheTimers() aufgerufen mit ${timers.size} Timern")

            // Konvertiere Timer zu WidgetTimer - nur nicht abgeschlossene
            val widgetTimers = timers
                .filter { !it.is_completed }
                .sortedBy { it.target_time }
                .take(10)
                .map { timer ->
                    WidgetTimer(
                        id = timer.id,
                        name = timer.name,
                        target_time = timer.target_time,
                        category = timer.category,
                        is_completed = timer.is_completed
                    )
                }

            Log.d(TAG, "📋 ${widgetTimers.size} aktive Timer für Cache")
            widgetTimers.forEach { timer ->
                Log.d(TAG, "   - ${timer.name} (${timer.target_time})")
            }

            val jsonString = json.encodeToString(widgetTimers)

            getPrefs(context).edit()
                .putString(KEY_TIMERS, jsonString)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .commit() // commit() statt apply() für synchrones Schreiben

            Log.d(TAG, "✅ ${widgetTimers.size} Timer im Cache gespeichert")

            // SOFORT Widget aktualisieren nach Cache-Update (neuer klassischer Provider)
            TimerWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Cachen: ${e.message}", e)
        }
    }

    /**
     * Speichert Timer im Cache OHNE Widget-Update.
     * Wird intern verwendet, wenn das Widget sich selbst aktualisiert.
     */
    fun cacheTimersWithoutUpdate(context: Context, timers: List<Timer>) {
        try {
            val widgetTimers = timers
                .filter { !it.is_completed }
                .sortedBy { it.target_time }
                .take(10)
                .map { timer ->
                    WidgetTimer(
                        id = timer.id,
                        name = timer.name,
                        target_time = timer.target_time,
                        category = timer.category,
                        is_completed = timer.is_completed
                    )
                }

            val jsonString = json.encodeToString(widgetTimers)

            getPrefs(context).edit()
                .putString(KEY_TIMERS, jsonString)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .commit()

            Log.d(TAG, "✅ ${widgetTimers.size} Timer im Cache gespeichert (ohne Widget-Update)")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Cachen: ${e.message}", e)
        }
    }

    /**
     * Lädt die Timer aus dem Cache.
     */
    fun loadTimers(context: Context): List<WidgetTimer> {
        return try {
            val jsonString = getPrefs(context).getString(KEY_TIMERS, null)
            if (jsonString.isNullOrEmpty()) {
                Log.d(TAG, "📭 Cache ist leer")
                emptyList()
            } else {
                val timers = json.decodeFromString<List<WidgetTimer>>(jsonString)
                Log.d(TAG, "📥 ${timers.size} Timer aus Cache geladen")
                timers
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden aus Cache: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Gibt den Zeitstempel des letzten Updates zurück.
     */
    fun getLastUpdateTime(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_UPDATE, 0)
    }

    /**
     * Löscht den Cache.
     */
    fun clearCache(context: Context) {
        getPrefs(context).edit().clear().commit()
        Log.d(TAG, "🗑️ Cache gelöscht")
    }

    /**
     * Lädt Timer direkt von Supabase und aktualisiert den Cache.
     * Diese Funktion wird vom Widget-Refresh-Button verwendet.
     */
    suspend fun refreshFromServer(context: Context): Boolean {
        return try {
            Log.d(TAG, "🌐 Lade Timer direkt von Supabase...")

            val client = SupabaseClient.client
            val response = client.from("timers")
                .select()
                .decodeList<Timer>()

            // Sortiere nach Zeit
            val sortedTimers = response.sortedBy {
                try {
                    ZonedDateTime.parse(it.target_time, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        .toInstant()
                        .toEpochMilli()
                } catch (e: Exception) {
                    Long.MAX_VALUE
                }
            }

            Log.d(TAG, "✅ ${sortedTimers.size} Timer von Server geladen")

            // In Cache speichern
            cacheTimers(context, sortedTimers)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden von Supabase: ${e.message}", e)
            false
        }
    }
}
