-- Timer App Database Schema for Supabase
-- Run this SQL in your Supabase SQL Editor to set up the database

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Categories Table
CREATE TABLE IF NOT EXISTS categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    color TEXT NOT NULL, -- Hex format: #RRGGBB
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Timer Templates Table
CREATE TABLE IF NOT EXISTS timer_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    defaultTime TEXT NOT NULL, -- Format: "HH:mm"
    category TEXT NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Timers Table
CREATE TABLE IF NOT EXISTS timers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    target_time TIMESTAMPTZ NOT NULL, -- ISO 8601 with offset
    category TEXT NOT NULL,
    note TEXT,
    is_completed BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    recurrence TEXT, -- "daily", "weekly", "weekdays", "weekends", "custom", null = one-time
    recurrence_end_date TIMESTAMPTZ, -- End date for recurring timers
    recurrence_weekdays TEXT -- Comma-separated weekdays (ISO 8601: 1=Mon, 7=Sun), e.g., "1,3,5" for Mon,Wed,Fri
);

-- QR Codes Table
CREATE TABLE IF NOT EXISTS qr_codes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    time TEXT NOT NULL, -- Format: "HH:mm"
    category TEXT NOT NULL,
    note TEXT,
    isflexible BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Enable Row Level Security (RLS) on all tables
ALTER TABLE categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE timer_templates ENABLE ROW LEVEL SECURITY;
ALTER TABLE timers ENABLE ROW LEVEL SECURITY;
ALTER TABLE qr_codes ENABLE ROW LEVEL SECURITY;

-- Create policies to allow public access (adjust based on your security requirements)
-- For development: Allow all operations with anon key

-- Categories policies
CREATE POLICY "Enable read access for all users" ON categories FOR SELECT USING (true);
CREATE POLICY "Enable insert for all users" ON categories FOR INSERT WITH CHECK (true);
CREATE POLICY "Enable update for all users" ON categories FOR UPDATE USING (true);
CREATE POLICY "Enable delete for all users" ON categories FOR DELETE USING (true);

-- Timer Templates policies
CREATE POLICY "Enable read access for all users" ON timer_templates FOR SELECT USING (true);
CREATE POLICY "Enable insert for all users" ON timer_templates FOR INSERT WITH CHECK (true);
CREATE POLICY "Enable update for all users" ON timer_templates FOR UPDATE USING (true);
CREATE POLICY "Enable delete for all users" ON timer_templates FOR DELETE USING (true);

-- Timers policies
CREATE POLICY "Enable read access for all users" ON timers FOR SELECT USING (true);
CREATE POLICY "Enable insert for all users" ON timers FOR INSERT WITH CHECK (true);
CREATE POLICY "Enable update for all users" ON timers FOR UPDATE USING (true);
CREATE POLICY "Enable delete for all users" ON timers FOR DELETE USING (true);

-- QR Codes policies
CREATE POLICY "Enable read access for all users" ON qr_codes FOR SELECT USING (true);
CREATE POLICY "Enable insert for all users" ON qr_codes FOR INSERT WITH CHECK (true);
CREATE POLICY "Enable update for all users" ON qr_codes FOR UPDATE USING (true);
CREATE POLICY "Enable delete for all users" ON qr_codes FOR DELETE USING (true);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_timers_target_time ON timers(target_time);
CREATE INDEX IF NOT EXISTS idx_timers_category ON timers(category);
CREATE INDEX IF NOT EXISTS idx_timers_is_completed ON timers(is_completed);
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);
