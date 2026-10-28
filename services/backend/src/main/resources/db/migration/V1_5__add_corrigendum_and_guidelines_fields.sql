-- ============================================================================
-- Migration V1_5: Add Corrigendum and Upload Guidelines Fields
-- Supports Corrigendum tracking, Photo/Signature specifications, and Fee payment windows
-- ============================================================================

ALTER TABLE notices
    ADD COLUMN IF NOT EXISTS has_corrigendum BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS corrigendum_summary TEXT,
    ADD COLUMN IF NOT EXISTS photo_specifications TEXT,
    ADD COLUMN IF NOT EXISTS signature_specifications TEXT,
    ADD COLUMN IF NOT EXISTS fee_payment_deadline DATE;
