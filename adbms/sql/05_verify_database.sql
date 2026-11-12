
USE smart_parking_db;

-- 1. Check users and mobile numbers
SELECT user_id, full_name, mobile_number
FROM users;

-- 2. Check vehicle details
SELECT vehicle_id, user_id, vehicle_name,
       vehicle_number, vehicle_seaters, vehicle_type
FROM vehicles;

-- 3. Check all parking slots
SELECT slot_id, slot_number, floor_number,
       zone_name, distance_meters, slot_status
FROM parking_slots
ORDER BY distance_meters;

-- 4. Check available parking slots
SELECT slot_number, floor_number, zone_name,
       distance_meters
FROM parking_slots
WHERE slot_status = 'AVAILABLE'
ORDER BY distance_meters;

-- 5. Check users with their vehicles
SELECT u.full_name, u.mobile_number,
       v.vehicle_name, v.vehicle_number,
       v.vehicle_seaters
FROM users u
JOIN vehicles v ON u.user_id = v.user_id;

-- 6. Check the table structures
SHOW TABLES;
