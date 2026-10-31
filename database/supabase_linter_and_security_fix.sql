-- ============================================================================
-- ExamRadar India — Supabase Security Advisor & Linter Optimization
-- 
-- Run this script in the Supabase SQL Editor:
-- https://supabase.com/dashboard/project/qcdbjbattatozsuhwfyf/sql/new
--
-- Fixes:
-- 1. [CRITICAL] Enables RLS on public.flyway_schema_history with service bypass
-- 2. Optimizes Auth RLS policies with scalar subqueries: (select auth.uid())
-- 3. Locks down search_path on handle_new_user() and auto_confirm_new_user()
-- ============================================================================

-- 0. Clean up any failed Flyway 1.7 migration attempt
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'flyway_schema_history') THEN
        DELETE FROM public.flyway_schema_history WHERE version = '1.7';
    END IF;
END $$;

-- 1. Fix Critical Linter: Enable RLS on Flyway schema history
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'flyway_schema_history') THEN
        ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'flyway_schema_history') THEN
        DROP POLICY IF EXISTS "flyway_schema_history_service_bypass" ON public.flyway_schema_history;
        CREATE POLICY "flyway_schema_history_service_bypass"
            ON public.flyway_schema_history
            FOR ALL
            TO postgres, authenticated, service_role
            USING (true)
            WITH CHECK (true);
    END IF;
END $$;

-- 2. Fix Auth RLS Initialization Plan: Profiles (4 policies)
DROP POLICY IF EXISTS "Users can view own profile" ON public.profiles;
CREATE POLICY "Users can view own profile"
    ON public.profiles FOR SELECT
    USING ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON public.profiles;
CREATE POLICY "Users can insert own profile"
    ON public.profiles FOR INSERT
    WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can update own profile" ON public.profiles;
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE
    USING ((select auth.uid()) = id)
    WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can delete own profile" ON public.profiles;
CREATE POLICY "Users can delete own profile"
    ON public.profiles FOR DELETE
    USING ((select auth.uid()) = id);

-- 3. Fix Auth RLS Initialization Plan: User Exam Preferences (2 policies)
DROP POLICY IF EXISTS "Users can view own exam preferences" ON public.user_exam_preferences;
CREATE POLICY "Users can view own exam preferences"
    ON public.user_exam_preferences FOR SELECT
    USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can manage own exam preferences" ON public.user_exam_preferences;
CREATE POLICY "Users can manage own exam preferences"
    ON public.user_exam_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 4. Fix Auth RLS Initialization Plan: Notification Preferences
DROP POLICY IF EXISTS "Users can manage own notification preferences" ON public.notification_preferences;
CREATE POLICY "Users can manage own notification preferences"
    ON public.notification_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 5. Fix Auth RLS Initialization Plan: Device Tokens
DROP POLICY IF EXISTS "Users can manage own device tokens" ON public.device_tokens;
CREATE POLICY "Users can manage own device tokens"
    ON public.device_tokens FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 6. Fix Auth RLS Initialization Plan: Application Checklists
DROP POLICY IF EXISTS "Users can manage own checklists" ON public.application_checklists;
CREATE POLICY "Users can manage own checklists"
    ON public.application_checklists FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 7. Fix Auth RLS Initialization Plan: Application Checklist Items
DROP POLICY IF EXISTS "Users can manage own checklist items" ON public.application_checklist_items;
CREATE POLICY "Users can manage own checklist items"
    ON public.application_checklist_items FOR ALL
    USING (EXISTS (
        SELECT 1 FROM public.application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = (select auth.uid())
    ))
    WITH CHECK (EXISTS (
        SELECT 1 FROM public.application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = (select auth.uid())
    ));

-- 8. Fix Function Search Path Mutability: handle_new_user and auto_confirm_new_user
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, display_name, target_state, language_preference)
    VALUES (
        new.id,
        COALESCE(new.raw_user_meta_data->>'full_name', split_part(new.email, '@', 1)),
        'ALL_INDIA',
        'en'
    )
    ON CONFLICT (id) DO NOTHING;

    INSERT INTO public.notification_preferences (user_id)
    VALUES (new.id)
    ON CONFLICT (user_id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth;

CREATE OR REPLACE FUNCTION public.auto_confirm_new_user()
RETURNS TRIGGER AS $$
BEGIN
    NEW.email_confirmed_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth;
