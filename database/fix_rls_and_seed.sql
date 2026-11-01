-- ==============================================================================
-- Health Bridge: Disable RLS and Insert Demo Seed Data
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/awegpsbewzbzazsrrnbr/sql
-- ==============================================================================

-- 1. DISABLE ROW LEVEL SECURITY (Required for demo app access via anon/authenticated)
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medications DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_history DISABLE ROW LEVEL SECURITY;

GRANT ALL ON public.profiles TO anon, authenticated;
GRANT ALL ON public.medical_records TO anon, authenticated;
GRANT ALL ON public.medications TO anon, authenticated;
GRANT ALL ON public.medical_history TO anon, authenticated;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 2. AUTO-CONFIRM RAHUL'S AUTH USER IF CREATED
UPDATE auth.users
SET email_confirmed_at = NOW()
WHERE email = 'rahul@healthbridge.com' AND email_confirmed_at IS NULL;

-- 3. INSERT DEMO PROFILE (Rahul Sharma, ID = 1)
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

SELECT setval('public.profiles_id_seq', COALESCE((SELECT MAX(id) FROM public.profiles), 1));

-- 4. INSERT 3 DEMO MEDICATIONS
DELETE FROM public.medications WHERE profile_id = 1;

INSERT INTO public.medications (profile_id, name, dosage, frequency, start_date, status) VALUES
(1, 'Vitamin D3', '60,000 IU', 'Once weekly', '2026-01-15', 'Active'),
(1, 'Cetirizine', '10 mg', 'As needed', '2026-02-01', 'Active'),
(1, 'Paracetamol', '650 mg', 'SOS', '2026-03-10', 'Completed');

SELECT setval('public.medications_id_seq', COALESCE((SELECT MAX(id) FROM public.medications), 1));

-- 5. INSERT 3 DEMO MEDICAL HISTORY ENTRIES
DELETE FROM public.medical_history WHERE profile_id = 1;

INSERT INTO public.medical_history (profile_id, condition, notes, diagnosed_on, date) VALUES
(1, 'Seasonal allergic rhinitis', 'Fictional demo entry: pollen trigger', '2024-03-10', '2024-03-10'),
(1, 'Mild Asthmatic Bronchitis', 'Triggered by dust, resolved with inhaler', '2022-11-05', '2022-11-05'),
(1, 'Appendectomy', 'Laparoscopic surgery, fully recovered', '2019-07-20', '2019-07-20');

SELECT setval('public.medical_history_id_seq', COALESCE((SELECT MAX(id) FROM public.medical_history), 1));

