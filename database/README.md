# Database Setup

This directory contains the SQL schema for the Timer App Supabase database.

## Initial Setup

To set up your Supabase database for the first time:

1. Go to your Supabase project dashboard at https://supabase.com/dashboard
2. Navigate to **SQL Editor**
3. Create a new query
4. Copy and paste the contents of `schema.sql`
5. Click **Run** to execute the SQL

This will create all necessary tables:
- `categories` - Timer categories with colors
- `timer_templates` - Reusable timer templates
- `timers` - Active and completed timers
- `qr_codes` - QR code data for quick timer creation

## Row Level Security (RLS)

The schema includes RLS policies that allow public access for development.

**⚠️ Security Warning:** The default policies allow all operations with the anon key. For production, you should implement proper authentication and restrict access based on user roles.

To implement user-based security:
1. Enable Supabase Authentication
2. Update the RLS policies to check `auth.uid()`
3. Add user_id columns to tables as needed

## Database Health Check

The GitHub Action `.github/workflows/keep-database-alive.yml` automatically pings your Supabase database every 4 days to prevent it from pausing (on Supabase free tier).

The workflow will succeed whether or not the tables exist, but you should see a message indicating if you need to run the schema.
