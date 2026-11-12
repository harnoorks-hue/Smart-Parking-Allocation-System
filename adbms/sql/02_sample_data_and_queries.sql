
USE smart_parking_db;

-- Add sample users with mobile numbers
INSERT IGNORE INTO users (full_name, mobile_number)
VALUES
('Demo User One', '9876500001'),
('Demo User Two', '9876500002'),
('Demo User Three', '9876500003');

-- Add sample vehicles for their owners
INSERT IGNORE INTO vehicles
    (user_id, vehicle_name, vehicle_number, vehicle_seaters, vehicle_type)
SELECT user_id, 'Honda City', 'DEMO-CAR-001', 5, 'CAR'
FROM users WHERE mobile_number = '9876500001';

INSERT IGNORE INTO vehicles
    (user_id, vehicle_name, vehicle_number, vehicle_seaters, vehicle_type)
SELECT user_id, 'Royal Enfield Classic', 'DEMO-BIKE-002', 2, 'BIKE'
FROM users WHERE mobile_number = '9876500002';

INSERT IGNORE INTO vehicles
    (user_id, vehicle_name, vehicle_number, vehicle_seaters, vehicle_type)
SELECT user_id, 'Toyota Fortuner', 'DEMO-SUV-003', 7, 'SUV'
FROM users WHERE mobile_number = '9876500003';

-- Display users
SELECT * FROM users;

-- Display vehicles with their owners
SELECT
    u.full_name,
    u.mobile_number,
    v.vehicle_name,
    v.vehicle_number,
    v.vehicle_seaters,
    v.vehicle_type
FROM users u
JOIN vehicles v ON u.user_id = v.user_id;

-- Display available slots by distance
SELECT slot_number, floor_number, zone_name, distance_meters
FROM parking_slots
WHERE slot_status = 'AVAILABLE'
ORDER BY distance_meters;

-- Count slots on each floor
SELECT floor_number, COUNT(*) AS total_slots
FROM parking_slots
GROUP BY floor_number;

-- Count slots by status
SELECT slot_status, COUNT(*) AS slot_count
FROM parking_slots
GROUP BY slot_status;
