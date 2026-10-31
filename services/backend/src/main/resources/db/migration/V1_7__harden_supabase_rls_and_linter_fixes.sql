-- ============================================================================
-- ExamRadar India — Flyway V1.7 Migration: Supabase Linter & RLS Optimization
-- 1. Enable RLS on flyway_schema_history to eliminate critical public exposure
-- 2. Optimize Auth RLS policies with scalar subqueries (select auth.uid())
-- 3. Lock down function search_path on security definer triggers
-- ============================================================================

-- 1. Enable RLS on Flyway schema history
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_tables WHERE schemaname = 'public' AND tablename = 'flyway_schema_history') THEN
        ALTER TABLE public.flyway_schema_history ENABLE ROW LEVEL SECURITY;
    END IF;
END $$;

-- 2. Optimize Profiles RLS Policies (use (select auth.uid()))
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

-- 3. Optimize User Exam Preferences RLS Policies
DROP POLICY IF EXISTS "Users can view own exam preferences" ON public.user_exam_preferences;
CREATE POLICY "Users can view own exam preferences"
    ON public.user_exam_preferences FOR SELECT
    USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can manage own exam preferences" ON public.user_exam_preferences;
CREATE POLICY "Users can manage own exam preferences"
    ON public.user_exam_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 4. Optimize Notification Preferences RLS Policies
DROP POLICY IF EXISTS "Users can manage own notification preferences" ON public.notification_preferences;
CREATE POLICY "Users can manage own notification preferences"
    ON public.notification_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 5. Optimize Device Tokens RLS Policies
DROP POLICY IF EXISTS "Users can manage own device tokens" ON public.device_tokens;
CREATE POLICY "Users can manage own device tokens"
    ON public.device_tokens FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 6. Optimize Checklists RLS Policies
DROP POLICY IF EXISTS "Users can manage own checklists" ON public.application_checklists;
CREATE POLICY "Users can manage own checklists"
    ON public.application_checklists FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- 7. Optimize Checklist Items RLS Policies
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

-- 8. Fix Function Search Path Mutability
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'handle_new_user') THEN
        ALTER FUNCTION public.handle_new_user() SET search_path = public, auth;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_proc WHERE proname = 'auto_confirm_new_user') THEN
        ALTER FUNCTION public.auto_confirm_new_user() SET search_path = public, auth;
    END IF;
END $$;
