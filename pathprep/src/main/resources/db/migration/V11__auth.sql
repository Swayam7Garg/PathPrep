-- V11: Authentication & Refresh Tokens

-- 1. Extend users table with Google OAuth & profile metadata
ALTER TABLE users
    ADD COLUMN google_sub VARCHAR(64) NULL UNIQUE AFTER email,
    ADD COLUMN picture_url VARCHAR(500) NULL AFTER name,
    ADD COLUMN last_login_at DATETIME(6) NULL AFTER updated_at;

-- 2. Rotating refresh tokens table
CREATE TABLE refresh_tokens (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id        BIGINT UNSIGNED NOT NULL,
    token_hash     CHAR(64)        NOT NULL,
    family_id      CHAR(36)        NOT NULL,
    expires_at     DATETIME(6)     NOT NULL,
    revoked_at     DATETIME(6)     NULL,
    replaced_by_id BIGINT UNSIGNED NULL,
    created_at     DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    user_agent     VARCHAR(255)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
    KEY idx_refresh_tokens_user_id (user_id),
    KEY idx_refresh_tokens_family_id (family_id),
    KEY idx_refresh_tokens_expires_at (expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
