package com.example.timerapp.fcm

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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

    fun getDeviceId(): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private suspend fun getFcmToken(): String = suspendCoroutine { cont ->
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token -> cont.resume(token) }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

    suspend fun registerToken() {
        try {
            val token = getFcmToken()
            val deviceId = getDeviceId()
            val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

            val entry = FcmTokenEntry(
                deviceId = deviceId,
                fcmToken = token,
                deviceName = deviceName
            )

            supabaseClient.from("fcm_tokens").upsert(entry)

            Log.d(TAG, "FCM Token registriert für Gerät: $deviceName ($deviceId)")
        } catch (e: Exception) {
            Log.w(TAG, "FCM Token Registration fehlgeschlagen: ${e.message}")
        }
    }

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
