// Supabase Edge Function: Push-Benachrichtigungen via FCM V1 API
// Deploy: supabase functions deploy send-push-notification
// Secret setzen: supabase secrets set FCM_SERVICE_ACCOUNT='{"type":"service_account",...}'

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
const supabaseServiceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;

const supabase = createClient(supabaseUrl, supabaseServiceKey);

interface PushPayload {
  event_type: "timer_created" | "timer_deleted" | "timer_expired";
  timer_name: string;
  timer_data: Record<string, unknown>;
  source_device_id?: string; // Gerät das die Aktion ausgelöst hat (wird gefiltert)
}

// --- Google OAuth2 Token holen (für FCM V1 API) ---

interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

function base64url(data: Uint8Array): string {
  return btoa(String.fromCharCode(...data))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function getAccessToken(sa: ServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);

  // JWT Header + Claims
  const header = base64url(
    new TextEncoder().encode(JSON.stringify({ alg: "RS256", typ: "JWT" }))
  );
  const claims = base64url(
    new TextEncoder().encode(
      JSON.stringify({
        iss: sa.client_email,
        scope: "https://www.googleapis.com/auth/firebase.messaging",
        aud: "https://oauth2.googleapis.com/token",
        iat: now,
        exp: now + 3600,
      })
    )
  );

  // Signieren mit Private Key
  const signingInput = `${header}.${claims}`;

  const pemContent = sa.private_key
    .replace(/-----BEGIN PRIVATE KEY-----/, "")
    .replace(/-----END PRIVATE KEY-----/, "")
    .replace(/\n/g, "");

  const keyData = Uint8Array.from(atob(pemContent), (c) => c.charCodeAt(0));

  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    keyData,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"]
  );

  const signature = new Uint8Array(
    await crypto.subtle.sign(
      "RSASSA-PKCS1-v1_5",
      cryptoKey,
      new TextEncoder().encode(signingInput)
    )
  );

  const jwt = `${signingInput}.${base64url(signature)}`;

  // JWT gegen Access Token tauschen
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: `grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${jwt}`,
  });

  const tokenData = await tokenResponse.json();
  return tokenData.access_token;
}

// --- Nachrichten-Texte ---

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

// --- Main Handler ---

Deno.serve(async (req) => {
  try {
    const payload: PushPayload = await req.json();
    console.log("Event empfangen:", payload.event_type, payload.timer_name);

    // Service Account aus Secret laden
    const saJson = Deno.env.get("FCM_SERVICE_ACCOUNT");
    if (!saJson) {
      console.error("FCM_SERVICE_ACCOUNT Secret nicht gesetzt!");
      return new Response(
        JSON.stringify({ error: "FCM_SERVICE_ACCOUNT nicht konfiguriert" }),
        { status: 500 }
      );
    }
    const serviceAccount: ServiceAccount = JSON.parse(saJson);

    // OAuth2 Access Token holen
    const accessToken = await getAccessToken(serviceAccount);

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

    // Eigenes Gerät rausfiltern — kein Push an das Gerät das die Aktion ausgelöst hat
    const sourceDeviceId = payload.source_device_id || payload.timer_data?.source_device_id;
    const filteredTokens = sourceDeviceId
      ? tokens.filter((t) => t.device_id !== sourceDeviceId)
      : tokens;

    if (filteredTokens.length === 0) {
      console.log("Alle Geräte gefiltert (nur eigenes Gerät registriert)");
      return new Response(JSON.stringify({ message: "Keine anderen Geräte", filtered: tokens.length }), {
        status: 200,
      });
    }

    const message = buildMessage(payload);
    const projectId = serviceAccount.project_id;
    console.log(`Sende Push an ${filteredTokens.length}/${tokens.length} Geräte (${sourceDeviceId ? 'eigenes Gerät gefiltert' : 'kein Filter'}):`, message.title);

    // Push an jedes Gerät senden (FCM V1 API) — eigenes Gerät bereits gefiltert
    const results = await Promise.allSettled(
      filteredTokens.map(async (token) => {
        const response = await fetch(
          `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${accessToken}`,
            },
            body: JSON.stringify({
              message: {
                token: token.fcm_token,
                data: {
                  type: payload.event_type,
                  title: message.title,
                  body: message.body,
                  timer_name: payload.timer_name,
                },
                android: {
                  priority: "high",
                },
              },
            }),
          }
        );

        const result = await response.json();
        return {
          device_id: token.device_id,
          status: response.status,
          result,
        };
      })
    );

    // Ungültige Tokens aufräumen (404 = Token nicht mehr gültig)
    for (const result of results) {
      if (result.status === "fulfilled") {
        const { device_id, status } = result.value;
        if (status === 404 || status === 400) {
          console.log(`Token ungültig, entferne Gerät: ${device_id}`);
          await supabase
            .from("fcm_tokens")
            .delete()
            .eq("device_id", device_id);
        }
      }
    }

    const successful = results.filter(
      (r) => r.status === "fulfilled" && r.value.status === 200
    ).length;
    console.log(`Push gesendet: ${successful}/${filteredTokens.length} erfolgreich`);

    return new Response(
      JSON.stringify({
        sent: successful,
        total: filteredTokens.length,
        filtered: sourceDeviceId ? 1 : 0,
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
