package com.example.timerapp.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.GlanceStateDefinition
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import com.example.timerapp.models.Timer
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * DataStore für Widget-State.
 * Wird automatisch von Glance beobachtet - Änderungen triggern Widget-Updates!
 */
private val Context.timerWidgetDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "timer_widget_state"
)

/**
 * Keys für den Widget-State
 */
object TimerWidgetStateKeys {
    val TIMERS_JSON = stringPreferencesKey("timers_json")
    val LAST_UPDATE = stringPreferencesKey("last_update")
}

/**
 * GlanceStateDefinition für das Timer Widget.
 * Ermöglicht Live-Updates wenn sich die Daten ändern.
 */
object TimerWidgetStateDefinition : GlanceStateDefinition<Preferences> {

    override suspend fun getDataStore(context: Context, fileKey: String): DataStore<Preferences> {
        return context.timerWidgetDataStore
    }

    override fun getLocation(context: Context, fileKey: String): File {
        return File(context.filesDir, "datastore/timer_widget_state.preferences_pb")
    }
}

/**
 * Hilfsobjekt zum Aktualisieren des Widget-States.
 * Wenn diese Funktionen aufgerufen werden, aktualisiert sich das Widget SOFORT!
 */
object TimerWidgetStateHelper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Aktualisiert die Timer im Widget-State.
     * Das Widget wird AUTOMATISCH aktualisiert!
     */
    suspend fun updateTimers(context: Context, timers: List<Timer>) {
        // Konvertiere zu WidgetTimer
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

        // In DataStore schreiben
        context.timerWidgetDataStore.edit { prefs ->
            prefs[TimerWidgetStateKeys.TIMERS_JSON] = jsonString
            prefs[TimerWidgetStateKeys.LAST_UPDATE] = System.currentTimeMillis().toString()
        }

        // Widget explizit aktualisieren
        TimerWidget().updateAll(context)
    }

    /**
     * Lädt die Timer aus dem Widget-State.
     */
    suspend fun loadTimers(context: Context): List<WidgetTimer> {
        return try {
            val prefs = context.timerWidgetDataStore.data.first()
            val jsonString = prefs[TimerWidgetStateKeys.TIMERS_JSON]

            if (jsonString.isNullOrEmpty()) {
                emptyList()
            } else {
                json.decodeFromString<List<WidgetTimer>>(jsonString)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
