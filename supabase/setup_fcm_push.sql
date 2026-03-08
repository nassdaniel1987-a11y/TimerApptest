-- =====================================================
-- 1. FCM Tokens Tabelle erstellen
-- =====================================================
-- Führe dies im Supabase SQL Editor aus (Dashboard → SQL Editor)

CREATE TABLE IF NOT EXISTS fcm_tokens (
    device_id TEXT PRIMARY KEY,
    fcm_token TEXT NOT NULL,
    device_name TEXT DEFAULT '',
    app_version TEXT DEFAULT '1.0',
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- RLS aktivieren (Row Level Security)
ALTER TABLE fcm_tokens ENABLE ROW LEVEL SECURITY;

-- Policy: Alle dürfen lesen und schreiben (da keine Auth verwendet wird)
CREATE POLICY "Allow all access to fcm_tokens"
    ON fcm_tokens
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- Automatisch updated_at aktualisieren
CREATE OR REPLACE FUNCTION update_fcm_token_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER fcm_tokens_updated_at
    BEFORE UPDATE ON fcm_tokens
    FOR EACH ROW
    EXECUTE FUNCTION update_fcm_token_timestamp();


-- =====================================================
-- 2. Webhook/Trigger für Timer-Änderungen
-- =====================================================
-- Dieser Trigger ruft eine Supabase Edge Function auf,
-- wenn ein Timer erstellt, gelöscht oder abgeschlossen wird.

-- Zuerst: pg_net Extension aktivieren (für HTTP-Aufrufe aus Trigger)
CREATE EXTENSION IF NOT EXISTS pg_net;

-- Trigger-Funktion: Sendet Timer-Event an Edge Function
CREATE OR REPLACE FUNCTION notify_timer_change()
RETURNS TRIGGER AS $$
DECLARE
    event_type TEXT;
    timer_name TEXT;
    timer_data JSONB;
    supabase_url TEXT := 'https://llqvowmainjrbfsyxtnb.supabase.co';
BEGIN
    -- Event-Typ bestimmen
    IF TG_OP = 'INSERT' THEN
        event_type := 'timer_created';
        timer_name := NEW.name;
        timer_data := to_jsonb(NEW);
    ELSIF TG_OP = 'DELETE' THEN
        event_type := 'timer_deleted';
        timer_name := OLD.name;
        timer_data := to_jsonb(OLD);
    ELSIF TG_OP = 'UPDATE' THEN
        -- Nur benachrichtigen wenn Timer als abgeschlossen markiert wird
        IF NEW.is_completed = true AND (OLD.is_completed = false OR OLD.is_completed IS NULL) THEN
            event_type := 'timer_expired';
            timer_name := NEW.name;
            timer_data := to_jsonb(NEW);
        ELSE
            RETURN NEW;
        END IF;
    END IF;

    -- Edge Function aufrufen (asynchron via pg_net)
    PERFORM net.http_post(
        url := supabase_url || '/functions/v1/send-push-notification',
        headers := jsonb_build_object(
            'Content-Type', 'application/json',
            'Authorization', 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImxscXZvd21haW5qcmJmc3l4dG5iIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjAxMjIwNDAsImV4cCI6MjA3NTY5ODA0MH0.TDNMMXo1ZAstWb6tXLMJfdvVxXVauNrjmDhCM7UyvY0'
        ),
        body := jsonb_build_object(
            'event_type', event_type,
            'timer_name', timer_name,
            'timer_data', timer_data
        )
    );

    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger auf timers-Tabelle setzen
DROP TRIGGER IF EXISTS timer_change_notification ON timers;

CREATE TRIGGER timer_change_notification
    AFTER INSERT OR UPDATE OR DELETE ON timers
    FOR EACH ROW
    EXECUTE FUNCTION notify_timer_change();
