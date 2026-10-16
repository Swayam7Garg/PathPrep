-- V2: dev seed data (3 tracks, 4 topics, 1 rubric, 2 questions)
-- No hard-coded IDs; sub-selects used for FK lookups.

INSERT INTO role_tracks (name, description, company_type) VALUES
('Java Backend',  'Core Java, Spring Boot, MySQL, and backend system design', 'PRODUCT'),
('Web Frontend',  'HTML, CSS, modern JavaScript, and React',                  'PRODUCT'),
('General SDE',   'Data structures, algorithms, CS fundamentals, and problem solving', 'MIXED');

INSERT INTO topics (name, domain) VALUES
('Java Fundamentals',   'Programming'),
('Spring Boot Basics',  'Frameworks'),
('Relational Databases','Databases'),
('Data Structures',     'CS Fundamentals');

-- Spring Boot Basics requires Java Fundamentals
INSERT INTO topic_prereqs (topic_id, prereq_id) VALUES
((SELECT id FROM topics WHERE name = 'Spring Boot Basics'),
 (SELECT id FROM topics WHERE name = 'Java Fundamentals'));

-- Track topics with weights
INSERT INTO track_topics (track_id, topic_id, weight) VALUES
((SELECT id FROM role_tracks WHERE name = 'Java Backend'), (SELECT id FROM topics WHERE name = 'Java Fundamentals'),    1.50),
((SELECT id FROM role_tracks WHERE name = 'Java Backend'), (SELECT id FROM topics WHERE name = 'Spring Boot Basics'),   2.00),
((SELECT id FROM role_tracks WHERE name = 'Java Backend'), (SELECT id FROM topics WHERE name = 'Relational Databases'), 1.50),
((SELECT id FROM role_tracks WHERE name = 'Java Backend'), (SELECT id FROM topics WHERE name = 'Data Structures'),      1.00),
((SELECT id FROM role_tracks WHERE name = 'Web Frontend'), (SELECT id FROM topics WHERE name = 'Data Structures'),      1.00),
((SELECT id FROM role_tracks WHERE name = 'General SDE'),  (SELECT id FROM topics WHERE name = 'Data Structures'),      2.00),
((SELECT id FROM role_tracks WHERE name = 'General SDE'),  (SELECT id FROM topics WHERE name = 'Relational Databases'), 1.20);

-- Rubric and criteria
INSERT INTO rubrics (name, version) VALUES ('Backend Engineering Rubric', 1);

INSERT INTO rubric_criteria (rubric_id, name, description, weight) VALUES
((SELECT id FROM rubrics WHERE name = 'Backend Engineering Rubric' AND version = 1),
 'Technical Correctness', 'Evaluates accuracy of technical concepts and syntax', 2.00),
((SELECT id FROM rubrics WHERE name = 'Backend Engineering Rubric' AND version = 1),
 'Clarity & Communication', 'Evaluates structured explanation and clarity', 1.00);

-- Question 1: Java Fundamentals MCQ (difficulty 1)
INSERT INTO questions (topic_id, rubric_id, type, difficulty, body, key_points, model_answer, status) VALUES
((SELECT id FROM topics WHERE name = 'Java Fundamentals'),
 NULL, 'MCQ', 1,
 'What is the default value of a boolean primitive variable in Java?',
 NULL, NULL, 'PUBLISHED');

SET @q1 = LAST_INSERT_ID();

INSERT INTO question_options (question_id, position, text, is_correct, explanation) VALUES
(@q1, 0, 'true',  0, 'In Java, boolean primitives default to false, not true.'),
(@q1, 1, 'false', 1, 'In Java, boolean primitives default to false.'),
(@q1, 2, 'null',  0, 'Primitives cannot be null in Java.'),
(@q1, 3, '0',     0, 'Java booleans are boolean literals, not integers.');

-- Question 2: Spring Boot Basics MCQ (difficulty 2)
INSERT INTO questions (topic_id, rubric_id, type, difficulty, body, key_points, model_answer, status) VALUES
((SELECT id FROM topics WHERE name = 'Spring Boot Basics'),
 NULL, 'MCQ', 2,
 'Which annotation in Spring Boot marks the main class to enable auto-configuration, component scanning, and extra configuration?',
 NULL, NULL, 'PUBLISHED');

SET @q2 = LAST_INSERT_ID();

INSERT INTO question_options (question_id, position, text, is_correct, explanation) VALUES
(@q2, 0, '@SpringBootApplication',   1, '@SpringBootApplication is a convenience annotation that wraps @Configuration, @EnableAutoConfiguration, and @ComponentScan.'),
(@q2, 1, '@EnableAutoConfiguration', 0, 'While @EnableAutoConfiguration enables auto-configuration, @SpringBootApplication also scans components and defines configuration.'),
(@q2, 2, '@ComponentScan',           0, '@ComponentScan only scans for components.'),
(@q2, 3, '@Configuration',           0, '@Configuration alone does not enable auto-configuration.');
