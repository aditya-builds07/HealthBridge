-- ==============================================================================
-- Health Bridge: Migration & Cleanup Script (In-place update)
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/awegpsbewzbzazsrrnbr/sql
-- ==============================================================================

-- 1. CLEANUP COLUMNS
-- Copy date into diagnosed_on where diagnosed_on is empty, then drop date
UPDATE public.medical_history
SET diagnosed_on = date
WHERE (diagnosed_on IS NULL OR diagnosed_on = '') AND date IS NOT NULL;

ALTER TABLE public.medical_history DROP COLUMN IF EXISTS date;

-- Drop profiles.password (not used by Java code; auth handled via GoTrue)
ALTER TABLE public.profiles DROP COLUMN IF EXISTS password;

-- Normalize medical_records category
UPDATE public.medical_records
SET category = 'Prescription'
WHERE category = 'Prescriptions';

-- Ensure medications has start_date and status columns
ALTER TABLE public.medications ADD COLUMN IF NOT EXISTS start_date VARCHAR(50);
ALTER TABLE public.medications ADD COLUMN IF NOT EXISTS status VARCHAR(50);

-- 2. DISABLE RLS ON ALL 4 TABLES (Demo Mode)
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medications DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_history DISABLE ROW LEVEL SECURITY;

GRANT ALL ON public.profiles TO anon, authenticated;
GRANT ALL ON public.medical_records TO anon, authenticated;
GRANT ALL ON public.medications TO anon, authenticated;
GRANT ALL ON public.medical_history TO anon, authenticated;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 3. STORAGE: MAKE BUCKET PUBLIC & CONFIGURE POLICIES
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'healthbridge-files',
    'healthbridge-files',
    true,
    10485760,
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

-- 4. LINK RAHUL'S USER_ID & SEED PROFILE
UPDATE auth.users
SET email_confirmed_at = NOW()
WHERE email = 'rahul@healthbridge.com' AND email_confirmed_at IS NULL;

INSERT INTO public.profiles (
    id, user_id, full_name, email, age, gender, blood_group,
    height_cm, weight_kg, allergies, emergency_contact_name, emergency_contact_phone
) VALUES (
    1,
    (SELECT id FROM auth.users WHERE email = 'rahul@healthbridge.com' LIMIT 1),
    'Rahul Sharma',
    'rahul@healthbridge.com',
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
    user_id = COALESCE((SELECT id FROM auth.users WHERE email = 'rahul@healthbridge.com' LIMIT 1), profiles.user_id),
    full_name = EXCLUDED.full_name,
    email = EXCLUDED.email,
    age = EXCLUDED.age,
    gender = EXCLUDED.gender,
    blood_group = EXCLUDED.blood_group,
    height_cm = EXCLUDED.height_cm,
    weight_kg = EXCLUDED.weight_kg,
    allergies = EXCLUDED.allergies,
    emergency_contact_name = EXCLUDED.emergency_contact_name,
    emergency_contact_phone = EXCLUDED.emergency_contact_phone;

SELECT setval('public.profiles_id_seq', COALESCE((SELECT MAX(id) FROM public.profiles), 1));

-- 5. SEED MEDICATIONS WITH REAL VALUES
DELETE FROM public.medications WHERE profile_id = 1;

INSERT INTO public.medications (profile_id, name, dosage, frequency, start_date, status) VALUES
(1, 'Vitamin D3', '60,000 IU', 'Once weekly', '2026-01-15', 'Active'),
(1, 'Cetirizine', '10 mg', 'As needed', '2026-02-01', 'Active'),
(1, 'Paracetamol', '650 mg', 'SOS', '2026-03-10', 'Completed');

SELECT setval('public.medications_id_seq', COALESCE((SELECT MAX(id) FROM public.medications), 1));

-- 6. SEED MEDICAL HISTORY
DELETE FROM public.medical_history WHERE profile_id = 1;

INSERT INTO public.medical_history (profile_id, condition, notes, diagnosed_on) VALUES
(1, 'Seasonal allergic rhinitis', 'Fictional demo entry: pollen trigger', '2024-03-10'),
(1, 'Mild Asthmatic Bronchitis', 'Triggered by dust, resolved with inhaler', '2022-11-05'),
(1, 'Appendectomy', 'Laparoscopic surgery, fully recovered', '2019-07-20');

SELECT setval('public.medical_history_id_seq', COALESCE((SELECT MAX(id) FROM public.medical_history), 1));

