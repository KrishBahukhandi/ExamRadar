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
