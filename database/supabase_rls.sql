-- ============================================================================
-- ExamRadar India — Supabase Row Level Security (RLS) & Security Policies
-- Run this in the Supabase SQL Editor to enforce security boundaries
-- ============================================================================

-- 1. Enable RLS on all tables
ALTER TABLE exam_categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE exams ENABLE ROW LEVEL SECURITY;
ALTER TABLE sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_fetch_runs ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_documents ENABLE ROW LEVEL SECURITY;
ALTER TABLE notices ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE notice_dates ENABLE ROW LEVEL SECURITY;
ALTER TABLE eligibility_rules ENABLE ROW LEVEL SECURITY;

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_exam_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE device_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_checklists ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_checklist_items ENABLE ROW LEVEL SECURITY;

ALTER TABLE notification_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_deliveries ENABLE ROW LEVEL SECURITY;
ALTER TABLE notification_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE parser_failures ENABLE ROW LEVEL SECURITY;
ALTER TABLE source_health_snapshots ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- 2. PUBLIC READ POLICIES (Anonymous and Authenticated can read published catalog)
-- ============================================================================

CREATE POLICY "Public read for exam categories"
    ON exam_categories FOR SELECT
    USING (true);

CREATE POLICY "Public read for active exams"
    ON exams FOR SELECT
    USING (is_active = true);

CREATE POLICY "Public read for active sources"
    ON sources FOR SELECT
    USING (is_enabled = true);

CREATE POLICY "Public read for published notices"
    ON notices FOR SELECT
    USING (status = 'PUBLISHED');

CREATE POLICY "Public read for notice versions of published notices"
    ON notice_versions FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM notices n WHERE n.id = notice_versions.notice_id AND n.status = 'PUBLISHED'
    ));

CREATE POLICY "Public read for notice dates of published notices"
    ON notice_dates FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM notices n WHERE n.id = notice_dates.notice_id AND n.status = 'PUBLISHED'
    ));

CREATE POLICY "Public read for eligibility rules"
    ON eligibility_rules FOR SELECT
    USING (true);

CREATE POLICY "Public read for source health snapshots"
    ON source_health_snapshots FOR SELECT
    USING (true);

-- ============================================================================
-- 3. USER DATA POLICIES (Isolated by Supabase auth.uid())
-- ============================================================================

-- Profiles
CREATE POLICY "Users can view own profile"
    ON profiles FOR SELECT
    USING (auth.uid() = id);

CREATE POLICY "Users can insert own profile"
    ON profiles FOR INSERT
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

CREATE POLICY "Users can delete own profile"
    ON profiles FOR DELETE
    USING (auth.uid() = id);

-- User Exam Preferences
CREATE POLICY "Users can view own exam preferences"
    ON user_exam_preferences FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can manage own exam preferences"
    ON user_exam_preferences FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Notification Preferences
CREATE POLICY "Users can manage own notification preferences"
    ON notification_preferences FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Device Tokens
CREATE POLICY "Users can manage own device tokens"
    ON device_tokens FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Checklists
CREATE POLICY "Users can manage own checklists"
    ON application_checklists FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- Checklist Items
CREATE POLICY "Users can manage own checklist items"
    ON application_checklist_items FOR ALL
    USING (EXISTS (
        SELECT 1 FROM application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = auth.uid()
    ))
    WITH CHECK (EXISTS (
        SELECT 1 FROM application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = auth.uid()
    ));

-- ============================================================================
-- 4. SERVICE ROLE EXCLUSIVE TABLES (No public/user direct modifications)
-- ============================================================================
-- Tables like notification_jobs, notification_deliveries, audit_events,
-- parser_failures, and source_fetch_runs have RLS enabled with NO user policies.
-- By PostgreSQL RLS design, non-service_role callers have zero access.
-- Only the backend / collector runner using the SERVICE_ROLE_KEY or direct DB
-- connection can insert or read these operational tables.
