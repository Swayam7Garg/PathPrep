-- V12: User-Defined Daily Plan & Interactive Checklist (Phase 8)

CREATE TABLE daily_plan_items (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    plan_date      DATE            NOT NULL,
    title          VARCHAR(255)    NOT NULL,
    item_type      VARCHAR(32)     NOT NULL,
    track_id       BIGINT UNSIGNED NULL,
    topic_id       BIGINT UNSIGNED NULL,
    question_count INT             NULL DEFAULT 5,
    session_id     BIGINT UNSIGNED NULL,
    completed      BOOLEAN         NOT NULL DEFAULT FALSE,
    completed_at   DATETIME(6)     NULL,
    sort_order     INT             NOT NULL DEFAULT 0,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_daily_plan_user_date (user_id, plan_date),
    KEY idx_daily_plan_session (session_id),
    CONSTRAINT fk_daily_plan_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_daily_plan_track FOREIGN KEY (track_id) REFERENCES role_tracks (id) ON DELETE SET NULL,
    CONSTRAINT fk_daily_plan_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE SET NULL,
    CONSTRAINT fk_daily_plan_session FOREIGN KEY (session_id) REFERENCES sessions (id) ON DELETE SET NULL,
    CONSTRAINT chk_daily_plan_item_type CHECK (item_type IN ('CUSTOM', 'QUIZ'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
