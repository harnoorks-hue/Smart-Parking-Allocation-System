
import pandas as pd
from db_connection import get_connection


def fetch_table(table_name):

    allowed_tables = [
        "parking_slots",
        "bookings",
        "parking_events"
    ]

    if table_name not in allowed_tables:
        print("Invalid table name.")
        return pd.DataFrame()

    connection = get_connection()

    if connection is None:
        return pd.DataFrame()

    try:
        query = f"SELECT * FROM {table_name}"
        dataframe = pd.read_sql(query, connection)

        return dataframe

    except Exception as e:
        print("Error fetching", table_name, ":", e)
        return pd.DataFrame()

    finally:
        if connection.is_connected():
            connection.close()


def get_parking_data():

    slots = fetch_table("parking_slots")
    bookings = fetch_table("bookings")
    events = fetch_table("parking_events")

    return slots, bookings, events


def display_parking_data():

    slots, bookings, events = get_parking_data()

    print("\n========== PARKING SLOTS ==========")
    print("Total slot records:", len(slots))
    print(slots.head(10).to_string(index=False))

    print("\n========== BOOKINGS ==========")
    print("Total booking records:", len(bookings))
    print(bookings.head(10).to_string(index=False))

    print("\n========== PARKING EVENTS ==========")
    print("Total event records:", len(events))
    print(events.head(10).to_string(index=False))


if __name__ == "__main__":
    display_parking_data()
