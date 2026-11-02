-- ==============================================================================
-- Health Bridge — Database Schema & Access Control
-- Platform: Android (Java + XML + Retrofit2)
-- Supabase Project: https://awegpsbewzbzazsrrnbr.supabase.co
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. DROP EXISTING TABLES (Clean Slate)
-- ------------------------------------------------------------------------------
DROP TABLE IF EXISTS public.medical_records CASCADE;
DROP TABLE IF EXISTS public.medications CASCADE;
DROP TABLE IF EXISTS public.medical_history CASCADE;
DROP TABLE IF EXISTS public.profiles CASCADE;

-- ------------------------------------------------------------------------------
-- 2. CREATE TABLES (Public Schema, Integer Primary Keys, Auth User FK)
-- ------------------------------------------------------------------------------

-- Profiles Table (Cleaned: no plaintext password column)
CREATE TABLE public.profiles (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    age INTEGER,
    gender VARCHAR(50),
    blood_group VARCHAR(10),
    height_cm NUMERIC(5, 2),
    weight_kg NUMERIC(5, 2),
    allergies TEXT,
    emergency_contact_name VARCHAR(255),
    emergency_contact_phone VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Medical Records Table
CREATE TABLE public.medical_records (
    id SERIAL PRIMARY KEY,
    profile_id INTEGER NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('Prescription', 'Lab Report', 'X-Ray', 'Summary')),
    file_url TEXT,
    mime_type VARCHAR(100),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Medications Table
CREATE TABLE public.medications (
    id SERIAL PRIMARY KEY,
    profile_id INTEGER NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    dosage VARCHAR(100),
    frequency VARCHAR(100),
    start_date VARCHAR(50),
    status VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Medical History Table (Cleaned: single diagnosed_on column)
CREATE TABLE public.medical_history (
    id SERIAL PRIMARY KEY,
    profile_id INTEGER NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    condition VARCHAR(255) NOT NULL,
    notes TEXT,
    diagnosed_on VARCHAR(50),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- ------------------------------------------------------------------------------
-- 3. INDEXES
-- ------------------------------------------------------------------------------
CREATE INDEX idx_profiles_user_id ON public.profiles(user_id);
CREATE INDEX idx_profiles_email ON public.profiles(email);
CREATE INDEX idx_medical_records_profile_id ON public.medical_records(profile_id);
CREATE INDEX idx_medications_profile_id ON public.medications(profile_id);
CREATE INDEX idx_medical_history_profile_id ON public.medical_history(profile_id);

-- ------------------------------------------------------------------------------
-- 4. ACCESS CONTROL (Demo Mode — RLS Disabled & Full Grants)
-- ------------------------------------------------------------------------------
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medications DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_history DISABLE ROW LEVEL SECURITY;

GRANT ALL ON public.profiles TO anon, authenticated;
GRANT ALL ON public.medical_records TO anon, authenticated;
GRANT ALL ON public.medications TO anon, authenticated;
GRANT ALL ON public.medical_history TO anon, authenticated;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- ------------------------------------------------------------------------------
-- 5. STORAGE BUCKET & POLICIES (healthbridge-files)
-- ------------------------------------------------------------------------------
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'healthbridge-files',
    'healthbridge-files',
    true,
    10485760, -- 10 MB limit
    ARRAY['image/jpeg', 'image/png', 'application/pdf']
)
ON CONFLICT (id) DO UPDATE SET
    public = true,
    file_size_limit = 10485760,
    allowed_mime_types = ARRAY['image/jpeg', 'image/png', 'application/pdf'];

DROP POLICY IF EXISTS "Allow select on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow insert on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow update on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon select on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon insert on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon update on healthbridge-files" ON storage.objects;

CREATE POLICY "Allow select on healthbridge-files"
ON storage.objects FOR SELECT TO anon, authenticated
USING (bucket_id = 'healthbridge-files');

CREATE POLICY "Allow insert on healthbridge-files"
ON storage.objects FOR INSERT TO anon, authenticated
WITH CHECK (bucket_id = 'healthbridge-files');

CREATE POLICY "Allow update on healthbridge-files"
ON storage.objects FOR UPDATE TO anon, authenticated
USING (bucket_id = 'healthbridge-files');

-- ------------------------------------------------------------------------------
-- 6. SYNC AUTO-INCREMENT SEQUENCES
-- ------------------------------------------------------------------------------
SELECT setval('public.profiles_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.profiles WHERE id > 0), 1), 1));
SELECT setval('public.medications_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medications WHERE id > 0), 1), 1));
SELECT setval('public.medical_history_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_history WHERE id > 0), 1), 1));
SELECT setval('public.medical_records_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_records WHERE id > 0), 1), 1));
