
# Greedy Parking Allocation using MySQL

import mysql.connector
from getpass import getpass


def connect_database():
    password = getpass("Enter MySQL Password: ")

    try:
        return mysql.connector.connect(
            host="localhost",
            user="root",
            password=password,
            database="smart_parking_db"
        )

    except mysql.connector.Error as e:
        print("Database Connection Error:", e)
        return None


def greedy_allocation():
    connection = connect_database()

    if connection is None:
        return

    cursor = None

    try:
        cursor = connection.cursor(dictionary=True)

        cursor.execute("""
            SELECT slot_number, zone_name, floor_number,
                   distance_meters
            FROM parking_slots
            WHERE slot_status = 'AVAILABLE'
            ORDER BY distance_meters ASC, slot_number ASC
            LIMIT 1
        """)

        nearest = cursor.fetchone()

        print("\nGREEDY PARKING ALLOCATION")

        if nearest:
            print("Nearest Available Slot:", nearest["slot_number"])
            print("Zone:", nearest["zone_name"])
            print("Floor:", nearest["floor_number"])
            print("Distance:", nearest["distance_meters"], "metres")
            print("Allocation Demonstration: SUCCESS")
        else:
            print("No Parking Slots Available")

    except mysql.connector.Error as e:
        print("Database Error:", e)

    finally:
        if cursor is not None:
            cursor.close()
        connection.close()


if __name__ == "__main__":
    greedy_allocation()
 