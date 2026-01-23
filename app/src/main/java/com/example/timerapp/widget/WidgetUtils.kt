package com.example.timerapp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility-Funktionen für das Timer Widget.
 */
object WidgetUtils {

    /**
     * Aktualisiert alle Timer-Widgets.
     * Sollte aufgerufen werden, wenn Timer erstellt, geändert oder gelöscht werden.
     */
    fun updateWidgets(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                TimerWidget().updateAll(context)
            } catch (e: Exception) {
                // Widget-Update fehlgeschlagen, ignorieren
            }
        }
    }

    /**
     * Prüft, ob Widgets aktiv sind.
     */
    suspend fun hasActiveWidgets(context: Context): Boolean {
        return try {
            val manager = GlanceAppWidgetManager(context)
            manager.getGlanceIds(TimerWidget::class.java).isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }
}
