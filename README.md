# Smart Parking System

This project contains a MySQL schema, a Java web application, Python analytics
and prediction endpoints, and command-line parking algorithm demonstrations.

## Requirements

- MySQL 8.x
- Python 3.10 or later
- JDK 17 or later and Maven
- A Jakarta EE 11-compatible application server for the Java WAR

The Java project includes Jakarta REST resources, so a plain Tomcat installation
may also need a Jakarta REST implementation.

## Database setup

Run the scripts in `adbms/sql` against MySQL, starting with
`01_create_database.sql`. The initial schema includes the queue and parking
history tables used by the application. The remaining scripts add sample data,
provide compatibility migrations, and verify the database.

When migrating a legacy `users` table that still has an `email` column, ensure
`mobile_number` exists and has been populated before running
`04_replace_email_with_mobile.sql`; the migration refuses to drop email if
mobile numbers are not available.

## Configure credentials

Set these variables in the shell that will launch Python and the application
server. Use a dedicated MySQL account rather than `root` outside local
development.

```powershell
$env:SMARTPARKING_DB_HOST = "localhost"
$env:SMARTPARKING_DB_PORT = "3306"
$env:SMARTPARKING_DB_NAME = "smart_parking_db"
$env:SMARTPARKING_DB_USER = "root"
$env:SMARTPARKING_DB_PASSWORD = "<your-mysql-password>"
$env:SMARTPARKING_API_KEY = "<a-long-random-secret>"
```

The Java app defaults the host, port, database name, and user to local
development values. `SMARTPARKING_DB_PASSWORD` is required. Set
`SMARTPARKING_API_KEY` before starting the Java server; otherwise protected
queue and diagnostic routes return HTTP 503. Send the configured value in the
`X-API-Key` header when calling those routes.

## Run the Python API

```powershell
cd python
py -m venv .venv
.\.venv\Scripts\Activate.ps1
python -m pip install -r requirements.txt
python app.py
```

The Python service listens on `127.0.0.1:5000` for local use.

## Build and deploy the Java web application

From `aip-web/SmartParkingWeb`, run:

```powershell
mvn clean package
```

Deploy `target/SmartParkingWeb-1.0-SNAPSHOT.war` to a compatible Jakarta EE
server. Start the server from the shell where the database and API-key
environment variables are configured.

## API routes

The Java servlet provides:

- `GET /api/slots`
- `GET /api/slots/search?slotNumber=A01`
- `GET /api/queue` — requires `X-API-Key`
- `POST /api/queue` — requires `X-API-Key`
- `POST /api/queue/process` — requires `X-API-Key`
- `GET /api/ai/prediction`
- `GET /api/ai/analytics`
- `GET /dbTest` — requires `X-API-Key`

The Python service provides `/api/parking/status`, `/api/parking/prediction`,
and `/api/parking/analytics`. Prediction results are based on synthetic
demonstration data, not a trained model over real historical records.
