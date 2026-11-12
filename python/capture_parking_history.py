import time
from db_connection import get_connection


def capture_parking_status():
    connection = get_connection()

    if connection is None:
        print("Database connection failed.")
        return

    try:
        cursor = connection.cursor()

        query = """
        SELECT
            COUNT(*) AS total_slots,
            SUM(CASE WHEN slot_status = 'OCCUPIED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN slot_status = 'AVAILABLE' THEN 1 ELSE 0 END)
        FROM parking_slots
        """

        cursor.execute(query)
        result = cursor.fetchone()

        total_slots = result[0] or 0
        occupied_slots = result[1] or 0
        available_slots = result[2] or 0

        insert_query = """
        INSERT INTO parking_status_history
        (total_slots, occupied_slots, available_slots)
        VALUES (%s, %s, %s)
        """

        cursor.execute(
            insert_query,
            (total_slots, occupied_slots, available_slots)
        )

        connection.commit()

        print("\nParking Status Saved Successfully!")
        print("Total Slots:", total_slots)
        print("Occupied Slots:", occupied_slots)
        print("Available Slots:", available_slots)

        cursor.close()

    except Exception as e:
        print("Error:", e)

    finally:
        if connection.is_connected():
            connection.close()


if __name__ == "__main__":
    print("Parking History Collection Started.")
    print("A new record will be saved every 5 minutes.")
    print("Press Ctrl + C to stop.")

    try:
        while True:
            capture_parking_status()
            time.sleep(300)

    except KeyboardInterrupt:
        print("\nParking History Collection Stopped.")