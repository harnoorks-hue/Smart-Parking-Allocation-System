
USE smart_parking_db;

DROP PROCEDURE IF EXISTS add_vehicle_details_columns;

DELIMITER //
CREATE PROCEDURE add_vehicle_details_columns()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'vehicles'
          AND column_name = 'vehicle_name'
    ) THEN
        ALTER TABLE vehicles
            ADD COLUMN vehicle_name VARCHAR(100) NOT NULL
                DEFAULT 'Not Specified';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'vehicles'
          AND column_name = 'vehicle_seaters'
    ) THEN
        ALTER TABLE vehicles
            ADD COLUMN vehicle_seaters INT NOT NULL DEFAULT 5;
    END IF;
END//
DELIMITER ;

CALL add_vehicle_details_columns();
DROP PROCEDURE add_vehicle_details_columns;

UPDATE vehicles
SET vehicle_name = 'Honda City', vehicle_seaters = 5
WHERE vehicle_number = 'DEMO-CAR-001';

UPDATE vehicles
SET vehicle_name = 'Royal Enfield Classic', vehicle_seaters = 2
WHERE vehicle_number = 'DEMO-BIKE-002';

UPDATE vehicles
SET vehicle_name = 'Toyota Fortuner', vehicle_seaters = 7
WHERE vehicle_number = 'DEMO-SUV-003';

SELECT vehicle_name, vehicle_number, vehicle_seaters, vehicle_type
FROM vehicles;
