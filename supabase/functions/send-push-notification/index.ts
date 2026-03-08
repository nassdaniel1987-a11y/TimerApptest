// Supabase Edge Function: Push-Benachrichtigungen an alle Geräte senden
// Deploy: supabase functions deploy send-push-notification
// Secrets setzen: supabase secrets set FCM_SERVER_KEY=dein_server_key

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const fcmServerKey = Deno.env.get("FCM_SERVER_KEY")!;

const supabase = createClient(supabaseUrl, supabaseServiceKey);

interface PushPayload {
  event_type: "timer_created" | "timer_deleted" | "timer_expired";
  timer_name: string;
  timer_data: Record<string, unknown>;
}

// Nachrichten-Texte je nach Event
function buildMessage(payload: PushPayload): { title: string; body: string } {
  const name = payload.timer_name || "Unbekannter Timer";

  switch (payload.event_type) {
    case "timer_created":
      return {
        title: "Neuer Timer erstellt",
        body: `Timer "${name}" wurde erstellt`,
      };
    case "timer_deleted":
      return {
        title: "Timer gelöscht",
        body: `Timer "${name}" wurde gelöscht`,
      };
    case "timer_expired":
      return {
        title: "Timer abgelaufen!",
        body: `Timer "${name}" ist abgelaufen`,
      };
    default:
      return { title: "TimerApp", body: "Es gibt Neuigkeiten" };
  }
}

Deno.serve(async (req) => {
  try {
    const payload: PushPayload = await req.json();
    console.log("Event empfangen:", payload.event_type, payload.timer_name);

    // Alle registrierten FCM Tokens holen
    const { data: tokens, error } = await supabase
      .from("fcm_tokens")
      .select("fcm_token, device_id");

    if (error) {
      console.error("Fehler beim Token-Abruf:", error);
      return new Response(JSON.stringify({ error: error.message }), {
        status: 500,
      });
    }

    if (!tokens || tokens.length === 0) {
      console.log("Keine registrierten Geräte gefunden");
      return new Response(JSON.stringify({ message: "Keine Geräte" }), {
        status: 200,
      });
    }

    const message = buildMessage(payload);
    console.log(`Sende Push an ${tokens.length} Geräte:`, message.title);

    // Push an jedes Gerät senden (via FCM HTTP v1 Legacy API)
    const results = await Promise.allSettled(
      tokens.map(async (token) => {
        const response = await fetch(
          "https://fcm.googleapis.com/fcm/send",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `key=${fcmServerKey}`,
            },
            body: JSON.stringify({
              to: token.fcm_token,
              data: {
                type: payload.event_type,
                title: message.title,
                body: message.body,
                timer_name: payload.timer_name,
              },
              // Data-only message → wird IMMER an onMessageReceived geliefert
              // (auch im Hintergrund)
            }),
          }
        );
        const result = await response.json();
        return { device_id: token.device_id, result };
      })
    );

    // Ungültige Tokens aufräumen
    for (const result of results) {
      if (result.status === "fulfilled") {
        const { device_id, result: fcmResult } = result.value;
        if (fcmResult.failure === 1) {
          console.log(`Token ungültig, entferne Gerät: ${device_id}`);
          await supabase
            .from("fcm_tokens")
            .delete()
            .eq("device_id", device_id);
        }
      }
    }

    const successful = results.filter((r) => r.status === "fulfilled").length;
    console.log(`Push gesendet: ${successful}/${tokens.length} erfolgreich`);

    return new Response(
      JSON.stringify({
        sent: successful,
        total: tokens.length,
        event: payload.event_type,
      }),
      { status: 200, headers: { "Content-Type": "application/json" } }
    );
  } catch (err) {
    console.error("Edge Function Fehler:", err);
    return new Response(JSON.stringify({ error: String(err) }), {
      status: 500,
    });
  }
});
