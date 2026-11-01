-- ==============================================================================
-- Health Bridge — Database Schema, Supabase Auth Integration & Demo Seed
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
-- 2. CREATE TABLES (Public Schema, Linked to Supabase auth.users)
-- ------------------------------------------------------------------------------

-- Profiles Table
CREATE TABLE public.profiles (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth.users(id) ON DELETE CASCADE, -- Supabase Auth link
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255),                                    -- Demo fallback
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
    frequency VARCHAR(100),   -- from Medication.java & PRD Section 7
    start_date VARCHAR(50),   -- from sprint dialog spec
    status VARCHAR(50),       -- from sprint dialog spec
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Medical History Table
CREATE TABLE public.medical_history (
    id SERIAL PRIMARY KEY,
    profile_id INTEGER NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    condition VARCHAR(255) NOT NULL,
    notes TEXT,
    diagnosed_on VARCHAR(50), -- from MedicalHistory.java & PRD Section 7
    date VARCHAR(50),         -- from sprint dialog spec
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

-- Optional Production-Grade RLS (uncomment when enabling RLS in production):
/*
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medications ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_history ENABLE ROW LEVEL SECURITY;

CREATE POLICY "profiles_select_own" ON public.profiles
FOR SELECT TO authenticated
USING ((select auth.uid()) = user_id);

CREATE POLICY "profiles_update_own" ON public.profiles
FOR UPDATE TO authenticated
USING ((select auth.uid()) = user_id)
WITH CHECK ((select auth.uid()) = user_id);

CREATE POLICY "medical_records_select_own" ON public.medical_records
FOR SELECT TO authenticated
USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = (select auth.uid())));
*/

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

DROP POLICY IF EXISTS "Allow anon select on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon insert on healthbridge-files" ON storage.objects;
DROP POLICY IF EXISTS "Allow anon update on healthbridge-files" ON storage.objects;

CREATE POLICY "Allow anon select on healthbridge-files"
ON storage.objects FOR SELECT TO anon, authenticated
USING (bucket_id = 'healthbridge-files');

CREATE POLICY "Allow anon insert on healthbridge-files"
ON storage.objects FOR INSERT TO anon, authenticated
WITH CHECK (bucket_id = 'healthbridge-files');

CREATE POLICY "Allow anon update on healthbridge-files"
ON storage.objects FOR UPDATE TO anon, authenticated
USING (bucket_id = 'healthbridge-files');

-- ------------------------------------------------------------------------------
-- 6. CONFIRM AUTH DEMO USER & SEED DATA
-- ------------------------------------------------------------------------------
-- Auto-confirm rahul@healthbridge.com in Supabase Auth if created
UPDATE auth.users
SET email_confirmed_at = NOW()
WHERE email = 'rahul@healthbridge.com' AND email_confirmed_at IS NULL;

-- Seed Profile (id = 1)
INSERT INTO public.profiles (
    id, user_id, full_name, email, password, age, gender, blood_group,
    height_cm, weight_kg, allergies, emergency_contact_name, emergency_contact_phone
) VALUES (
    1,
    (SELECT id FROM auth.users WHERE email = 'rahul@healthbridge.com' LIMIT 1),
    'Rahul Sharma',
    'rahul@healthbridge.com',
    '123456',
    24,
    'Male',
    'O+',
    172.00,
    68.50,
    'Penicillin',
    'Amit Sharma',
    '9000000001'
)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    password = EXCLUDED.password,
    age = EXCLUDED.age,
    gender = EXCLUDED.gender,
    blood_group = EXCLUDED.blood_group,
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    allergies = EXCLUDED.allergies,
    emergency_contact_name = EXCLUDED.emergency_contact_name,
    emergency_contact_phone = EXCLUDED.emergency_contact_phone;

SELECT setval('public.profiles_id_seq', (SELECT MAX(id) FROM public.profiles));

-- Seed 3 Medications for Rahul
INSERT INTO public.medications (profile_id, name, dosage, frequency, start_date, status) VALUES
(1, 'Vitamin D3', '60,000 IU', 'Once weekly', '2026-01-15', 'Active'),
(1, 'Cetirizine', '10 mg', 'As needed', '2026-02-01', 'Active'),
(1, 'Paracetamol', '650 mg', 'SOS', '2026-03-10', 'Completed');

-- Seed 3 Medical History Entries for Rahul
INSERT INTO public.medical_history (profile_id, condition, notes, diagnosed_on, date) VALUES
(1, 'Seasonal allergic rhinitis', 'Fictional demo entry: pollen trigger', '2024-03-10', '2024-03-10'),
(1, 'Mild Asthmatic Bronchitis', 'Triggered by dust, resolved with inhaler', '2022-11-05', '2022-11-05'),
(1, 'Appendectomy', 'Laparoscopic surgery, fully recovered', '2019-07-20', '2019-07-20');

SELECT setval('public.medications_id_seq', (SELECT MAX(id) FROM public.medications));
SELECT setval('public.medical_history_id_seq', (SELECT MAX(id) FROM public.medical_history));
