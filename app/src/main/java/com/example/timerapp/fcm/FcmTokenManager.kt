package com.example.timerapp.fcm

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FcmTokenEntry(
    @SerialName("device_id") val deviceId: String,
    @SerialName("fcm_token") val fcmToken: String,
    @SerialName("device_name") val deviceName: String,
    @SerialName("app_version") val appVersion: String = "1.0"
)

class FcmTokenManager(
    private val context: Context,
    private val supabaseClient: SupabaseClient
) {
    private val TAG = "FcmTokenManager"

    /**
     * Generiert eine stabile Device-ID basierend auf ANDROID_ID.
     * Bleibt gleich solange die App installiert ist.
     */
    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    /**
     * Holt den aktuellen FCM Token und speichert ihn in Supabase.
     * Nutzt UPSERT - existierender Eintrag wird aktualisiert, neuer wird erstellt.
     */
    suspend fun registerToken() {
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            val deviceId = getDeviceId()
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

            val entry = FcmTokenEntry(
                deviceId = deviceId,
                fcmToken = token,
                deviceName = deviceName
            )

            supabaseClient.from("fcm_tokens").upsert(entry) {
                onConflict = "device_id"
            }

            Log.d(TAG, "FCM Token registriert für Gerät: $deviceName ($deviceId)")
        } catch (e: Exception) {
            Log.w(TAG, "FCM Token Registration fehlgeschlagen: ${e.message}")
        }
    }

    /**
     * Entfernt den Token (z.B. wenn User sich abmeldet oder App deinstalliert).
     */
    suspend fun unregisterToken() {
        try {
            val deviceId = getDeviceId()
            supabaseClient.from("fcm_tokens").delete {
                filter { eq("device_id", deviceId) }
            }
            Log.d(TAG, "FCM Token entfernt für Gerät: $deviceId")
        } catch (e: Exception) {
            Log.w(TAG, "FCM Token Entfernung fehlgeschlagen: ${e.message}")
        }
    }
}
