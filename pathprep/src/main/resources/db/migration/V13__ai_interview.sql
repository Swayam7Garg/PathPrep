-- V13: AI Interview Mode (LITE & LIVE), LLM Usage, User Keys, Evaluations, and Criterion Scores

-- Alter attempts table: allow selected_option_id to be NULL for free-text answers,
-- and add answer_text, parent_attempt_id for interview follow-ups.
ALTER TABLE attempts
    MODIFY COLUMN selected_option_id BIGINT UNSIGNED NULL,
    ADD COLUMN answer_text TEXT NULL AFTER selected_option_id,
    ADD COLUMN parent_attempt_id BIGINT UNSIGNED NULL AFTER answer_text,
    ADD CONSTRAINT fk_attempts_parent FOREIGN KEY (parent_attempt_id) REFERENCES attempts(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS user_llm_keys (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(32) NOT NULL DEFAULT 'GROQ',
    encrypted_key VARCHAR(512) NOT NULL,
    key_hint VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_llm_keys_user UNIQUE (user_id),
    CONSTRAINT fk_user_llm_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS llm_usage (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT UNSIGNED NOT NULL,
    usage_date DATE NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NOT NULL,
    call_count INT NOT NULL DEFAULT 0,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    is_byok BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_llm_usage_user_day UNIQUE (user_id, usage_date, provider, model, is_byok),
    CONSTRAINT fk_llm_usage_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_llm_usage_date (usage_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evaluations (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    attempt_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    overall_score DECIMAL(5,2) NULL,
    feedback TEXT NULL,
    strengths TEXT NULL,
    improvements TEXT NULL,
    model_version VARCHAR(64) NULL,
    tokens_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_evaluations_attempt UNIQUE (attempt_id),
    CONSTRAINT fk_evaluations_attempt FOREIGN KEY (attempt_id) REFERENCES attempts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS criterion_scores (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    evaluation_id BIGINT UNSIGNED NOT NULL,
    criterion_id BIGINT UNSIGNED NOT NULL,
    score DECIMAL(5,2) NOT NULL,
    evidence_quote TEXT NULL,
    feedback TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_criterion_scores_evaluation FOREIGN KEY (evaluation_id) REFERENCES evaluations(id) ON DELETE CASCADE,
    CONSTRAINT fk_criterion_scores_criterion FOREIGN KEY (criterion_id) REFERENCES rubric_criteria(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

