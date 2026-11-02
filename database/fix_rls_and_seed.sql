-- ==============================================================================
-- Health Bridge: Disable RLS and Configure Permissions
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/awegpsbewzbzazsrrnbr/sql
-- ==============================================================================

-- 1. DISABLE ROW LEVEL SECURITY (Required for demo app access via anon/authenticated)
ALTER TABLE public.profiles DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_records DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medications DISABLE ROW LEVEL SECURITY;
ALTER TABLE public.medical_history DISABLE ROW LEVEL SECURITY;

-- 2. GRANT ACCESS TO ANON AND AUTHENTICATED ROLES
GRANT ALL ON public.profiles TO anon, authenticated;
GRANT ALL ON public.medical_records TO anon, authenticated;
GRANT ALL ON public.medications TO anon, authenticated;
GRANT ALL ON public.medical_history TO anon, authenticated;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO anon, authenticated;

-- 3. SYNC AUTO-INCREMENT SEQUENCES
-- Safely guarantees sequence values are always >= 1 and prevents out-of-bounds errors
SELECT setval('public.profiles_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.profiles WHERE id > 0), 1), 1));
SELECT setval('public.medications_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medications WHERE id > 0), 1), 1));
SELECT setval('public.medical_history_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_history WHERE id > 0), 1), 1));
SELECT setval('public.medical_records_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_records WHERE id > 0), 1), 1));
