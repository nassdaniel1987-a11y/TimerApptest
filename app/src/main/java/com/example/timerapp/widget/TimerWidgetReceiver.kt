package com.example.timerapp.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * Receiver für das Timer Widget.
 * Wird vom System aufgerufen, wenn das Widget aktualisiert werden soll.
 */
class TimerWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = TimerWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget wurde zum ersten Mal hinzugefügt
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Letztes Widget wurde entfernt
    }
}
