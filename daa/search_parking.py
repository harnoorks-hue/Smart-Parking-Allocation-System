
# Search Parking Slot using Binary Search and MySQL

import mysql.connector
from getpass import getpass


def connect_database():
    password = getpass("Enter MySQL Password: ")

    try:
        connection = mysql.connector.connect(
            host="localhost",
            user="root",
            password=password,
            database="smart_parking_db"
        )
        return connection

    except mysql.connector.Error as e:
        print("Database Connection Error:", e)
        return None


def search_slot(slots, target):
    low = 0
    high = len(slots) - 1

    while low <= high:
        mid = (low + high) // 2

        if slots[mid]["slot_number"] == target:
            return slots[mid]

        elif slots[mid]["slot_number"] < target:
            low = mid + 1

        else:
            high = mid - 1

    return None


def display_search():
    connection = connect_database()

    if connection is None:
        return

    cursor = None

    try:
        cursor = connection.cursor(dictionary=True)

        cursor.execute("""
            SELECT slot_number, zone_name, floor_number,
                   distance_meters, slot_status
            FROM parking_slots
            ORDER BY slot_number
        """)

        slots = cursor.fetchall()

        if not slots:
            print("No Parking Slots Found")
            return

        target = input("Enter Slot Number to Search: ").strip().upper()

        result = search_slot(slots, target)

        if result:
            print("\nPARKING SLOT FOUND")
            print("Slot Number:", result["slot_number"])
            print("Zone:", result["zone_name"])
            print("Floor:", result["floor_number"])
            print("Distance:", result["distance_meters"], "metres")
            print("Status:", result["slot_status"])
        else:
            print("Parking Slot Not Found")

    except mysql.connector.Error as e:
        print("Database Error:", e)

    finally:
        if cursor is not None:
            cursor.close()
        connection.close()


if __name__ == "__main__":
    display_search()
