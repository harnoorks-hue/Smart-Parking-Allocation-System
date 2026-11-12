
# FIFO Booking Queue using MySQL

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


def add_booking():
    connection = connect_database()

    if connection is None:
        return

    cursor = None

    try:
        cursor = connection.cursor()

        print("\nADD BOOKING REQUEST")

        name = input("Enter Full Name: ")
        mobile = input("Enter Mobile Number: ")
        vehicle = input("Enter Vehicle Number: ").upper()
        vehicle_name = input("Enter Vehicle Name: ")
        vehicle_type = input("Enter Vehicle Type (CAR/BIKE/SUV): ").upper()
        seaters = int(input("Enter Number of Seaters: "))

        query = """
            INSERT INTO booking_queue
            (full_name, mobile_number, vehicle_number,
             vehicle_name, vehicle_type, vehicle_seaters,
             queue_status)
            VALUES (%s, %s, %s, %s, %s, %s, 'PENDING')
        """

        values = (name, mobile, vehicle, vehicle_name,
                  vehicle_type, seaters)

        cursor.execute(query, values)
        connection.commit()

        print("Booking Request Added Successfully!")

    except (mysql.connector.Error, ValueError) as e:
        connection.rollback()
        print("Error:", e)

    finally:
        if cursor is not None:
            cursor.close()
        connection.close()


def process_booking():
    connection = connect_database()

    if connection is None:
        return

    cursor = None

    try:
        cursor = connection.cursor(dictionary=True)

        cursor.execute("""
            SELECT queue_id, vehicle_number
            FROM booking_queue
            WHERE queue_status = 'PENDING'
            ORDER BY request_time, queue_id
            LIMIT 1
        """)

        booking = cursor.fetchone()

        if booking:
            print("First Booking in Queue:", booking["vehicle_number"])
            print("Queue ID:", booking["queue_id"])
            print("FIFO Processing Demonstration Only")
            print("Actual parking allocation is handled by Java.")
        else:
            print("No Pending Booking Requests")

    except mysql.connector.Error as e:
        print("Database Error:", e)

    finally:
        if cursor is not None:
            cursor.close()
        connection.close()


def display_queue():
    connection = connect_database()

    if connection is None:
        return

    cursor = None

    try:
        cursor = connection.cursor(dictionary=True)

        cursor.execute("""
            SELECT queue_id, full_name, vehicle_number,
                   vehicle_type, request_time, queue_status
            FROM booking_queue
            WHERE queue_status = 'PENDING'
            ORDER BY request_time, queue_id
        """)

        bookings = cursor.fetchall()

        print("\nPENDING BOOKING QUEUE")

        if bookings:
            for booking in bookings:
                print("-------------------------")
                print("Queue ID:", booking["queue_id"])
                print("Name:", booking["full_name"])
                print("Vehicle:", booking["vehicle_number"])
                print("Type:", booking["vehicle_type"])
                print("Status:", booking["queue_status"])
        else:
            print("Booking Queue is Empty")

    except mysql.connector.Error as e:
        print("Database Error:", e)

    finally:
        if cursor is not None:
            cursor.close()
        connection.close()
