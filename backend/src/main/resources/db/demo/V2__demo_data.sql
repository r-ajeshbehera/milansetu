-- =====================================================================
-- DEMONSTRATION DATA ONLY (dev profile). Fictional people and event.
-- Every demo account password: Demo@1234   (development only!)
-- Dates are relative to NOW() so the demo event is always 'live' and timelines sort correctly.
-- =====================================================================
SET @pw = '$2a$12$WqQ9oB4wH0miXWH9wk2YQeXXaimEwktYzievjQlfmqYDePztcyDEW';

INSERT INTO users (id, full_name, email, phone, password_hash, role) VALUES
 (1, 'Event Admin (Demo)',      'admin@milansetu.demo',     NULL,         @pw, 'ADMIN'),
 (2, 'Police Control (Demo)',   'police@milansetu.demo',    NULL,         @pw, 'POLICE'),
 (3, 'Singhadwara Desk (Demo)', 'desk1@milansetu.demo',     NULL,         @pw, 'DESK'),
 (4, 'Gundicha Desk (Demo)',    'desk2@milansetu.demo',     NULL,         @pw, 'DESK'),
 (5, 'Volunteer Asha (Demo)',   'volunteer@milansetu.demo', NULL,         @pw, 'VOLUNTEER'),
 (6, 'Family Mohanty (Demo)',   'family1@milansetu.demo',   '9000000001', @pw, 'FAMILY'),
 (7, 'Family Sahu (Demo)',      'family2@milansetu.demo',   '9000000002', @pw, 'FAMILY');

INSERT INTO events (id, name, city, start_date, end_date, active, created_by) VALUES
 (1, 'Rath Yatra (DEMO)', 'Puri', CURDATE() - INTERVAL 2 DAY, CURDATE() + INTERVAL 7 DAY, TRUE, 1),
 (2, 'Bali Jatra (DEMO)', 'Cuttack', CURDATE() + INTERVAL 30 DAY, CURDATE() + INTERVAL 37 DAY, FALSE, 1);

-- Approximate points along Bada Danda (Grand Road), Puri
INSERT INTO help_desks (id, event_id, name, latitude, longitude, in_charge_id) VALUES
 (1, 1, 'Singhadwara Help Desk', 19.804900, 85.818200, 3),
 (2, 1, 'Gundicha Temple Help Desk', 19.813500, 85.831200, 4),
 (3, 1, 'Medical Camp Help Desk', 19.809000, 85.824500, NULL);

INSERT INTO event_staff (event_id, user_id, desk_id, assigned_by) VALUES
 (1, 2, NULL, 1), (1, 3, 1, 1), (1, 4, 2, 1), (1, 5, 1, 1);

-- Registered members. Demo QR tokens are fixed so the scanner can be tried with manual entry.
INSERT INTO members (id, guardian_id, event_id, full_name, age, gender, notes, qr_token) VALUES
 (1, 6, 1, 'Riya Mohanty', 6, 'FEMALE', 'Allergic to peanuts (private note)', '3f2a9c1e-7b4d-4e8a-9c21-5d6f7a8b9c01'),
 (2, 6, 1, 'Bhagaban Mohanty', 74, 'MALE', NULL, '8d1e2f3a-4b5c-4d6e-8f70-1a2b3c4d5e02'),
 (3, 7, 1, 'Arjun Sahu', 9, 'MALE', NULL, 'c4b3a291-0f1e-4d2c-9b8a-7e6d5c4b3a03');

-- A missing report (family 2, registered member Arjun) and a matching manual found report (score 90).
INSERT INTO missing_reports (id, event_id, reported_by, member_id, person_name, approx_age, gender, clothing_color,
                             last_seen_lat, last_seen_lng, last_seen_at) VALUES
 (1, 1, 7, 3, 'Arjun Sahu', 9, 'MALE', 'YELLOW', 19.806000, 85.820000, NOW() - INTERVAL 150 MINUTE);
INSERT INTO found_reports (id, event_id, desk_id, reported_by, source, approx_age, gender, clothing_color,
                           found_lat, found_lng, found_at, safe_notes) VALUES
 (1, 1, 2, 4, 'MANUAL', 10, 'MALE', 'YELLOW', 19.812000, 85.829000, NOW() - INTERVAL 60 MINUTE, 'Boy says his father sells flowers');

INSERT INTO case_files (id, event_id, missing_report_id, member_id, status, opened_at) VALUES (1, 1, 1, 3, 'REPORTED', NOW() - INTERVAL 145 MINUTE);
INSERT INTO case_files (id, event_id, found_report_id, assigned_desk_id, status, opened_at) VALUES (2, 1, 1, 2, 'REPORTED', NOW() - INTERVAL 59 MINUTE);
INSERT INTO case_events (case_id, from_status, status, note, changed_by, changed_at) VALUES
 (1, NULL, 'REPORTED', 'Missing report filed by family', 7, NOW() - INTERVAL 145 MINUTE),
 (2, NULL, 'REPORTED', 'Found person registered at desk', 4, NOW() - INTERVAL 59 MINUTE);
