
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from db_connection import get_connection


def get_current_parking_status():

    connection = get_connection()

    if connection is None:
        return None

    try:
        cursor = connection.cursor()

        query = """
        SELECT
            COUNT(*),
            SUM(CASE WHEN slot_status = 'OCCUPIED' THEN 1 ELSE 0 END),
            SUM(CASE WHEN slot_status = 'AVAILABLE' THEN 1 ELSE 0 END)
        FROM parking_slots
        """

        cursor.execute(query)
        result = cursor.fetchone()

        return {
            "total_slots": result[0] or 0,
            "occupied_slots": result[1] or 0,
            "available_slots": result[2] or 0
        }

    except Exception as e:
        print("Database Error:", e)
        return None

    finally:
        if connection.is_connected():
            connection.close()


def train_prediction_model():

    # Expected occupancy changes for each hour.
    # These are demonstration assumptions, not real historical data.

    hourly_changes = {
        0: (-8, -12),
        1: (-8, -12),
        2: (-8, -12),
        3: (-8, -12),
        4: (-8, -12),
        5: (-8, -12),
        6: (5, 9),
        7: (8, 14),
        8: (10, 16),
        9: (8, 14),
        10: (5, 9),
        11: (5, 9),
        12: (5, 9),
        13: (5, 9),
        14: (-3, -5),
        15: (-3, -5),
        16: (-3, -5),
        17: (-3, -5),
        18: (7, 12),
        19: (7, 12),
        20: (7, 12),
        21: (7, 12),
        22: (-8, -12),
        23: (-8, -12)
    }

    training_data = []

    # Generate demonstration training records
    # for different hours and occupancy percentages.

    for hour in range(24):

        change_30, change_60 = hourly_changes[hour]

        for occupancy in range(0, 101, 5):

            future_30 = min(
                100,
                max(0, occupancy + change_30)
            )

            future_60 = min(
                100,
                max(0, occupancy + change_60)
            )

            training_data.append({
                "hour": hour,
                "occupancy_percentage": occupancy,
                "available_after_30": 100 - future_30,
                "available_after_60": 100 - future_60
            })

    df = pd.DataFrame(training_data)

    X = df[["hour", "occupancy_percentage"]]

    y_30 = df["available_after_30"]
    y_60 = df["available_after_60"]

    model_30 = RandomForestRegressor(
        n_estimators=100,
        random_state=42
    )

    model_60 = RandomForestRegressor(
        n_estimators=100,
        random_state=42
    )

    model_30.fit(X, y_30)
    model_60.fit(X, y_60)

    return model_30, model_60


def get_availability_prediction():

    status = get_current_parking_status()

    if status is None:
        return None

    total_slots = status["total_slots"]
    occupied_slots = status["occupied_slots"]
    available_slots = status["available_slots"]

    if total_slots == 0:
        return None

    # Calculate current occupancy percentage.

    occupancy_percentage = (
        occupied_slots / total_slots
    ) * 100

    # Train the Random Forest models.

    model_30, model_60 = train_prediction_model()

    current_hour = pd.Timestamp.now().hour

    input_data = pd.DataFrame([{
        "hour": current_hour,
        "occupancy_percentage": occupancy_percentage
    }])

    # Generate future availability predictions.

    prediction_30_percentage = model_30.predict(
        input_data
    )[0]

    prediction_60_percentage = model_60.predict(
        input_data
    )[0]

    # Convert predicted percentages into parking slots.

    prediction_30 = round(
        (prediction_30_percentage / 100) * total_slots
    )

    prediction_60 = round(
        (prediction_60_percentage / 100) * total_slots
    )

    # Keep predictions within parking capacity.

    prediction_30 = max(
        0,
        min(total_slots, prediction_30)
    )

    prediction_60 = max(
        0,
        min(total_slots, prediction_60)
    )

    return {
        "total_slots": total_slots,
        "occupied_slots": occupied_slots,
        "current_available": available_slots,
        "occupancy_percentage": round(occupancy_percentage, 2),
        "predicted_after_30_minutes": prediction_30,
        "predicted_after_60_minutes": prediction_60,
        "model": "Random Forest Regressor",
        "training_data": "Synthetic demonstration data"
    }


def predict_availability():

    result = get_availability_prediction()

    if result is None:
        print("Unable to generate parking prediction.")
        return

    print("\n===== AI PARKING AVAILABILITY PREDICTION =====")

  

    print("\nCurrent Parking Status:")

    print("Total Slots:", result["total_slots"])
    print("Occupied Slots:", result["occupied_slots"])
    print("Available Slots:", result["current_available"])

    print(
        "Occupancy Percentage:",
        result["occupancy_percentage"], "%"
    )

    print("\nPredicted Available Slots:")

    print(
        "After 30 Minutes:",
        result["predicted_after_30_minutes"]
    )

    print(
        "After 60 Minutes:",
        result["predicted_after_60_minutes"]
    )


if __name__ == "__main__":
    predict_availability()
