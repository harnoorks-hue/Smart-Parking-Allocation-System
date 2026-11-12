
CREATE DATABASE IF NOT EXISTS smart_parking_db;
USE smart_parking_db;

CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    vehicle_name VARCHAR(100) NOT NULL DEFAULT 'Not Specified',
    vehicle_number VARCHAR(20) NOT NULL UNIQUE,
    vehicle_seaters INT NOT NULL DEFAULT 5,
    vehicle_type ENUM('CAR', 'BIKE', 'SUV') NOT NULL DEFAULT 'CAR',
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS parking_slots (
    slot_id INT AUTO_INCREMENT PRIMARY KEY,
    slot_number VARCHAR(10) NOT NULL UNIQUE,
    floor_number INT NOT NULL DEFAULT 1,
    zone_name VARCHAR(20) NOT NULL DEFAULT 'A',
    distance_meters INT NOT NULL DEFAULT 0,
    slot_status ENUM('AVAILABLE', 'OCCUPIED', 'MAINTENANCE')
        NOT NULL DEFAULT 'AVAILABLE'
);

CREATE TABLE IF NOT EXISTS bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    vehicle_id INT NOT NULL,
    slot_id INT NOT NULL,
    entry_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    exit_time DATETIME NULL,
    booking_status ENUM('BOOKED', 'COMPLETED', 'CANCELLED')
        NOT NULL DEFAULT 'BOOKED',
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id),
    FOREIGN KEY (slot_id) REFERENCES parking_slots(slot_id),
    INDEX idx_booking_status (booking_status),
    INDEX idx_booking_vehicle (vehicle_id)
);

CREATE TABLE IF NOT EXISTS parking_events (
    event_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    event_type ENUM('ENTRY', 'EXIT', 'CANCELLATION') NOT NULL,
    event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(255),
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id)
);

CREATE TABLE IF NOT EXISTS booking_queue (
    queue_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    vehicle_name VARCHAR(100) NOT NULL,
    vehicle_number VARCHAR(20) NOT NULL,
    vehicle_seaters INT NOT NULL,
    vehicle_type ENUM('CAR', 'BIKE', 'SUV') NOT NULL,
    request_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    queue_status ENUM('PENDING', 'PROCESSED') NOT NULL DEFAULT 'PENDING',
    INDEX idx_queue_status_time (queue_status, request_time, queue_id)
);

CREATE TABLE IF NOT EXISTS parking_status_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    captured_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_slots INT NOT NULL,
    occupied_slots INT NOT NULL,
    available_slots INT NOT NULL,
    INDEX idx_parking_history_captured_at (captured_at)
);

INSERT IGNORE INTO parking_slots
    (slot_number, floor_number, zone_name, distance_meters)
VALUES
    ('A01', 1, 'A', 20),
    ('A02', 1, 'A', 35),
    ('A03', 1, 'A', 50),
    ('A04', 1, 'A', 65),
    ('B01', 1, 'B', 25),
    ('B02', 1, 'B', 45),
    ('B03', 1, 'B', 60),
    ('C01', 2, 'C', 30),
    ('C02', 2, 'C', 55),
    ('C03', 2, 'C', 75);

SHOW TABLES;
SELECT * FROM parking_slots;
