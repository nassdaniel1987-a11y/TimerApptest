package com.example.timerapp

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout

object SupabaseClient {

    // ⚠️ WICHTIG: Hier die echten Supabase-Credentials eintragen!
    private const val SUPABASE_URL = "https://llqvowmainjrbfsyxtnb.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxscXZvd21haW5qcmJmc3l4dG5iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAxMjIwNDAsImV4cCI6MjA3NTY5ODA0MH0.TDNMMXo1ZAstWb6tXLMJfdvVxXVauNrjmDhCM7UyvY0"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_ANON_KEY
    ) {
        install(Postgrest)

        // ✅ HTTP Timeouts: Verhindert App-Hangs bei langsamer Verbindung
        httpEngine {
            engine = Android.create {
                connectTimeout = 5_000  // 5 Sekunden für Connection-Aufbau
                socketTimeout = 15_000  // 15 Sekunden für Socket-Read
            }
        }
    }
}
