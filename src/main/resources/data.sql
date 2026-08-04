-- Clean up existing data
DELETE FROM meeting_participants;
DELETE FROM meeting;
DELETE FROM slot;

-- Insert sample slots for different users
INSERT INTO slot (id, user_id, start_time, end_time, status)
VALUES
    ('slot-001', 'user1', '2026-08-04 09:00:00', '2026-08-04 10:00:00', 'AVAILABLE'),
    ('slot-002', 'user1', '2026-08-04 14:00:00', '2026-08-04 15:00:00', 'AVAILABLE'),
    ('slot-003', 'user2', '2026-08-04 10:00:00', '2026-08-04 11:00:00', 'AVAILABLE'),
    ('slot-004', 'user2', '2026-08-04 15:00:00', '2026-08-04 16:00:00', 'AVAILABLE'),
    ('slot-005', 'user3', '2026-08-04 11:00:00', '2026-08-04 12:00:00', 'AVAILABLE'),
    ('slot-006', 'user1', '2026-08-05 09:00:00', '2026-08-05 10:00:00', 'AVAILABLE'),
    ('slot-007', 'user3', '2026-08-05 14:00:00', '2026-08-05 15:00:00', 'AVAILABLE');

-- Insert a booked slot with a meeting
INSERT INTO slot (id, user_id, start_time, end_time, status)
VALUES
    ('slot-008', 'user1', '2026-08-04 11:00:00', '2026-08-04 12:00:00', 'BOOKED');

-- Insert sample meetings
INSERT INTO meeting (id, slot_id, organizer_id, title, description, start_time, end_time)
VALUES
    ('meeting-001', 'slot-008', 'user1', 'Team Standup', 'Daily standup meeting', '2026-08-04 11:00:00', '2026-08-04 12:00:00');

-- Insert meeting participants
INSERT INTO meeting_participants (meeting_id, participants)
VALUES
    ('meeting-001', 'alice@example.com'),
    ('meeting-001', 'bob@example.com'),
    ('meeting-001', 'charlie@example.com');