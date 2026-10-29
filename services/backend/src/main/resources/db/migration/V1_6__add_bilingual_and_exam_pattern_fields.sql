-- ============================================================================
-- Migration V1_6: Add Bilingual Support, Exam Pattern & State PSC Catalogue
-- ============================================================================

-- 1. Add Bilingual and Exam Pattern Columns to notices
ALTER TABLE notices
    ADD COLUMN IF NOT EXISTS title_hi TEXT,
    ADD COLUMN IF NOT EXISTS summary_hi TEXT,
    ADD COLUMN IF NOT EXISTS exam_stages TEXT,
    ADD COLUMN IF NOT EXISTS negative_marking VARCHAR(128),
    ADD COLUMN IF NOT EXISTS exam_mode VARCHAR(128),
    ADD COLUMN IF NOT EXISTS syllabus_url VARCHAR(1024);

-- 2. State Public Service Commission Category
INSERT INTO exam_categories (id, name, slug, description, display_order) VALUES
(
    'cat-state-psc',
    'State PSC Examinations',
    'state-psc',
    'State Public Service Commissions recruitment across administrative, police & revenue cadres',
    6
)
ON CONFLICT (id) DO NOTHING;

-- 3. State Public Service Commission Sources
INSERT INTO sources (
    id, organization, source_name, official_base_url, source_type, fetch_method,
    polling_interval_minutes, is_enabled, parser_version, access_policy_notes
) VALUES
(
    'src-uppsc',
    'Uttar Pradesh Public Service Commission (UPPSC)',
    'UPPSC Official Notice Board',
    'https://uppsc.up.nic.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-uppsc',
    'Public notification portal for Combined State / Upper Subordinate Services (PCS).'
),
(
    'src-bpsc',
    'Bihar Public Service Commission (BPSC)',
    'BPSC Official Notice Circulars',
    'https://www.bpsc.bih.nic.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-bpsc',
    'Public notice circulars for Combined Competitive Examination (CCE).'
)
ON CONFLICT (id) DO NOTHING;

-- 4. State Examinations
INSERT INTO exams (id, category_id, organization, exam_name, exam_code, official_portal_url, description, is_active) VALUES
(
    'exam-uppsc-pcs',
    'cat-state-psc',
    'UPPSC',
    'UP Combined State / Upper Subordinate Exam (PCS)',
    'UPPSC PCS',
    'https://uppsc.up.nic.in',
    'Premier civil administrative recruitment for SDM, DSP, and Treasury Officers in Uttar Pradesh.',
    TRUE
),
(
    'exam-bpsc-cce',
    'cat-state-psc',
    'BPSC',
    'BPSC Combined Competitive Examination',
    'BPSC CCE',
    'https://www.bpsc.bih.nic.in',
    'State administrative and police service recruitment for Bihar Administrative Service & Bihar Police Service.',
    TRUE
)
ON CONFLICT (id) DO NOTHING;
