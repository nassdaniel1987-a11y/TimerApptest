package com.example.timerapp.shortcuts

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.timerapp.MainActivity
import com.example.timerapp.R
import com.example.timerapp.models.Timer
import com.example.timerapp.utils.DateTimeUtils

/**
 * Verwaltet Dynamic Shortcuts für die nächsten anstehenden Timer.
 *
 * Dynamic Shortcuts erscheinen beim Long-Press auf das App-Icon
 * unterhalb der statischen Shortcuts und zeigen die nächsten Timer.
 */
object ShortcutManagerHelper {

    private const val TAG = "ShortcutManagerHelper"
    private const val MAX_DYNAMIC_SHORTCUTS = 2

    /**
     * Aktualisiert die Dynamic Shortcuts mit den nächsten anstehenden Timern.
     */
    fun updateDynamicShortcuts(context: Context, timers: List<Timer>) {
        try {
            val now = java.time.ZonedDateTime.now()

            // Nur zukünftige, nicht abgeschlossene Timer
            val upcomingTimers = timers
                .filter { !it.is_completed }
                .mapNotNull { timer ->
                    val targetTime = DateTimeUtils.parseIsoDateTime(timer.target_time)
                    if (targetTime != null && targetTime.isAfter(now)) {
                        timer to targetTime
                    } else null
                }
                .sortedBy { it.second }
                .take(MAX_DYNAMIC_SHORTCUTS)

            // Alte Dynamic Shortcuts entfernen
            ShortcutManagerCompat.removeAllDynamicShortcuts(context)

            if (upcomingTimers.isEmpty()) {
                Log.d(TAG, "📭 Keine anstehenden Timer für Dynamic Shortcuts")
                return
            }

            val shortcuts = upcomingTimers.map { (timer, targetTime) ->
                val timeText = DateTimeUtils.getTimeUntilText(targetTime)

                ShortcutInfoCompat.Builder(context, "dynamic_timer_${timer.id}")
                    .setShortLabel(timer.name)
                    .setLongLabel("${timer.name} - $timeText")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_shortcut_timer))
                    .setIntent(
                        Intent(context, MainActivity::class.java).apply {
                            action = Intent.ACTION_VIEW
                            // Öffnet die App (Timer ist sichtbar in der Liste)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                    )
                    .setRank(upcomingTimers.indexOf(timer to targetTime))
                    .build()
            }

            ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
            Log.d(TAG, "✅ ${shortcuts.size} Dynamic Shortcuts aktualisiert")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Fehler beim Aktualisieren der Dynamic Shortcuts: ${e.message}", e)
        }
    }
}
