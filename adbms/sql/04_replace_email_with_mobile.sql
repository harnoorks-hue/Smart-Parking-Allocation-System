
USE smart_parking_db;

DROP PROCEDURE IF EXISTS remove_legacy_email_column;

DELIMITER //
CREATE PROCEDURE remove_legacy_email_column()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'users'
          AND column_name = 'email'
    ) THEN
        IF NOT EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'users'
              AND column_name = 'mobile_number'
        ) THEN
            SIGNAL SQLSTATE '45000'
                SET MESSAGE_TEXT =
                    'Add and populate mobile_number before removing email.';
        END IF;

        ALTER TABLE users DROP COLUMN email;
    END IF;
END//
DELIMITER ;

CALL remove_legacy_email_column();
DROP PROCEDURE remove_legacy_email_column;

-- Verify the final structure
DESCRIBE users;

SELECT user_id, full_name, mobile_number
FROM users;
