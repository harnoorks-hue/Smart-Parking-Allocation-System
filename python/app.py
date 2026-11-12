
from flask import Flask, jsonify

from availability_prediction import (
    get_current_parking_status,
    get_availability_prediction
)

from parking_analytics import get_parking_analytics


app = Flask(__name__)


@app.route("/")
def home():

    return jsonify({
        "message": "ParkSmart Python API is running!",
        "status": "Success"
    })


@app.route("/api/parking/status")
def parking_status():

    status = get_current_parking_status()

    if status is None:
        return jsonify({
            "error": "Database connection failed"
        }), 500

    return jsonify(status)


@app.route("/api/parking/prediction")
def parking_prediction():

    prediction = get_availability_prediction()

    if prediction is None:
        return jsonify({
            "error": "Unable to generate parking prediction"
        }), 500

    return jsonify(prediction)


@app.route("/api/parking/analytics")
def parking_analytics():

    analytics = get_parking_analytics()

    if analytics["total_slots"] == 0:
        return jsonify({
            "error": "No parking data found"
        }), 404

    return jsonify(analytics)


if __name__ == "__main__":

    print("Starting ParkSmart Python API...")

    app.run(
        host="127.0.0.1",
        port=5000,
        debug=False
    )