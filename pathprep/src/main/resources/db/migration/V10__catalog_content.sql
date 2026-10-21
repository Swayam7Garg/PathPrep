-- V10: Real production catalog.
-- Strategy:
--   1. Rename legacy dev-seed topic rows that now have canonical names, so existing questions
--      and attempts keep their foreign key IDs. Do NOT delete them.
--   2. Upsert tracks by name (INSERT ... ON DUPLICATE KEY UPDATE).
--   3. Upsert topics by name (INSERT ... ON DUPLICATE KEY UPDATE).
--   4. Upsert track_topics weights (INSERT ... ON DUPLICATE KEY UPDATE).
--   5. Add prerequisite edges (INSERT IGNORE - safe to re-run).
-- Never edit this file after it has run. Add V11... for further changes.

-- ── Step 1: Rename legacy dev-seed topics to their canonical names ────────────
-- This preserves foreign keys from questions, attempts, and skill_mastery.
-- "Java Fundamentals" already has the right name (no rename needed).
-- "Spring Boot Basics"  -> "Spring Boot & REST"
-- "Relational Databases" -> "SQL & Indexing"
-- "Data Structures"      -> "Arrays & Strings"

UPDATE topics SET name = 'Spring Boot & REST'  WHERE name = 'Spring Boot Basics';
UPDATE topics SET name = 'SQL & Indexing'       WHERE name = 'Relational Databases';
UPDATE topics SET name = 'Arrays & Strings'     WHERE name = 'Data Structures';

-- ── Step 2: Upsert tracks ─────────────────────────────────────────────────────
INSERT INTO role_tracks (name, description, company_type)
VALUES
    ('Java Backend',  'Core Java, Spring Boot, JPA, MySQL, and backend system design',         'MIXED'),
    ('Web Frontend',  'JavaScript, TypeScript, React, HTML/CSS, and web APIs',                 'PRODUCT'),
    ('General SDE',   'Data structures, algorithms, CS fundamentals, and aptitude',            'SERVICE')
ON DUPLICATE KEY UPDATE
    description  = VALUES(description),
    company_type = VALUES(company_type);

-- ── Step 3: Upsert topics ─────────────────────────────────────────────────────
-- Shared topics (OOP Concepts, SQL & Indexing, Java Fundamentals, Arrays & Strings) live
-- in this table ONCE and are linked to multiple tracks with different weights below.
INSERT INTO topics (name, domain)
VALUES
    -- Java domain
    ('Java Fundamentals',              'Java'),
    ('OOP Concepts',                   'Java'),
    ('Collections & Generics',         'Java'),
    ('Exception Handling',             'Java'),
    ('Multithreading & Concurrency',   'Java'),
    ('Java 8+ Features',               'Java'),
    ('JVM & Memory',                   'Java'),
    -- Database domain
    ('SQL & Indexing',                 'Databases'),
    ('Normalization & Transactions',   'Databases'),
    -- Spring domain
    ('Spring Boot & REST',             'Spring'),
    ('JPA & Hibernate',                'Spring'),
    -- JavaScript domain
    ('JavaScript Fundamentals',        'JavaScript'),
    ('Closures & Scope',               'JavaScript'),
    ('Async JavaScript',               'JavaScript'),
    -- Web domain
    ('DOM & Browser APIs',             'Web'),
    ('HTML & CSS Layout',              'Web'),
    ('HTTP & Web Security',            'Web'),
    -- React domain
    ('React Fundamentals',             'React'),
    ('React Hooks & State',            'React'),
    -- TypeScript domain
    ('TypeScript Basics',              'TypeScript'),
    -- DSA domain
    ('Arrays & Strings',               'DSA'),
    ('Linked Lists, Stacks & Queues',  'DSA'),
    ('Trees & Graphs',                 'DSA'),
    ('Sorting & Searching',            'DSA'),
    ('Recursion & Dynamic Programming','DSA'),
    ('Time & Space Complexity',        'DSA'),
    -- CS Core domain
    ('Operating Systems',              'CS Core'),
    ('Computer Networks',              'CS Core'),
    -- Aptitude domain
    ('Aptitude & Reasoning',           'Aptitude')
ON DUPLICATE KEY UPDATE
    domain = VALUES(domain);

-- ── Step 4: Upsert track_topics (track → topic weights) ──────────────────────
DELETE FROM track_topics;

-- Java Backend
INSERT INTO track_topics (track_id, topic_id, weight)
SELECT
    (SELECT id FROM role_tracks WHERE name = 'Java Backend'),
    (SELECT id FROM topics WHERE name = t.name),
    t.weight
FROM (
    SELECT 'Java Fundamentals'             AS name, 1.00 AS weight UNION ALL
    SELECT 'OOP Concepts',                           1.50 UNION ALL
    SELECT 'Collections & Generics',                 2.00 UNION ALL
    SELECT 'Exception Handling',                     1.00 UNION ALL
    SELECT 'Multithreading & Concurrency',           2.00 UNION ALL
    SELECT 'Java 8+ Features',                       1.50 UNION ALL
    SELECT 'JVM & Memory',                           1.00 UNION ALL
    SELECT 'SQL & Indexing',                         2.00 UNION ALL
    SELECT 'Spring Boot & REST',                     2.00 UNION ALL
    SELECT 'JPA & Hibernate',                        1.50
) AS t
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- Web Frontend
INSERT INTO track_topics (track_id, topic_id, weight)
SELECT
    (SELECT id FROM role_tracks WHERE name = 'Web Frontend'),
    (SELECT id FROM topics WHERE name = t.name),
    t.weight
