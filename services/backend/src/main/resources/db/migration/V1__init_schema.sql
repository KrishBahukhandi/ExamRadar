-- ============================================================================
-- ExamRadar India — Initial Database Schema (Flyway V1 Migration)
-- Normalized PostgreSQL Schema for Catalog, Ingestion, Notifications & Auditing
-- ============================================================================

-- Enable UUID extension if not already present
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- 1. PUBLIC CATALOGUE
-- ============================================================================

CREATE TABLE IF NOT EXISTS exam_categories (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL UNIQUE,
    description TEXT,
    display_order INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS exams (
    id VARCHAR(64) PRIMARY KEY,
    category_id VARCHAR(64) NOT NULL REFERENCES exam_categories(id) ON DELETE RESTRICT,
    organization VARCHAR(128) NOT NULL,
    exam_name VARCHAR(255) NOT NULL,
    exam_code VARCHAR(64),
    official_portal_url VARCHAR(512) NOT NULL,
    description TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exams_category ON exams(category_id);
CREATE INDEX idx_exams_organization ON exams(organization);

CREATE TABLE IF NOT EXISTS sources (
    id VARCHAR(64) PRIMARY KEY,
    organization VARCHAR(128) NOT NULL,
    source_name VARCHAR(255) NOT NULL,
    official_base_url VARCHAR(512) NOT NULL,
    source_type VARCHAR(32) NOT NULL,      -- HTML_SCRAPE, RSS_FEED, JSON_API, PDF_DIRECTORY
    fetch_method VARCHAR(32) NOT NULL,     -- HTTP_GET, CONDITIONAL_GET
    polling_interval_minutes INT DEFAULT 60,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parser_version VARCHAR(32) NOT NULL,
    last_attempted_fetch TIMESTAMPTZ,
    last_successful_fetch TIMESTAMPTZ,
    last_successful_content_hash VARCHAR(64),
    consecutive_failures INT NOT NULL DEFAULT 0,
    last_error_message TEXT,
    access_policy_notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS source_fetch_runs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id VARCHAR(64) NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL,           -- SUCCESS, FAILED, NO_CHANGE
    http_status INT,
    response_time_ms BIGINT,
    content_hash VARCHAR(64),
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_fetch_runs_source_time ON source_fetch_runs(source_id, created_at DESC);

CREATE TABLE IF NOT EXISTS source_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id VARCHAR(64) NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    run_id UUID REFERENCES source_fetch_runs(id) ON DELETE SET NULL,
    document_url VARCHAR(1024) NOT NULL,
    document_type VARCHAR(32) NOT NULL,    -- HTML, PDF, IMAGE
    content_hash VARCHAR(64) NOT NULL,
    raw_text_preview TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_source_docs_hash ON source_documents(content_hash);

CREATE TABLE IF NOT EXISTS notices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exam_id VARCHAR(64) NOT NULL REFERENCES exams(id) ON DELETE RESTRICT,
    source_id VARCHAR(64) NOT NULL REFERENCES sources(id) ON DELETE RESTRICT,
    notice_type VARCHAR(32) NOT NULL,      -- RECRUITMENT, ADMIT_CARD, RESULT, ANSWER_KEY, CORRIGENDUM, POSTPONEMENT
    title VARCHAR(512) NOT NULL,
    summary TEXT,
    official_notice_url VARCHAR(1024) NOT NULL,
    official_apply_url VARCHAR(1024),
    status VARCHAR(32) NOT NULL DEFAULT 'DETECTED', -- DETECTED, EXTRACTED, VALIDATION_REQUIRED, PUBLISHED, SUPERSEDED, CANCELLED, REJECTED
    published_date DATE,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, VERIFIED, AUTOMATED_HIGH_CONFIDENCE, QUARANTINED
    last_verified_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notices_exam_type ON notices(exam_id, notice_type);
CREATE INDEX idx_notices_status_date ON notices(status, published_date DESC);

CREATE TABLE IF NOT EXISTS notice_versions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notice_id UUID NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    version_number INT NOT NULL,
    document_id UUID REFERENCES source_documents(id) ON DELETE SET NULL,
    content_hash VARCHAR(64) NOT NULL,
    change_summary TEXT,
    extracted_data JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notice_version UNIQUE (notice_id, version_number)
);

CREATE TABLE IF NOT EXISTS notice_dates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notice_id UUID NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    date_type VARCHAR(32) NOT NULL,        -- APPLICATION_START, APPLICATION_DEADLINE, CORRECTION_WINDOW, EXAM_DATE, ADMIT_CARD_RELEASE, RESULT_DATE
    event_date DATE NOT NULL,
    event_time TIMESTAMPTZ,
    is_tentative BOOLEAN NOT NULL DEFAULT FALSE,
    is_revised BOOLEAN NOT NULL DEFAULT FALSE,
    original_date_str VARCHAR(128),
    source_text_snippet TEXT,
    confidence_score NUMERIC(3, 2) DEFAULT 1.0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notice_dates_deadline ON notice_dates(date_type, event_date);

CREATE TABLE IF NOT EXISTS eligibility_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    exam_id VARCHAR(64) NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    notice_id UUID REFERENCES notices(id) ON DELETE SET NULL,
    rule_type VARCHAR(32) NOT NULL,        -- AGE_LIMIT, EDUCATION, ATTEMPTS, NATIONALITY
    min_age INT,
    max_age INT,
    cut_off_date DATE,
    required_degree VARCHAR(255),
    rule_description TEXT NOT NULL,
    source_version_id UUID REFERENCES notice_versions(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 2. USER DATA
-- ============================================================================

CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY,                   -- Maps to Supabase auth.users(id)
    display_name VARCHAR(128),
    graduation_status VARCHAR(32),         -- PURSUING_FINAL_YEAR, GRADUATED
    graduation_year INT,
    education_level VARCHAR(64),           -- GRADUATE_ANY, ENGINEERING, COMMERCE, LAW, POST_GRADUATE
    target_state VARCHAR(64) DEFAULT 'ALL_INDIA',
    language_preference VARCHAR(16) DEFAULT 'en',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS user_exam_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    exam_id VARCHAR(64) REFERENCES exams(id) ON DELETE CASCADE,
    category_id VARCHAR(64) REFERENCES exam_categories(id) ON DELETE CASCADE,
    notify_new_notices BOOLEAN NOT NULL DEFAULT TRUE,
    notify_deadlines BOOLEAN NOT NULL DEFAULT TRUE,
    notify_results BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_exam UNIQUE (user_id, exam_id)
);

CREATE INDEX idx_user_exam_prefs ON user_exam_preferences(user_id);

CREATE TABLE IF NOT EXISTS notification_preferences (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE UNIQUE,
    reminder_7_days BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_3_days BOOLEAN NOT NULL DEFAULT TRUE,
    reminder_1_day BOOLEAN NOT NULL DEFAULT TRUE,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS device_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token VARCHAR(512) NOT NULL UNIQUE,
    device_type VARCHAR(32) NOT NULL DEFAULT 'ANDROID',
    app_version VARCHAR(32),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_tokens_user ON device_tokens(user_id);

CREATE TABLE IF NOT EXISTS application_checklists (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    exam_id VARCHAR(64) NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED', -- NOT_STARTED, IN_PROGRESS, SUBMITTED
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_user_checklist UNIQUE (user_id, exam_id)
);

CREATE TABLE IF NOT EXISTS application_checklist_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    checklist_id UUID NOT NULL REFERENCES application_checklists(id) ON DELETE CASCADE,
    item_key VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_checklist_item UNIQUE (checklist_id, item_key)
);

-- ============================================================================
-- 3. NOTIFICATIONS
-- ============================================================================

CREATE TABLE IF NOT EXISTS notification_jobs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    notice_id UUID NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,       -- NEW_NOTICE, DEADLINE_7D, DEADLINE_3D, DEADLINE_1D, DEADLINE_REVISED, RESULT_OUT
    scheduled_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- PENDING, PROCESSING, SENT, FAILED, CANCELLED
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_jobs_status_sched ON notification_jobs(status, scheduled_at);

CREATE TABLE IF NOT EXISTS notification_deliveries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    job_id UUID NOT NULL REFERENCES notification_jobs(id) ON DELETE CASCADE,
    device_token_id UUID REFERENCES device_tokens(id) ON DELETE SET NULL,
    provider_response_id VARCHAR(255),
    status VARCHAR(32) NOT NULL,           -- ACCEPTED, DELIVERED, FAILED, INVALID_TOKEN
    attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    error_details TEXT
);

CREATE TABLE IF NOT EXISTS notification_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    notice_id UUID NOT NULL REFERENCES notices(id) ON DELETE CASCADE,
    event_type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- 4. OPERATIONS & AUDITING
-- ============================================================================

CREATE TABLE IF NOT EXISTS audit_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    actor_id VARCHAR(128) NOT NULL,
    action VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64) NOT NULL,
    entity_id VARCHAR(128) NOT NULL,
    old_values JSONB,
    new_values JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS parser_failures (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id VARCHAR(64) NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    document_url VARCHAR(1024) NOT NULL,
    parser_version VARCHAR(32) NOT NULL,
    error_stack TEXT NOT NULL,
    raw_snippet TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS source_health_snapshots (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    source_id VARCHAR(64) NOT NULL REFERENCES sources(id) ON DELETE CASCADE,
    total_runs INT NOT NULL DEFAULT 0,
    successful_runs INT NOT NULL DEFAULT 0,
    failed_runs INT NOT NULL DEFAULT 0,
    average_latency_ms BIGINT NOT NULL DEFAULT 0,
    quarantined_notices_count INT NOT NULL DEFAULT 0,
    snapshot_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_source_health_time ON source_health_snapshots(source_id, snapshot_timestamp DESC);
