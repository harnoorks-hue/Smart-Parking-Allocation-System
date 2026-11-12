
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt

from parking_data import fetch_table


def get_parking_analytics():

    slots = fetch_table("parking_slots")
    bookings = fetch_table("bookings")

    if slots.empty:
        return {
            "total_slots": 0,
            "occupied_slots": 0,
            "available_slots": 0,
            "maintenance_slots": 0,
            "total_bookings": 0,
            "zone_wise_bookings": {},
            "daily_booking_trends": {}
        }

    # PARKING SLOT STATISTICS

    total_slots = len(slots)

    occupied = int(
        (slots["slot_status"] == "OCCUPIED").sum()
    )

    available = int(
        (slots["slot_status"] == "AVAILABLE").sum()
    )

    maintenance = int(
        (slots["slot_status"] == "MAINTENANCE").sum()
    )

    total_bookings = len(bookings)

    # ZONE-WISE BOOKING STATISTICS

    zone_wise_bookings = {}

    if not bookings.empty:

        zone_usage = bookings.merge(
            slots[["slot_id", "zone_name"]],
            on="slot_id",
            how="left"
        )

        zone_usage["zone_name"] = zone_usage["zone_name"].fillna(
            "Unknown"
        )

        zone_wise_bookings = {
            str(zone): int(count)
            for zone, count in
            zone_usage["zone_name"].value_counts().items()
        }

    # DAILY BOOKING TRENDS

    daily_booking_trends = {}

    if not bookings.empty and "entry_time" in bookings.columns:

        booking_dates = pd.to_datetime(
            bookings["entry_time"],
            errors="coerce"
        )

        daily = booking_dates.dropna().dt.strftime(
            "%Y-%m-%d"
        ).value_counts().sort_index()

        daily_booking_trends = {
            str(date): int(count)
            for date, count in daily.items()
        }

    # RETURN ANALYTICS DATA FOR FLASK API

    return {
        "total_slots": total_slots,
        "occupied_slots": occupied,
        "available_slots": available,
        "maintenance_slots": maintenance,
        "total_bookings": total_bookings,
        "zone_wise_bookings": zone_wise_bookings,
        "daily_booking_trends": daily_booking_trends
    }


def show_parking_statistics():

    data = get_parking_analytics()

    print("\n========== PARKING ANALYTICS ==========")

    print("Total Slots:", data["total_slots"])
    print("Occupied Slots:", data["occupied_slots"])
    print("Available Slots:", data["available_slots"])
    print("Maintenance Slots:", data["maintenance_slots"])

    print("\n========== ZONE-WISE USAGE ==========")

    if data["zone_wise_bookings"]:

        for zone, count in data["zone_wise_bookings"].items():
            print("Zone", zone, ":", count, "bookings")

    else:
        print("No booking records found.")

    print("\n========== BOOKING TRENDS ==========")

    if data["daily_booking_trends"]:

        for date, count in data["daily_booking_trends"].items():
            print(date, ":", count, "bookings")

        dates = list(data["daily_booking_trends"].keys())
        counts = list(data["daily_booking_trends"].values())

        plt.figure(figsize=(9, 5))

        plt.bar(dates, counts)

        plt.title("Daily Parking Bookings")
        plt.xlabel("Date")
        plt.ylabel("Number of Bookings")
        plt.xticks(rotation=45)

        plt.tight_layout()
        plt.savefig("daily_booking_trend.png")
        plt.show()

    else:
        print("No daily booking data available.")

    # PARKING AVAILABILITY CHART

    print("\n========== AVAILABILITY CHART ==========")

    values = np.array([
        data["occupied_slots"],
        data["available_slots"],
        data["maintenance_slots"]
    ])

    plt.figure(figsize=(8, 5))

    plt.bar(
        ["Occupied", "Available", "Maintenance"],
        values
    )

    plt.title("Current Parking Availability")
    plt.ylabel("Number of Slots")

    plt.tight_layout()
    plt.savefig("parking_availability.png")
    plt.show()


if __name__ == "__main__":
    show_parking_statistics()
