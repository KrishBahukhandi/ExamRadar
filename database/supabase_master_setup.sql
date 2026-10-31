-- ============================================================================
-- ExamRadar India — Complete Supabase Database Master Setup
-- Project Reference: qcdbjbattatozsuhwfyf
-- Supabase REST URL: https://qcdbjbattatozsuhwfyf.supabase.co
--
-- INSTRUCTIONS:
-- 1. Open your Supabase Dashboard: https://supabase.com/dashboard/project/qcdbjbattatozsuhwfyf
-- 2. In the left navigation, click "SQL Editor"
-- 3. Click "New Query"
-- 4. Paste this entire script into the editor and click "Run" (or Cmd+Enter / Ctrl+Enter)
-- ============================================================================

-- ============================================================================
-- PART 1: CORE SCHEMA (Extensions, Tables, Foreign Keys, Indexes)
-- ============================================================================

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 1.1 Exam Categories
CREATE TABLE IF NOT EXISTS exam_categories (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    slug VARCHAR(64) NOT NULL UNIQUE,
    description TEXT,
    display_order INT DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 1.2 Examinations
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

CREATE INDEX IF NOT EXISTS idx_exams_category ON exams(category_id);
CREATE INDEX IF NOT EXISTS idx_exams_organization ON exams(organization);

-- 1.3 Official Sources Registry
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

-- 1.4 Source Fetch Runs & Audits
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

CREATE INDEX IF NOT EXISTS idx_fetch_runs_source_time ON source_fetch_runs(source_id, created_at DESC);

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

CREATE INDEX IF NOT EXISTS idx_source_docs_hash ON source_documents(content_hash);

-- 1.5 Examination Notices
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

CREATE INDEX IF NOT EXISTS idx_notices_exam_type ON notices(exam_id, notice_type);
CREATE INDEX IF NOT EXISTS idx_notices_status_date ON notices(status, published_date DESC);

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

CREATE INDEX IF NOT EXISTS idx_notice_dates_deadline ON notice_dates(date_type, event_date);

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

-- 1.6 User Profiles & Isolated Preferences
-- Linked directly to Supabase Auth (auth.users)
CREATE TABLE IF NOT EXISTS profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
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

CREATE INDEX IF NOT EXISTS idx_user_exam_prefs ON user_exam_preferences(user_id);

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

CREATE INDEX IF NOT EXISTS idx_device_tokens_user ON device_tokens(user_id);

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

-- 1.7 Operational & Worker Tables
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

CREATE INDEX IF NOT EXISTS idx_notif_jobs_status_sched ON notification_jobs(status, scheduled_at);

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

CREATE INDEX IF NOT EXISTS idx_source_health_time ON source_health_snapshots(source_id, snapshot_timestamp DESC);

-- ============================================================================
-- PART 2: CATALOGUE SEED DATA (Categories, Sources, Exams)
-- ============================================================================

-- ============================================================================
-- ExamRadar India — Seed Catalogue (V1_1 Migration)
-- Real official exam categories, examinations and official sources registry
-- ============================================================================

-- 1. Exam Categories
INSERT INTO exam_categories (id, name, slug, description, display_order) VALUES
('cat-upsc', 'UPSC Examinations', 'upsc', 'Central Civil Services, Engineering, Defence & Combined Medical Services', 1),
('cat-ssc', 'SSC Examinations', 'ssc', 'Staff Selection Commission recruitment for Central Ministries & Departments', 2),
('cat-banking', 'Banking & Financial', 'banking', 'Public Sector Banks, IBPS PO, Clerk, SO & SBI Recruitment', 3),
('cat-regulatory', 'Regulatory Bodies', 'regulatory', 'Reserve Bank of India, SEBI, NABARD & IRDAI Officer Recruitment', 4),
('cat-psu', 'PSUs & Technical', 'psu', 'Graduate recruitment across Maharatna, Navratna & Central PSUs', 5)
ON CONFLICT (id) DO NOTHING;

-- 2. Official Sources
INSERT INTO sources (
    id, organization, source_name, official_base_url, source_type, fetch_method,
    polling_interval_minutes, is_enabled, parser_version, access_policy_notes
) VALUES
(
    'src-upsc',
    'Union Public Service Commission (UPSC)',
    'UPSC Active Examinations Notice Board',
    'https://upsc.gov.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    60,
    TRUE,
    '1.0.0-upsc',
    'Permitted public active examination notice board. Max 1 request per 2 seconds.'
),
(
    'src-ssc',
    'Staff Selection Commission (SSC)',
    'SSC Official Notices Feed',
    'https://ssc.gov.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    60,
    TRUE,
    '1.0.0-ssc',
    'Permitted public examination circulars and application updates.'
),
(
    'src-ibps',
    'Institute of Banking Personnel Selection (IBPS)',
    'IBPS Common Recruitment Process (CRP)',
    'https://www.ibps.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-ibps',
    'CRP notices for PO/MT, Clerk, Specialist Officers, and RRBs.'
),
(
    'src-rbi',
    'Reserve Bank of India (RBI)',
    'RBI Opportunities & Vacancies',
    'https://opportunities.rbi.org.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-rbi',
    'RBI Grade B and Assistant recruitment announcements.'
),
(
    'src-sbi',
    'State Bank of India (SBI)',
    'SBI Careers Current Openings',
    'https://sbi.co.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-sbi',
    'SBI Probationary Officers, Junior Associates, and Specialist Cadre.'
)
ON CONFLICT (id) DO NOTHING;

-- 3. Core Examinations
INSERT INTO exams (id, category_id, organization, exam_name, exam_code, official_portal_url, description, is_active) VALUES
-- UPSC
('exam-upsc-cse', 'cat-upsc', 'UPSC', 'Civil Services Examination', 'CSE', 'https://upsc.gov.in/examinations/active-examinations', 'Recruitment for IAS, IPS, IFS, IRS and other Group A/B central services.', TRUE),
('exam-upsc-ese', 'cat-upsc', 'UPSC', 'Engineering Services Examination', 'ESE', 'https://upsc.gov.in/examinations/active-examinations', 'Recruitment for technical engineering cadres in Indian Railways, Telecom, Power.', TRUE),
('exam-upsc-cds', 'cat-upsc', 'UPSC', 'Combined Defence Services Examination', 'CDS', 'https://upsc.gov.in/examinations/active-examinations', 'Recruitment for Indian Military Academy, Naval Academy, Air Force Academy and OTA.', TRUE),
('exam-upsc-nda', 'cat-upsc', 'UPSC', 'National Defence Academy & NA Exam', 'NDA', 'https://upsc.gov.in/examinations/active-examinations', 'Entry into Army, Navy and Air Force wings of the NDA.', TRUE),

-- SSC
('exam-ssc-cgl', 'cat-ssc', 'SSC', 'Combined Graduate Level Examination', 'SSC CGL', 'https://ssc.gov.in', 'Recruitment to Group B and C posts across various ministries, departments and offices of GOI.', TRUE),
('exam-ssc-chsl', 'cat-ssc', 'SSC', 'Combined Higher Secondary Level Examination', 'SSC CHSL', 'https://ssc.gov.in', 'Recruitment for Lower Divisional Clerk, Junior Secretariat Assistant, and Postal Assistants.', TRUE),
('exam-ssc-cpo', 'cat-ssc', 'SSC', 'Sub-Inspector in Delhi Police & CAPFs', 'SSC CPO', 'https://ssc.gov.in', 'Recruitment of Sub-Inspectors in Delhi Police, BSF, CISF, CRPF, ITBP, SSB.', TRUE),
('exam-ssc-je', 'cat-ssc', 'SSC', 'Junior Engineer (Civil, Mech, Electrical)', 'SSC JE', 'https://ssc.gov.in', 'Recruitment of Junior Engineers in CPWD, MES, Central Water Commission.', TRUE),

-- Banking & IBPS
('exam-ibps-po', 'cat-banking', 'IBPS', 'IBPS Probationary Officer (CRP PO/MT)', 'IBPS PO', 'https://www.ibps.in', 'Common recruitment process for Probationary Officers in 11 participating public sector banks.', TRUE),
('exam-ibps-clerk', 'cat-banking', 'IBPS', 'IBPS Clerk (CRP Clerical)', 'IBPS Clerk', 'https://www.ibps.in', 'Common recruitment process for Clerical cadre in participating public sector banks.', TRUE),
('exam-ibps-so', 'cat-banking', 'IBPS', 'IBPS Specialist Officer (CRP SPL)', 'IBPS SO', 'https://www.ibps.in', 'Specialist officers (IT, Agricultural, Law, HR, Marketing) in public sector banks.', TRUE),
('exam-sbi-po', 'cat-banking', 'SBI', 'SBI Probationary Officer', 'SBI PO', 'https://sbi.co.in/web/careers', 'Annual recruitment for Probationary Officers across State Bank of India branches nationwide.', TRUE),
('exam-sbi-clerk', 'cat-banking', 'SBI', 'SBI Junior Associates (Customer Support & Sales)', 'SBI Clerk', 'https://sbi.co.in/web/careers', 'Recruitment in clerical cadre across State Bank of India circles.', TRUE),

-- Regulatory Bodies
('exam-rbi-grade-b', 'cat-regulatory', 'RBI', 'RBI Grade B Officers (General, DEPR, DSIM)', 'RBI Gr B', 'https://opportunities.rbi.org.in', 'Premier recruitment for Manager-level Grade B officers at the Reserve Bank of India.', TRUE),
('exam-sebi-grade-a', 'cat-regulatory', 'SEBI', 'SEBI Grade A Assistant Manager', 'SEBI Gr A', 'https://www.sebi.gov.in', 'Assistant Managers across General, Legal, Information Technology, Engineering streams.', TRUE),
('exam-nabard-grade-a', 'cat-regulatory', 'NABARD', 'NABARD Grade A Assistant Manager', 'NABARD Gr A', 'https://www.nabard.org', 'Rural development banking cadre in the National Bank for Agriculture and Rural Development.', TRUE),

-- PSUs & Technical
('exam-gate-psu', 'cat-psu', 'PSU Consortium', 'PSU Recruitment through GATE', 'GATE PSU', 'https://gate2025.iitr.ac.in', 'Executive Trainee recruitment across IOCL, ONGC, NTPC, BHEL, PowerGrid based on GATE scores.', TRUE),
('exam-ongc-gt', 'cat-psu', 'ONGC', 'ONGC Graduate Trainee Engineering & Geo-sciences', 'ONGC GT', 'https://ongcindia.com', 'Recruitment of Graduate Trainees in E-1 level posts at Oil and Natural Gas Corporation.', TRUE)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- PART 3: REAL VERIFIED OFFICIAL NOTICES (SSC & IBPS Feeds)
-- ============================================================================

-- 3.1 Live Notices
INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('45e6af1a-7985-4c45-9ee8-e497b87b55c8', 'exam-ssc-cgl', 'src-ssc', 'ADMIT_CARD', 'Constable (GD) in Central Armed Police Forces (CAPFs) and SSF, and Rifleman (GD) in Assam Rifles Examination, 2026– Shortlisting of candidates to appear in Physical Efficiency Test (PET)/Physical Standard Test (PST)', 'Constable (GD) in Central Armed Police Forces (CAPFs) and SSF, and Rifleman (GD) in Assam Rifles Examination, 2026– Shortlisting of candidates to appear in Physical Efficiency Test (PET)/Physical Standard Test (PST)', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/writeup_12102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-12', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('c3374fc1-369b-4c02-ad0a-d77b9fdbb9a4', 'exam-ssc-cgl', 'src-ssc', 'ANSWER_KEY', 'Phase-XIV/2026/Selection Posts Examination - Uploading of Candidate’s Response Sheet(s) along with Tentative Answer Keys and inviting challenges thereon', 'Phase-XIV/2026/Selection Posts Examination - Uploading of Candidate’s Response Sheet(s) along with Tentative Answer Keys and inviting challenges thereon', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/writeup_rhq.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-09', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('4103b18c-9d13-4875-ba3a-ed34162343a5', 'exam-ssc-je', 'src-ssc', 'RECRUITMENT', 'Tentative Vacancies of Junior Engineer Examination, 2026.', 'Tentative Vacancies of Junior Engineer Examination, 2026.', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/JE_Tentative_Vacancy_08102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-08', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('d4b96058-c5a6-4c9f-b828-9665ec0dad45', 'exam-ssc-cgl', 'src-ssc', 'RESULT', 'Stenographer Grade ''C'' and ''D'' Examination, 2025: Uploading Marks of CBE and Skill Test', 'Stenographer Grade ''C'' and ''D'' Examination, 2025: Uploading Marks of CBE and Skill Test', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/Final_Marks_Writeup_07102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-07', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('b78816fb-ebf3-43e9-8160-121bf60ea343', 'exam-ssc-chsl', 'src-ssc', 'RESULT', 'Combined Higher Secondary (10+2) Level Examination, 2025: Declaration of Final Result', 'Combined Higher Secondary (10+2) Level Examination, 2025: Declaration of Final Result', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/writeup_final_chsl_2025_07102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-07', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('8a907f66-ffe9-4b30-a0e7-e9b5265139de', 'exam-ssc-cgl', 'src-ssc', 'RESULT', 'Multi Tasking (Non-Technical) Staff, and Havaldar (CBIC and CBN) Examination, 2025: Declaration of additional result of Computer Based Examination to shortlist candidates for PET/ PST for the post of Havaldar.', 'Multi Tasking (Non-Technical) Staff, and Havaldar (CBIC and CBN) Examination, 2025: Declaration of additional result of Computer Based Examination to shortlist candidates for PET/ PST for the post of Havaldar.', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/writeup_07102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-07', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('7b9f4766-14b6-4898-95b0-25b4e7461f43', 'exam-ssc-cgl', 'src-ssc', 'RECRUITMENT', 'NOTIFICATION FOR RECRUITMENT OF ENROLLED PERSONS IN THE INDIAN COAST GUARD THROUGH COAST GUARD ENROLLED PERSONS TEST (CGEPT) –BATCHES 01/2027 and 02/2027', 'NOTIFICATION FOR RECRUITMENT OF ENROLLED PERSONS IN THE INDIAN COAST GUARD THROUGH COAST GUARD ENROLLED PERSONS TEST (CGEPT) –BATCHES 01/2027 and 02/2027', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/advertisement_06102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-06', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('a107f533-125c-4986-b0a6-52b99b047551', 'exam-ssc-cgl', 'src-ssc', 'RECRUITMENT', 'Important Notice - Schedule of Examinations', 'Important Notice - Schedule of Examinations', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/Notice_01102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-01', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('78a45b58-7832-4784-b7fd-a206422bd622', 'exam-ssc-cgl', 'src-ssc', 'CORRIGENDUM', 'Cancellation Notice for the post of ''Junior Technical Officer'', Post Code KK13526, advertised under Selection Posts Examination, Phase-XIV/2026', 'Cancellation Notice for the post of ''Junior Technical Officer'', Post Code KK13526, advertised under Selection Posts Examination, Phase-XIV/2026', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/Notice_KK13526_01102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-01', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('651c3cf8-dd3d-4b56-8ef9-28dfa68c3401', 'exam-ssc-cgl', 'src-ssc', 'CORRIGENDUM', 'Cancellation Notice for the post of ''Agriculture Assistant'', Post Code KK11426, advertised under Selection Posts Examination, Phase-XIV/2026', 'Cancellation Notice for the post of ''Agriculture Assistant'', Post Code KK11426, advertised under Selection Posts Examination, Phase-XIV/2026', 'https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/Notice_KK11426_01102026.pdf', 'https://ssc.gov.in', 'PUBLISHED', '2026-10-01', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('ce04d7e6-6f35-42d6-8d2c-8d8da9a448e4', 'exam-ibps-po', 'src-ibps', 'RESULT', 'Notification Regarding CRP-RRBs–XIV (Provisional Allotment – Reserve List)- Office Assistants (Multipurpose)', 'Notification Regarding CRP-RRBs–XIV (Provisional Allotment – Reserve List)- Office Assistants (Multipurpose)', 'https://www.ibps.in/index.php/notification-regarding-crp-rrbsxiv-provisional-allotment-reserve-list-office-assistants-multipurpose-30sep26/', NULL, 'PUBLISHED', '2026-09-30', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('2f782dac-ea06-4216-8331-eb7ab70df85d', 'exam-ibps-po', 'src-ibps', 'RESULT', 'provisional-allotment-under-reserve-list-for-crp-rrbsxiv-office-assistants-multipurpose-30sep26', '30 Sep 2026 Provisional Allotment under Reserve List for CRP-RRBs–XIV-Office Assistants (Multipurpose) 30 Sep 2026 Notification Regarding CRP-RRBs–XIV (Provisional Allotment - Reserve List)- Office Assistants (Multipurpose)', 'https://www.ibps.in/index.php/provisional-allotment-under-reserve-list-for-crp-rrbsxiv-office-assistants-multipurpose-30sep26/', NULL, 'PUBLISHED', '2026-09-30', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('f4e3c917-0a5c-446a-9081-fa790caae82e', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Notification under CRP-PO/MT-XVI', 'Notification under CRP-PO/MT-XVI', 'https://www.ibps.in/index.php/notification-under-crp-po-mt-xvi-29sep26/', NULL, 'PUBLISHED', '2026-09-29', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('a1f517ff-6e3c-41c0-a1fd-21b6e1be0c27', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Notification for Common Recruitment Process for CRP-RRB-XV', '01 Sep 2026 Notification for Common Recruitment Process for CRP-RRB-XV', 'https://www.ibps.in/index.php/notification-for-common-recruitment-process-for-crp-rrb-xv/', NULL, 'PUBLISHED', '2026-08-31', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('94100b4f-3dc7-418e-b8f9-0650fa990526', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Window Notification for CRP-RRB-XV', '31 Aug 2026 Window Notification for CRP-RRB-XV', 'https://www.ibps.in/index.php/window-notification-for-crp-rrb-xv/', NULL, 'PUBLISHED', '2026-08-31', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('fc440a06-bbac-4b9e-969c-944214d17ebb', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Notification for Common Recruitment Process for CRP-CSA-XVI', '01 Aug 2026 Notification for Common Recruitment Process for CRP-CSA-XVI', 'https://www.ibps.in/index.php/notification-for-common-recruitment-process-for-crp-csa-xvi/', NULL, 'PUBLISHED', '2026-07-31', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('e23e4d6b-ea33-4146-b60a-9d0620400293', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Window Notification for CRP-CSA-XVI', '31 Jul 2026 Window Notification for CRP-CSA-XVI', 'https://www.ibps.in/index.php/window-notification-for-crp-csa-xvi/', NULL, 'PUBLISHED', '2026-07-31', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('704b0e24-7c30-4650-9874-9f2f6e3ddf14', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'Window Notification for CRP-PO/MTs -XVI', '30 Jun 2026 Window Notification for CRP- PO/MTs -XVI 01 Jul 2026 Notification for Common Recruitment Process for CRP- PO/MTs-XVI', 'https://www.ibps.in/index.php/window-notification-for-crp-po-mts-xvi/', NULL, 'PUBLISHED', '2026-06-30', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('5607f3df-37cc-4634-9bc0-ac558bd404a9', 'exam-ibps-so', 'src-ibps', 'RECRUITMENT', 'Window Notification for CRP- SPL-XVI', '30 Jun 2026 Window Notification for CRP- SPL-XVI 01 Jul 2026 Notification for Common Recruitment Process for CRP- SPL-XVI', 'https://www.ibps.in/index.php/window-notification-for-crp-spl-xvi/', NULL, 'PUBLISHED', '2026-06-30', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notices (id, exam_id, source_id, notice_type, title, summary, official_notice_url, official_apply_url, status, published_date, verification_status)
VALUES ('270a6b40-ebe4-400f-bdc9-068ae38af09d', 'exam-ibps-po', 'src-ibps', 'RECRUITMENT', 'https://www.ibps.in/wp-content/uploads/IBPS-Notification-Fraud-Websites-27April26.pdf', '27 Apr 2026 Notification on Fraudulent Websites Resembling IBPS', 'https://www.ibps.in/index.php/notification-regarding-revised-advisory-and-standard-operating-procedure-sop-to-handle-the-cases-of-pwbd-2/', NULL, 'PUBLISHED', '2026-04-27', 'VERIFIED')
ON CONFLICT (id) DO NOTHING;

-- 3.2 Important Notice Dates
INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('a422a7bd-e792-41bc-b1e5-8099048dfec1', '45e6af1a-7985-4c45-9ee8-e497b87b55c8', 'APPLICATION_START', '2026-10-12', false, false, '2026-10-12', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('83f7d5ec-a6bb-4faa-b50b-969e13ac6361', 'c3374fc1-369b-4c02-ad0a-d77b9fdbb9a4', 'APPLICATION_START', '2026-10-09', false, false, '2026-10-09', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('8a765d75-9e2c-4ddb-822e-d00c19e0a3e5', '4103b18c-9d13-4875-ba3a-ed34162343a5', 'APPLICATION_START', '2026-10-08', false, false, '2026-10-08', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('09790020-8999-4f28-8089-2397e367714f', 'd4b96058-c5a6-4c9f-b828-9665ec0dad45', 'APPLICATION_START', '2026-10-07', false, false, '2026-10-07', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('f9006279-289e-43a9-8f70-a1eb8bb2f614', 'b78816fb-ebf3-43e9-8160-121bf60ea343', 'APPLICATION_START', '2026-10-07', false, false, '2026-10-07', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('252d5186-75e0-49ca-beaf-e25b36a4fc5b', '8a907f66-ffe9-4b30-a0e7-e9b5265139de', 'APPLICATION_START', '2026-10-07', false, false, '2026-10-07', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('05e845e8-ecd6-4237-a199-d8a76fd9b1a6', '7b9f4766-14b6-4898-95b0-25b4e7461f43', 'APPLICATION_START', '2026-10-06', false, false, '2026-10-06', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('00b225f3-5178-444d-a209-50905e18a218', 'a107f533-125c-4986-b0a6-52b99b047551', 'APPLICATION_START', '2026-10-01', false, false, '2026-10-01', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('e43c0349-a156-4ec6-9f46-9ce1b711989e', '78a45b58-7832-4784-b7fd-a206422bd622', 'APPLICATION_START', '2026-10-01', false, false, '2026-10-01', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('72708e6d-11e6-4841-805c-5daffdc17fd2', '651c3cf8-dd3d-4b56-8ef9-28dfa68c3401', 'APPLICATION_START', '2026-10-01', false, false, '2026-10-01', 'SSC Notice Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('db85e53a-3821-4a58-9433-4da7a4f6b630', 'ce04d7e6-6f35-42d6-8d2c-8d8da9a448e4', 'APPLICATION_START', '2026-09-30', false, false, '2026-09-30', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('06c8b594-bec6-46c2-8413-7de0c9421732', '2f782dac-ea06-4216-8331-eb7ab70df85d', 'APPLICATION_START', '2026-09-30', false, false, '2026-09-30', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('98b66e8a-6a14-4045-b40b-7f04b44760d6', 'f4e3c917-0a5c-446a-9081-fa790caae82e', 'APPLICATION_START', '2026-09-29', false, false, '2026-09-29', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('341ec950-6e61-4c1b-a8b7-c3e14f25dec2', 'a1f517ff-6e3c-41c0-a1fd-21b6e1be0c27', 'APPLICATION_START', '2026-08-31', false, false, '2026-08-31', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('6a5bf03e-a68e-47dd-8bc5-c33c5fc60197', '94100b4f-3dc7-418e-b8f9-0650fa990526', 'APPLICATION_START', '2026-08-31', false, false, '2026-08-31', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('d1484a16-9fdd-40b7-a8db-3493ffd01d59', 'fc440a06-bbac-4b9e-969c-944214d17ebb', 'APPLICATION_START', '2026-07-31', false, false, '2026-07-31', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('e4a2def3-e421-4746-95b2-c6f92d5fb2b3', 'e23e4d6b-ea33-4146-b60a-9d0620400293', 'APPLICATION_START', '2026-07-31', false, false, '2026-07-31', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('0863e854-c422-4a1b-a00f-a050e89aec9f', '704b0e24-7c30-4650-9874-9f2f6e3ddf14', 'APPLICATION_START', '2026-06-30', false, false, '2026-06-30', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('891462a0-4fab-4e4d-b5be-67dcea117573', '5607f3df-37cc-4634-9bc0-ac558bd404a9', 'APPLICATION_START', '2026-06-30', false, false, '2026-06-30', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.notice_dates (id, notice_id, date_type, event_date, is_tentative, is_revised, original_date_str, source_text_snippet, confidence_score)
VALUES ('503a20d6-d691-4b08-bf5a-7c53fb99b8c3', '270a6b40-ebe4-400f-bdc9-068ae38af09d', 'APPLICATION_START', '2026-04-27', false, false, '2026-04-27', 'Official Gazette Publication Date', 1.0)
ON CONFLICT (id) DO NOTHING;


-- ============================================================================
-- PART 4: ROW LEVEL SECURITY (RLS) POLICIES
-- ============================================================================

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

DROP POLICY IF EXISTS "Public read for exam categories" ON exam_categories;
CREATE POLICY "Public read for exam categories"
    ON exam_categories FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Public read for active exams" ON exams;
CREATE POLICY "Public read for active exams"
    ON exams FOR SELECT
    USING (is_active = true);

DROP POLICY IF EXISTS "Public read for active sources" ON sources;
CREATE POLICY "Public read for active sources"
    ON sources FOR SELECT
    USING (is_enabled = true);

DROP POLICY IF EXISTS "Public read for published notices" ON notices;
CREATE POLICY "Public read for published notices"
    ON notices FOR SELECT
    USING (status = 'PUBLISHED');

DROP POLICY IF EXISTS "Public read for notice versions of published notices" ON notice_versions;
CREATE POLICY "Public read for notice versions of published notices"
    ON notice_versions FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM notices n WHERE n.id = notice_versions.notice_id AND n.status = 'PUBLISHED'
    ));

DROP POLICY IF EXISTS "Public read for notice dates of published notices" ON notice_dates;
CREATE POLICY "Public read for notice dates of published notices"
    ON notice_dates FOR SELECT
    USING (EXISTS (
        SELECT 1 FROM notices n WHERE n.id = notice_dates.notice_id AND n.status = 'PUBLISHED'
    ));

DROP POLICY IF EXISTS "Public read for eligibility rules" ON eligibility_rules;
CREATE POLICY "Public read for eligibility rules"
    ON eligibility_rules FOR SELECT
    USING (true);

DROP POLICY IF EXISTS "Public read for source health snapshots" ON source_health_snapshots;
CREATE POLICY "Public read for source health snapshots"
    ON source_health_snapshots FOR SELECT
    USING (true);

-- ============================================================================
-- 3. USER DATA POLICIES (Isolated by Supabase auth.uid())
-- ============================================================================

-- Profiles
DROP POLICY IF EXISTS "Users can view own profile" ON profiles;
CREATE POLICY "Users can view own profile"
    ON profiles FOR SELECT
    USING ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can insert own profile" ON profiles;
CREATE POLICY "Users can insert own profile"
    ON profiles FOR INSERT
    WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can update own profile" ON profiles;
CREATE POLICY "Users can update own profile"
    ON profiles FOR UPDATE
    USING ((select auth.uid()) = id)
    WITH CHECK ((select auth.uid()) = id);

DROP POLICY IF EXISTS "Users can delete own profile" ON profiles;
CREATE POLICY "Users can delete own profile"
    ON profiles FOR DELETE
    USING ((select auth.uid()) = id);

-- User Exam Preferences
DROP POLICY IF EXISTS "Users can view own exam preferences" ON user_exam_preferences;
CREATE POLICY "Users can view own exam preferences"
    ON user_exam_preferences FOR SELECT
    USING ((select auth.uid()) = user_id);

DROP POLICY IF EXISTS "Users can manage own exam preferences" ON user_exam_preferences;
CREATE POLICY "Users can manage own exam preferences"
    ON user_exam_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- Notification Preferences
DROP POLICY IF EXISTS "Users can manage own notification preferences" ON notification_preferences;
CREATE POLICY "Users can manage own notification preferences"
    ON notification_preferences FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- Device Tokens
DROP POLICY IF EXISTS "Users can manage own device tokens" ON device_tokens;
CREATE POLICY "Users can manage own device tokens"
    ON device_tokens FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- Checklists
DROP POLICY IF EXISTS "Users can manage own checklists" ON application_checklists;
CREATE POLICY "Users can manage own checklists"
    ON application_checklists FOR ALL
    USING ((select auth.uid()) = user_id)
    WITH CHECK ((select auth.uid()) = user_id);

-- Checklist Items
DROP POLICY IF EXISTS "Users can manage own checklist items" ON application_checklist_items;
CREATE POLICY "Users can manage own checklist items"
    ON application_checklist_items FOR ALL
    USING (EXISTS (
        SELECT 1 FROM application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = (select auth.uid())
    ))
    WITH CHECK (EXISTS (
        SELECT 1 FROM application_checklists c
        WHERE c.id = application_checklist_items.checklist_id AND c.user_id = (select auth.uid())
    ));

-- ============================================================================
-- 4. SERVICE ROLE EXCLUSIVE TABLES (No public/user direct modifications)
-- ============================================================================
-- Tables like notification_jobs, notification_deliveries, audit_events,
-- parser_failures, and source_fetch_runs have RLS enabled with NO user policies.
-- By PostgreSQL RLS design, non-service_role callers have zero access.
-- Only the backend / collector runner using the SERVICE_ROLE_KEY or direct DB
-- connection can insert or read these operational tables.


-- ============================================================================
-- PART 5: SUPABASE AUTH TRIGGER (Auto-create Profile upon Signup)
-- ============================================================================

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

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();

-- Auto-confirm email trigger for immediate friction-free authentication
CREATE OR REPLACE FUNCTION public.auto_confirm_new_user()
RETURNS TRIGGER AS $$
BEGIN
    NEW.email_confirmed_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER SET search_path = public, auth;

DROP TRIGGER IF EXISTS on_auth_user_auto_confirm ON auth.users;
CREATE TRIGGER on_auth_user_auto_confirm
    BEFORE INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION public.auto_confirm_new_user();
