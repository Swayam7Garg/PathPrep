-- V1: catalog (content) tables. MySQL 8.0.16+ (needs CHECK enforcement).
-- IDs are BIGINT UNSIGNED AUTO_INCREMENT (clustered index, smaller than UUID, no page splits).
-- Never edit this file after it has run once. Add V3, V4... for changes.

CREATE TABLE role_tracks (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name         VARCHAR(100)     NOT NULL,
    description  VARCHAR(500)     NULL,
    company_type VARCHAR(20)      NULL,
    created_at   DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_tracks_name (name),
    CONSTRAINT chk_role_tracks_company_type
        CHECK (company_type IS NULL OR company_type IN ('SERVICE', 'PRODUCT', 'MIXED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE topics (
    id         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)     NOT NULL,
    domain     VARCHAR(50)      NOT NULL,
    created_at DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_topics_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Prerequisite edges: "to learn topic_id you should know prereq_id first"
CREATE TABLE topic_prereqs (
    topic_id  BIGINT UNSIGNED NOT NULL,
    prereq_id BIGINT UNSIGNED NOT NULL,
    PRIMARY KEY (topic_id, prereq_id),
    CONSTRAINT fk_topic_prereqs_topic  FOREIGN KEY (topic_id)  REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT fk_topic_prereqs_prereq FOREIGN KEY (prereq_id) REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT chk_topic_prereqs_not_self CHECK (topic_id <> prereq_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- How important each topic is for a track (used by skill-gap ranking later)
CREATE TABLE track_topics (
    track_id BIGINT UNSIGNED  NOT NULL,
    topic_id BIGINT UNSIGNED  NOT NULL,
    weight   DECIMAL(4,2)     NOT NULL DEFAULT 1.00,
    PRIMARY KEY (track_id, topic_id),
    KEY idx_track_topics_topic (topic_id),
    CONSTRAINT fk_track_topics_track FOREIGN KEY (track_id) REFERENCES role_tracks (id) ON DELETE CASCADE,
    CONSTRAINT fk_track_topics_topic FOREIGN KEY (topic_id) REFERENCES topics (id) ON DELETE CASCADE,
    CONSTRAINT chk_track_topics_weight CHECK (weight > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rubrics (
    id         BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    name       VARCHAR(100)     NOT NULL,
    version    INT              NOT NULL DEFAULT 1,
    created_at DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY uk_rubrics_name_version (name, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE rubric_criteria (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    rubric_id   BIGINT UNSIGNED  NOT NULL,
    name        VARCHAR(100)     NOT NULL,
    description VARCHAR(500)     NULL,
    weight      DECIMAL(4,2)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_rubric_criteria_name (rubric_id, name),
    CONSTRAINT fk_rubric_criteria_rubric FOREIGN KEY (rubric_id) REFERENCES rubrics (id) ON DELETE CASCADE,
    CONSTRAINT chk_rubric_criteria_weight CHECK (weight > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE questions (
    id           BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    topic_id     BIGINT UNSIGNED  NOT NULL,
    rubric_id    BIGINT UNSIGNED  NULL,                 -- null for MCQ (graded in code)
    type         VARCHAR(20)      NOT NULL,
    difficulty   TINYINT          NOT NULL,
    body         TEXT             NOT NULL,
    key_points   JSON             NULL,                 -- what a good answer must cover
    model_answer TEXT             NULL,
    status       VARCHAR(20)      NOT NULL DEFAULT 'DRAFT',
    created_at   DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at   DATETIME(6)      NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    KEY idx_questions_topic_status (topic_id, status),
    KEY idx_questions_status (status),
    CONSTRAINT fk_questions_topic  FOREIGN KEY (topic_id)  REFERENCES topics (id),
    CONSTRAINT fk_questions_rubric FOREIGN KEY (rubric_id) REFERENCES rubrics (id),
    CONSTRAINT chk_questions_type       CHECK (type IN ('MCQ', 'CONCEPT', 'SCENARIO', 'BEHAVIOURAL')),
    CONSTRAINT chk_questions_difficulty CHECK (difficulty BETWEEN 1 AND 3),
    CONSTRAINT chk_questions_status     CHECK (status IN ('DRAFT', 'PUBLISHED', 'FLAGGED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE question_options (
    id          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED  NOT NULL,
    position    TINYINT          NOT NULL,
    text        VARCHAR(500)     NOT NULL,
    is_correct  TINYINT(1)       NOT NULL DEFAULT 0,
    explanation TEXT             NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_options_position (question_id, position),
    CONSTRAINT fk_question_options_question FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;