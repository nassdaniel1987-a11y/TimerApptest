-- Fix timer_templates Tabelle: Spaltenname und fehlende Spalten
-- Dieses SQL im Supabase SQL Editor ausführen!

-- 1. Spalte "defaultTime" umbenennen zu "default_time" (snake_case wie im Kotlin-Model)
ALTER TABLE timer_templates RENAME COLUMN "defaultTime" TO default_time;

-- 2. Fehlende Spalten hinzufügen
ALTER TABLE timer_templates ADD COLUMN IF NOT EXISTS klasse TEXT;
ALTER TABLE timer_templates ADD COLUMN IF NOT EXISTS source_device_id TEXT;
