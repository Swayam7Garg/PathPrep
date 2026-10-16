-- V6: Attempts table for quiz answer submissions (Phase 4)
--
-- Each row records one MCQ answer submitted by a student during a quiz session.
-- Grading is performed in Java (zero AI): correct column is set at insert time by comparing
-- selectedOptionId against question_options.is_correct.
--
-- Idempotency: clients supply an idempotency_key (UUID). If the same key arrives twice
-- (network retry, double-click), the handler short-circuits and returns the already-stored result.

CREATE TABLE attempts (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_id       BIGINT UNSIGNED NOT NULL,
    user_id          BIGINT UNSIGNED NOT NULL,        -- denormalised for fast per-user queries
    question_id      BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NOT NULL,
    correct          TINYINT(1)      NOT NULL,         -- 1 = correct, 0 = wrong
    idempotency_key  VARCHAR(64)     NOT NULL,         -- client-supplied UUID, de-duplicates retries
    answered_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attempts_idempotency (idempotency_key),
    KEY idx_attempts_session  (session_id),
    KEY idx_attempts_user     (user_id),
    KEY idx_attempts_question (question_id),
    CONSTRAINT fk_attempts_session  FOREIGN KEY (session_id)        REFERENCES sessions         (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempts_user     FOREIGN KEY (user_id)           REFERENCES users            (id) ON DELETE CASCADE,
    CONSTRAINT fk_attempts_question FOREIGN KEY (question_id)       REFERENCES questions        (id),
    CONSTRAINT fk_attempts_option   FOREIGN KEY (selected_option_id) REFERENCES question_options (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
