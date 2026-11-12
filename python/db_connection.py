
import os

import mysql.connector
from mysql.connector import Error


def get_connection():
    password = os.getenv("SMARTPARKING_DB_PASSWORD")

    if not password:
        print("SMARTPARKING_DB_PASSWORD environment variable is not set.")
        return None

    try:
        connection = mysql.connector.connect(
            host=os.getenv("SMARTPARKING_DB_HOST", "localhost"),
            port=int(os.getenv("SMARTPARKING_DB_PORT", "3306")),
            user=os.getenv("SMARTPARKING_DB_USER", "root"),
            password=password,
            database=os.getenv("SMARTPARKING_DB_NAME", "smart_parking_db")
        )

        if connection.is_connected():
            return connection

        connection.close()
        return None

    except Error as e:
        print("Database Connection Error:", e)
        return None


if __name__ == "__main__":

    connection = get_connection()

    if connection:
        print("MySQL Database Connected Successfully!")
        print("Database Name: smart_parking_db")

        connection.close()
        print("Database Connection Closed.")
    else:
        print("Database Connection Failed.")
