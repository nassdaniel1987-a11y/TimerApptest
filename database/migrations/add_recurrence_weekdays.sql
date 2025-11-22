-- Migration: Add recurrence_weekdays column to timers table
-- Run this SQL in your Supabase SQL Editor if you already have an existing database

-- Add the new column
ALTER TABLE timers
ADD COLUMN IF NOT EXISTS recurrence_weekdays TEXT;

-- Add comment for documentation
COMMENT ON COLUMN timers.recurrence_weekdays IS 'Comma-separated weekdays (ISO 8601: 1=Mon, 7=Sun), e.g., "1,3,5" for Mon,Wed,Fri';

-- Update recurrence column comment
COMMENT ON COLUMN timers.recurrence IS '"daily", "weekly", "weekdays", "weekends", "custom", null = one-time';
