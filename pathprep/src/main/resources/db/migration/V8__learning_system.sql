-- V8: Learning System - Skill Mastery, Spaced Repetition Review Queue, and Daily Activity (Phase 6)

-- 1. Skill Mastery per user and topic
CREATE TABLE skill_mastery (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    topic_id          BIGINT UNSIGNED NOT NULL,
    mastery_score     DECIMAL(5, 1)   NOT NULL,
    attempts_count    INT             NOT NULL DEFAULT 0,
    correct_count     INT             NOT NULL DEFAULT 0,
    recent_outcomes   VARCHAR(20)     NOT NULL DEFAULT '',
    confidence_level  VARCHAR(10)     NOT NULL,
    last_practiced_at DATETIME(6)     NULL,
    updated_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_skill_mastery_user_topic (user_id, topic_id),
    CONSTRAINT fk_skill_mastery_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_skill_mastery_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT chk_skill_mastery_score CHECK (mastery_score >= 0.0 AND mastery_score <= 100.0),
    CONSTRAINT chk_skill_mastery_confidence CHECK (confidence_level IN ('LOW', 'MEDIUM', 'HIGH'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 2. Spaced Repetition Review Queue
CREATE TABLE review_queue (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    question_id       BIGINT UNSIGNED NOT NULL,
    last_attempt_id   BIGINT UNSIGNED NOT NULL,
    due_on            DATE            NOT NULL,
    interval_days     INT             NOT NULL DEFAULT 1,
    repetition_count  INT             NOT NULL DEFAULT 0,
    status            VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_reviewed_at  DATETIME(6)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_review_queue_user_question (user_id, question_id),
    KEY idx_review_queue_user_status_due (user_id, status, due_on),
    CONSTRAINT fk_review_queue_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_queue_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_queue_attempt FOREIGN KEY (last_attempt_id) REFERENCES attempts (id) ON DELETE CASCADE,
    CONSTRAINT chk_review_queue_repetition CHECK (repetition_count >= 0 AND repetition_count <= 4),
    CONSTRAINT chk_review_queue_status CHECK (status IN ('PENDING', 'COMPLETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 3. Review Attempts history
CREATE TABLE review_attempts (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    review_id         BIGINT UNSIGNED NOT NULL,
    user_id           BIGINT UNSIGNED NOT NULL,
    question_id       BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NOT NULL,
    correct           TINYINT(1)      NOT NULL,
    time_spent_sec    INT UNSIGNED    NOT NULL,
    step_before       INT             NOT NULL,
    step_after        INT             NOT NULL,
    created_at        DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_review_attempts_user_created (user_id, created_at),
    CONSTRAINT fk_review_attempts_review FOREIGN KEY (review_id) REFERENCES review_queue (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_attempts_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_attempts_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_attempts_option FOREIGN KEY (selected_option_id) REFERENCES question_options (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 4. Daily Activity & Streaks
CREATE TABLE daily_activity (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    activity_date     DATE            NOT NULL,
    questions_answered INT            NOT NULL DEFAULT 0,
    correct_answers   INT             NOT NULL DEFAULT 0,
    reviews_answered  INT             NOT NULL DEFAULT 0,
    sessions_completed INT            NOT NULL DEFAULT 0,
    active_seconds    INT             NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_daily_activity_user_date (user_id, activity_date),
    CONSTRAINT fk_daily_activity_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
