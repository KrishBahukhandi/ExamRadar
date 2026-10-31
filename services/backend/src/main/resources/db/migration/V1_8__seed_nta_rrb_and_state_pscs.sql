-- ============================================================================
-- ExamRadar India — Flyway V1.7 Migration: Seed NTA, RRB, and State PSCs
-- Adds national entrance and railway categories, sources, and exam registries.
-- ============================================================================

-- 1. New Exam Categories
INSERT INTO exam_categories (id, name, slug, description, display_order)
VALUES
(
    'cat-nta',
    'National Testing Agency (NTA)',
    'nta',
    'National entrance tests for Engineering, Medical, University Admissions & Eligibility (JEE, NEET, CUET, UGC NET)',
    6
),
(
    'cat-railways',
    'Indian Railways (RRB / RRC)',
    'railways',
    'Railway Recruitment Boards centralized employment notices (NTPC, Group D, ALP, JE)',
    7
),
(
    'cat-state-psc',
    'State Public Service Commissions',
    'state-psc',
    'State Civil Services, Judicial Services & State Departmental Recruitments (UPPSC, BPSC, MPSC)',
    8
)
ON CONFLICT (id) DO NOTHING;

-- 2. New Official Sources
INSERT INTO sources (
    id, organization, source_name, official_base_url, source_type, fetch_method,
    polling_interval_minutes, is_enabled, parser_version, access_policy_notes
)
VALUES
(
    'src-nta',
    'National Testing Agency (NTA)',
    'NTA Public Notices & Circulars Archive',
    'https://nta.ac.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    60,
    TRUE,
    '1.0.0-nta',
    'Public entrance exam notices for JEE Main, NEET UG, and CUET. Max 1 request per 2 seconds.'
),
(
    'src-rrb',
    'Railway Recruitment Control Board (RRB)',
    'Indian Railways Centralized Employment Notices (CEN)',
    'https://indianrailways.gov.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    120,
    TRUE,
    '1.0.0-rrb',
    'Centralized employment notices for NTPC, ALP, Group D, and Technician recruitment.'
),
(
    'src-uppsc',
    'Uttar Pradesh Public Service Commission (UPPSC)',
    'UPPSC Official Notices Feed',
    'https://uppsc.up.nic.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    60,
    TRUE,
    '1.0.0-uppsc',
    'Official notices for Combined State / Upper Subordinate Services (PCS).'
),
(
    'src-bpsc',
    'Bihar Public Service Commission (BPSC)',
    'BPSC Official Circulars Board',
    'https://www.bpsc.bih.nic.in',
    'HTML_SCRAPE',
    'HTTP_GET',
    60,
    TRUE,
    '1.0.0-bpsc',
    'Official circulars for Combined Competitive Examination (CCE) and Teacher Recruitment Exam.'
)
ON CONFLICT (id) DO NOTHING;

-- 3. New Exam Registries
INSERT INTO exams (
    id, category_id, authority, exam_name, abbreviation,
    official_portal_url, description, is_active
)
VALUES
(
    'exam-nta-jee-main',
    'cat-nta',
    'NTA',
    'Joint Entrance Examination (Main)',
    'JEE Main',
    'https://jeemain.nta.nic.in',
    'National level entrance examination for admission into undergraduate engineering programs at NITs, IIITs, and CFTIs.',
    TRUE
),
(
    'exam-nta-neet-ug',
    'cat-nta',
    'NTA',
    'National Eligibility cum Entrance Test (UG)',
    'NEET UG',
    'https://neet.nta.nic.in',
    'All India medical entrance test for MBBS, BDS, BAMS, BHMS and allied undergraduate medical courses.',
    TRUE
),
(
    'exam-nta-cuet-ug',
    'cat-nta',
    'NTA',
    'Common University Entrance Test (UG)',
    'CUET UG',
    'https://cuetug.nta.online.in',
    'Standardized entrance test for undergraduate admissions across Central, State, and participating universities in India.',
    TRUE
),
(
    'exam-rrb-ntpc',
    'cat-railways',
    'RRB',
    'Non-Technical Popular Categories (NTPC)',
    'RRB NTPC',
    'https://indianrailways.gov.in',
    'Centralized recruitment for Station Master, Goods Guard, Senior Clerk, Junior Accounts Assistant across 21 RRBs.',
    TRUE
),
(
    'exam-rrb-alp',
    'cat-railways',
    'RRB',
    'Assistant Loco Pilot & Technicians',
    'RRB ALP',
    'https://indianrailways.gov.in',
    'Recruitment of Assistant Loco Pilots and Technical trade personnel across Indian Railways operational divisions.',
    TRUE
),
(
    'exam-rrb-group-d',
    'cat-railways',
    'RRC',
    'Railway Group D (Level-1 Track Maintainer, Pointsman)',
    'RRC Group D',
    'https://indianrailways.gov.in',
    'Level 1 posts recruitment across 16 Railway Zones in Indian Railways.',
    TRUE
),
(
    'exam-uppsc-pcs',
    'cat-state-psc',
    'UPPSC',
    'Combined State / Upper Subordinate Services (PCS)',
    'UPPSC PCS',
    'https://uppsc.up.nic.in',
    'Premier administrative recruitment examination for Sub-Divisional Magistrate (SDM), DSP, and ARTO in Uttar Pradesh.',
    TRUE
),
(
    'exam-bpsc-cce',
    'cat-state-psc',
    'BPSC',
    'Combined Competitive Examination (CCE)',
    'BPSC CCE',
    'https://bpsc.bih.nic.in',
    'Premier civil services examination for Bihar Administrative Service (BAS), Bihar Police Service (BPS) and Finance Service.',
    TRUE
)
ON CONFLICT (id) DO NOTHING;

