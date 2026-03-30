package com.example.timerapp.fcm

import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Ruft die Supabase Edge Function "send-push-notification" DIREKT auf,
 * damit FCM Push-Benachrichtigungen sofort gesendet werden — unabhängig
 * vom normalen Sync-Prozess (der bis zu 15+ Minuten dauern kann).
 */
class PushNotificationManager(
    private val supabaseClient: SupabaseClient
) {
    private val TAG = "PushNotificationManager"
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Serializable
    data class PushPayload(
        val event_type: String,
        val timer_name: String,
        val timer_data: Map<String, String> = emptyMap(),
        val source_device_id: String? = null
    )

    /**
     * Sendet eine Push-Benachrichtigung über die Edge Function.
     * Diese Methode ist non-blocking und fängt alle Fehler ab.
     * Falls sie fehlschlägt (z.B. offline), wird der Push über den
     * normalen Sync-Weg nachgeholt (DB-Trigger/Webhook).
     */
    suspend fun sendPushNotification(
        eventType: String,
        timerName: String,
        sourceDeviceId: String?,
        timerData: Map<String, String> = emptyMap()
    ) {
        try {
            val payload = PushPayload(
                event_type = eventType,
                timer_name = timerName,
                timer_data = timerData,
                source_device_id = sourceDeviceId
            )

            val bodyJson = json.encodeToString(payload)

            Log.d(TAG, "📤 Push direkt senden: $eventType für '$timerName'")

            supabaseClient.functions.invoke("send-push-notification") {
                contentType(ContentType.Application.Json)
                setBody(bodyJson)
            }

            Log.d(TAG, "✅ Push direkt gesendet: $eventType für '$timerName'")
        } catch (e: Exception) {
            // Fehler sind OK — Push wird über den normalen DB-Trigger/Webhook nachgeholt
            Log.w(TAG, "⚠️ Push direkt senden fehlgeschlagen (wird über Sync nachgeholt): ${e.message}")
        }
    }
}
