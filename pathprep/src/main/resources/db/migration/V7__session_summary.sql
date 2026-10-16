-- V7: Session summary, completion, and active session constraint (Phase 5)

-- 1. Alter sessions table to add counters, score, and timestamps
ALTER TABLE sessions
    ADD COLUMN answered_count INT NOT NULL DEFAULT 0,
    ADD COLUMN correct_count INT NOT NULL DEFAULT 0,
    ADD COLUMN score_percent DECIMAL(5,1) NULL,
    ADD COLUMN completion_reason VARCHAR(20) NULL,
    ADD COLUMN current_question_started_at DATETIME(6) NULL;

-- 2. Alter attempts table to add time_spent_sec
ALTER TABLE attempts
    ADD COLUMN time_spent_sec INT NULL DEFAULT 0;

-- 3. Completion reason constraint
ALTER TABLE sessions
    ADD CONSTRAINT chk_sessions_completion_reason
    CHECK (completion_reason IS NULL OR completion_reason IN ('FINISHED', 'ENDED_EARLY'));

-- 4. Update status check constraint to include 'ABANDONED'
ALTER TABLE sessions DROP CHECK chk_sessions_status;
ALTER TABLE sessions
    ADD CONSTRAINT chk_sessions_status
    CHECK (status IN ('IN_PROGRESS', 'EVALUATING', 'COMPLETED', 'FAILED', 'ABANDONED'));

-- 5. Indexes for fast session history and status queries
CREATE INDEX idx_sessions_user_started ON sessions (user_id, started_at);
CREATE INDEX idx_sessions_user_status_started ON sessions (user_id, status, started_at);

-- 6. Backfill existing rows from attempts
UPDATE sessions s
LEFT JOIN (
    SELECT session_id,
           COUNT(*) AS ans_cnt,
           SUM(CASE WHEN correct = 1 THEN 1 ELSE 0 END) AS corr_cnt
    FROM attempts
    GROUP BY session_id
) a ON s.id = a.session_id
SET s.answered_count = COALESCE(a.ans_cnt, 0),
    s.correct_count = COALESCE(a.corr_cnt, 0);

UPDATE sessions
SET current_question_started_at = started_at
WHERE status = 'IN_PROGRESS';

-- 7. One-active-session guard: mark all but the newest IN_PROGRESS session per user as ABANDONED
UPDATE sessions s
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY started_at DESC, id DESC) AS rn
    FROM sessions
    WHERE status = 'IN_PROGRESS'
) ranked ON s.id = ranked.id
SET s.status = 'ABANDONED',
    s.completion_reason = 'ENDED_EARLY',
    s.completed_at = CURRENT_TIMESTAMP(6)
WHERE ranked.rn > 1;

-- 8. Add generated virtual column and unique index
ALTER TABLE sessions
    ADD COLUMN active_marker TINYINT GENERATED ALWAYS AS (IF(status = 'IN_PROGRESS', 1, NULL)) VIRTUAL;

ALTER TABLE sessions
    ADD CONSTRAINT uk_sessions_user_active UNIQUE (user_id, active_marker);
