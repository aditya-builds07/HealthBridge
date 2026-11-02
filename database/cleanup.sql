-- ==============================================================================
-- Health Bridge: Migration & Cleanup Script (In-place update)
-- Run this in your Supabase SQL Editor:
-- https://supabase.com/dashboard/project/awegpsbewzbzazsrrnbr/sql
-- ==============================================================================

-- 1. CLEANUP COLUMNS
-- Ensure diagnosed_on exists on medical_history
ALTER TABLE public.medical_history ADD COLUMN IF NOT EXISTS diagnosed_on VARCHAR(50);

-- Safely copy legacy 'date' into 'diagnosed_on' only if 'date' column exists, then drop it
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_schema = 'public' 
          AND table_name = 'medical_history' 
          AND column_name = 'date'
    ) THEN
        EXECUTE 'UPDATE public.medical_history SET diagnosed_on = date WHERE (diagnosed_on IS NULL OR diagnosed_on = '''') AND date IS NOT NULL';
        EXECUTE 'ALTER TABLE public.medical_history DROP COLUMN date';
    END IF;
END $$;

-- Drop profiles.password if it exists (auth is handled via GoTrue)
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

-- 4. SYNC AUTO-INCREMENT SEQUENCES
-- Safely guarantees sequence values are always >= 1 and will never trigger out-of-bounds error
SELECT setval('public.profiles_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.profiles WHERE id > 0), 1), 1));
SELECT setval('public.medications_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medications WHERE id > 0), 1), 1));
SELECT setval('public.medical_history_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_history WHERE id > 0), 1), 1));
SELECT setval('public.medical_records_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM public.medical_records WHERE id > 0), 1), 1));
