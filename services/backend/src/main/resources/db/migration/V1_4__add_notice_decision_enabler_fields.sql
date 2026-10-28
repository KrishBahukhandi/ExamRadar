-- ============================================================================
-- Migration V1_4: Add Notice Decision Enabler Fields & Performance Indexes
-- Empowers candidates with Vacancy breakdown, Salary, Fees, and Age criteria
-- ============================================================================

ALTER TABLE notices
    ADD COLUMN IF NOT EXISTS total_vacancies INT,
    ADD COLUMN IF NOT EXISTS vacancy_breakdown_json TEXT,
    ADD COLUMN IF NOT EXISTS pay_scale_summary VARCHAR(255),
    ADD COLUMN IF NOT EXISTS application_fee_general INT,
    ADD COLUMN IF NOT EXISTS application_fee_reserved INT,
    ADD COLUMN IF NOT EXISTS fee_exemption_details TEXT,
    ADD COLUMN IF NOT EXISTS age_min INT,
    ADD COLUMN IF NOT EXISTS age_max INT,
    ADD COLUMN IF NOT EXISTS age_cutoff_date DATE,
    ADD COLUMN IF NOT EXISTS age_relaxation_summary TEXT,
    ADD COLUMN IF NOT EXISTS educational_qualification TEXT;

-- Index to optimize follower notice queries in NotificationDispatcherService
CREATE INDEX IF NOT EXISTS idx_user_exam_prefs_exam_notify ON user_exam_preferences(exam_id, notify_new_notices);
