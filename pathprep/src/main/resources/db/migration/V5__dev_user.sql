-- V5: Dev seed user (IDs assigned by DB auto-increment; institution gets id=1, user gets id=1)

INSERT INTO institutions (name, email_domain) VALUES
('National Institute of Engineering', 'nie.edu.in');

INSERT INTO users (institution_id, email, name, role, branch, grad_year, target_track_id) VALUES
(1, 'student@nie.edu.in', 'Dev Student', 'STUDENT', 'CSE', 2026,
 (SELECT id FROM role_tracks WHERE name = 'Java Backend'));
