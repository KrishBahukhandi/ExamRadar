-- ============================================================================
-- ExamRadar India — Flyway V1.3 Migration: Distributed Scheduler Locking (ShedLock)
-- Coordinates background jobs (official source scraping and FCM notification queue)
-- across multiple horizontally-scaled backend containers.
-- ============================================================================

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- Enable RLS to prevent unauthorized access via Supabase PostgREST
ALTER TABLE shedlock ENABLE ROW LEVEL SECURITY;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'service_role') THEN
        EXECUTE 'CREATE POLICY shedlock_service_role_all ON shedlock FOR ALL TO service_role USING (true) WITH CHECK (true)';
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'postgres') THEN
        EXECUTE 'CREATE POLICY shedlock_postgres_all ON shedlock FOR ALL TO postgres USING (true) WITH CHECK (true)';
    END IF;
END $$;