FROM (
    SELECT 'JavaScript Fundamentals'   AS name, 2.00 AS weight UNION ALL
    SELECT 'Closures & Scope',                   1.50 UNION ALL
    SELECT 'Async JavaScript',                   2.00 UNION ALL
    SELECT 'DOM & Browser APIs',                 1.00 UNION ALL
    SELECT 'HTML & CSS Layout',                  1.50 UNION ALL
    SELECT 'React Fundamentals',                 2.00 UNION ALL
    SELECT 'React Hooks & State',                2.00 UNION ALL
    SELECT 'TypeScript Basics',                  1.50 UNION ALL
    SELECT 'HTTP & Web Security',                1.00
) AS t
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- General SDE
INSERT INTO track_topics (track_id, topic_id, weight)
SELECT
    (SELECT id FROM role_tracks WHERE name = 'General SDE'),
    (SELECT id FROM topics WHERE name = t.name),
    t.weight
FROM (
    SELECT 'Arrays & Strings'                  AS name, 2.00 AS weight UNION ALL
    SELECT 'Linked Lists, Stacks & Queues',              1.50 UNION ALL
    SELECT 'Trees & Graphs',                             2.00 UNION ALL
    SELECT 'Sorting & Searching',                        1.50 UNION ALL
    SELECT 'Recursion & Dynamic Programming',            2.00 UNION ALL
    SELECT 'Time & Space Complexity',                    1.50 UNION ALL
    SELECT 'OOP Concepts',                               1.00 UNION ALL
    SELECT 'SQL & Indexing',                             1.50 UNION ALL
    SELECT 'Normalization & Transactions',               1.00 UNION ALL
    SELECT 'Operating Systems',                          1.50 UNION ALL
    SELECT 'Computer Networks',                          1.00 UNION ALL
    SELECT 'Aptitude & Reasoning',                       1.00
) AS t
ON DUPLICATE KEY UPDATE weight = VALUES(weight);

-- ── Step 5: Upsert prerequisite edges (INSERT IGNORE) ────────────────────────
-- Format: (topic_id that needs X, prereq_id = X)
DELETE FROM topic_prereqs;

-- Java Backend prereqs
INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'OOP Concepts'),
       (SELECT id FROM topics WHERE name = 'Java Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Collections & Generics'),
       (SELECT id FROM topics WHERE name = 'Java Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Collections & Generics'),
       (SELECT id FROM topics WHERE name = 'OOP Concepts');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Exception Handling'),
       (SELECT id FROM topics WHERE name = 'Java Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Multithreading & Concurrency'),
       (SELECT id FROM topics WHERE name = 'OOP Concepts');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Java 8+ Features'),
       (SELECT id FROM topics WHERE name = 'Collections & Generics');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'JVM & Memory'),
       (SELECT id FROM topics WHERE name = 'Java Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Spring Boot & REST'),
       (SELECT id FROM topics WHERE name = 'OOP Concepts');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'JPA & Hibernate'),
       (SELECT id FROM topics WHERE name = 'SQL & Indexing');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'JPA & Hibernate'),
       (SELECT id FROM topics WHERE name = 'Spring Boot & REST');

-- Web Frontend prereqs
INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Closures & Scope'),
       (SELECT id FROM topics WHERE name = 'JavaScript Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Async JavaScript'),
       (SELECT id FROM topics WHERE name = 'JavaScript Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'DOM & Browser APIs'),
       (SELECT id FROM topics WHERE name = 'JavaScript Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'React Fundamentals'),
       (SELECT id FROM topics WHERE name = 'JavaScript Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'React Hooks & State'),
       (SELECT id FROM topics WHERE name = 'React Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'React Hooks & State'),
       (SELECT id FROM topics WHERE name = 'Closures & Scope');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'TypeScript Basics'),
       (SELECT id FROM topics WHERE name = 'JavaScript Fundamentals');

-- General SDE prereqs
INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Linked Lists, Stacks & Queues'),
       (SELECT id FROM topics WHERE name = 'Arrays & Strings');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Trees & Graphs'),
       (SELECT id FROM topics WHERE name = 'Linked Lists, Stacks & Queues');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Sorting & Searching'),
       (SELECT id FROM topics WHERE name = 'Arrays & Strings');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Recursion & Dynamic Programming'),
       (SELECT id FROM topics WHERE name = 'Arrays & Strings');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'OOP Concepts'),
       (SELECT id FROM topics WHERE name = 'Java Fundamentals');

INSERT IGNORE INTO topic_prereqs (topic_id, prereq_id)
SELECT (SELECT id FROM topics WHERE name = 'Normalization & Transactions'),
       (SELECT id FROM topics WHERE name = 'SQL & Indexing');
