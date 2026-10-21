-- V9: Admin content tools schema changes.
-- Never edit this file after it has run. Add V10... for further changes.
--
-- 1. Drop and re-add chk_questions_status to include ARCHIVED.
-- 2. Add admin-tracking columns to questions (source, content_hash, import_batch, flag_reason, reviewed_at).
-- 3. Create question_reports for student "report a problem" feature.

-- ── 1. Drop old status CHECK, re-add with ARCHIVED allowed ───────────────────
ALTER TABLE questions DROP CONSTRAINT chk_questions_status;
ALTER TABLE questions ADD CONSTRAINT chk_questions_status
    CHECK (status IN ('DRAFT', 'PUBLISHED', 'FLAGGED', 'ARCHIVED'));

-- ── 2. New admin tracking columns on questions ────────────────────────────────
-- source: provenance (manual entry, bulk import, future AI generation)
ALTER TABLE questions ADD COLUMN source VARCHAR(20) NOT NULL DEFAULT 'MANUAL'
    AFTER status;
ALTER TABLE questions ADD CONSTRAINT chk_questions_source
    CHECK (source IN ('MANUAL', 'IMPORTED', 'AI_GENERATED'));

-- subtopic: optional finer grouping within a topic (e.g. "HashMap" inside "Collections & Generics").
-- Admin-only field; not exposed in student-facing responses, not included in content_hash.
ALTER TABLE questions ADD COLUMN subtopic VARCHAR(100) NULL
    AFTER source;

-- content_hash: SHA-256 of normalised body + options, to prevent duplicate imports.
-- NULL on existing rows (retroactively computed only when question is edited/imported).
ALTER TABLE questions ADD COLUMN content_hash CHAR(64) NULL UNIQUE
    AFTER subtopic;


-- import_batch: arbitrary label set by the admin at import time (e.g. "java-batch-oct-2026")
ALTER TABLE questions ADD COLUMN import_batch VARCHAR(64) NULL
    AFTER content_hash;

-- flag_reason: human-readable note explaining why the question was flagged
ALTER TABLE questions ADD COLUMN flag_reason VARCHAR(500) NULL
    AFTER import_batch;

-- reviewed_at: set when an admin explicitly reviews and publishes a question
ALTER TABLE questions ADD COLUMN reviewed_at DATETIME(6) NULL
    AFTER flag_reason;

-- ── 3. Student question reports ───────────────────────────────────────────────
-- Allows students to flag bad/incorrect questions. One report per user per question.
CREATE TABLE question_reports (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    reason      VARCHAR(500)    NOT NULL,
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    -- Prevent spam: one report per (user, question)
    UNIQUE KEY uk_question_reports_user_question (user_id, question_id),
    CONSTRAINT fk_question_reports_user     FOREIGN KEY (user_id)     REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_question_reports_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
