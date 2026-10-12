-- V4: Sessions and session questions (BIGINT UNSIGNED, no UUID)

CREATE TABLE sessions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL,
    track_id        BIGINT UNSIGNED NOT NULL,
    mode            VARCHAR(20)     NOT NULL,
    style           VARCHAR(20)     NULL,
    difficulty      VARCHAR(20)     NOT NULL DEFAULT 'MIXED',
    status          VARCHAR(20)     NOT NULL DEFAULT 'IN_PROGRESS',
    total_questions INT             NOT NULL,
    current_index   INT             NOT NULL DEFAULT 0,
    started_at      DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    completed_at    DATETIME(6)     NULL,
    PRIMARY KEY (id),
    KEY idx_sessions_user  (user_id),
    KEY idx_sessions_track (track_id),
    CONSTRAINT fk_sessions_user  FOREIGN KEY (user_id)  REFERENCES users (id)       ON DELETE CASCADE,
    CONSTRAINT fk_sessions_track FOREIGN KEY (track_id) REFERENCES role_tracks (id),
    CONSTRAINT chk_sessions_mode       CHECK (mode IN ('QUIZ', 'INTERVIEW')),
    CONSTRAINT chk_sessions_style      CHECK (style IS NULL OR style IN ('LITE', 'LIVE')),
    CONSTRAINT chk_sessions_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD', 'MIXED')),
    CONSTRAINT chk_sessions_status     CHECK (status IN ('IN_PROGRESS', 'EVALUATING', 'COMPLETED', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE session_questions (
    session_id  BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    position    INT             NOT NULL,
    PRIMARY KEY (session_id, position),
    UNIQUE KEY uk_session_questions_question (session_id, question_id),
    CONSTRAINT fk_session_questions_session  FOREIGN KEY (session_id)  REFERENCES sessions  (id) ON DELETE CASCADE,
    CONSTRAINT fk_session_questions_question FOREIGN KEY (question_id) REFERENCES questions (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
