package com.example.timerapp.widget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.timerapp.SettingsManager
import com.example.timerapp.models.Timer
import androidx.glance.appwidget.updateAll
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

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
     * HINWEIS: Ruft NICHT mehr automatisch WidgetUtils.updateWidgets() auf.
     * Der Aufrufer (z.B. TimerViewModel.updateWidgetCache()) steuert das Widget-Update selbst.
     */
    fun cacheTimers(context: Context, timers: List<Timer>) {
        try {
            val klasseFilter = SettingsManager.getInstance(context).klasseFilter

            // Konvertiere Timer zu WidgetTimer - nur nicht abgeschlossene + Klassen-Filter
            val widgetTimers = timers
                .filter { !it.is_completed && (klasseFilter.isEmpty() || it.klasse in klasseFilter) }
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
                .apply() // apply() — non-blocking, schreibt in-memory sofort

            Log.d(TAG, "⚡ ${widgetTimers.size} Timer im Cache gespeichert")
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
     * Lädt Timer aus der lokalen Room-Datenbank und aktualisiert den Cache.
     * Funktioniert komplett offline.
     */
    suspend fun refreshFromDatabase(context: Context): Boolean {
        return try {
            Log.d(TAG, "📦 Lade Timer aus Room-Datenbank...")

            val db = androidx.room.Room.databaseBuilder(
                context,
                com.example.timerapp.data.AppDatabase::class.java,
                "timer_database"
            ).addMigrations(com.example.timerapp.data.AppDatabase.MIGRATION_1_2).build()

            val timers = db.timerDao().getActiveTimersForWidget()
            Log.d(TAG, "✅ ${timers.size} Timer aus Room geladen")

            // In Cache speichern (cacheTimers filtert auch nach Klasse)
            cacheTimers(context, timers)

            // Widget aktualisieren
            TimerWidget().updateAll(context)

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Laden aus Room: ${e.message}", e)
            false
        }
    }
}
