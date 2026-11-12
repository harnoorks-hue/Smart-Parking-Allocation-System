
package com.smartparking;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/api/*")
public class ParkingApiServlet extends HttpServlet {

    private static class Slot {
        int id;
        String number;
        int floor;
        String zone;
        String status;

        Slot(int id, String number, int floor,
             String zone, String status) {
            this.id = id;
            this.number = number;
            this.floor = floor;
            this.zone = zone;
            this.status = status;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getPathInfo();

        if ("/queue".equals(path) &&
                !ApiKeyAuth.authorize(request, response)) {
            return;
        }

        if ("/slots".equals(path)) {
            getSlots(response);
        } else if ("/slots/search".equals(path)) {
            searchSlot(request, response);
        } else if ("/queue".equals(path)) {
            getQueue(response);
        } else if ("/ai/prediction".equals(path)) {
            forwardPythonApi("/api/parking/prediction", response);
        } else if ("/ai/analytics".equals(path)) {
            forwardPythonApi("/api/parking/analytics", response);
        } else {
            send(response, 404,
                    "{\"success\":false,\"message\":\"Endpoint not found\"}");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        String path = request.getPathInfo();

        if (("/queue".equals(path) || "/queue/process".equals(path)) &&
                !ApiKeyAuth.authorize(request, response)) {
            return;
        }

        if ("/queue".equals(path)) {
            addToQueue(request, response);
        } else if ("/queue/process".equals(path)) {
            processNextBooking(response);
        } else {
            send(response, 404,
                    "{\"success\":false,\"message\":\"Endpoint not found\"}");
        }
    }

    private void getSlots(HttpServletResponse response)
            throws IOException {

        String sql = "SELECT slot_id, slot_number, floor_number, " +
                "zone_name, slot_status FROM parking_slots " +
                "ORDER BY slot_number";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"slots\":[");

            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                first = false;

                Slot slot = readSlot(rs);
                json.append(slotJson(slot));
            }

            json.append("]}");
            send(response, 200, json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            send(response, 500,
                    "{\"success\":false,\"message\":\"Could not load slots\"}");
        }
    }

    private void searchSlot(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException {

        String target = request.getParameter("slotNumber");

        if (target == null || target.trim().isEmpty()) {
            send(response, 400,
                    "{\"success\":false,\"message\":\"Enter a slot number\"}");
            return;
        }

        String sql = "SELECT slot_id, slot_number, floor_number, " +
                "zone_name, slot_status FROM parking_slots " +
                "ORDER BY slot_number";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            List<Slot> slots = new ArrayList<>();

            while (rs.next()) {
                slots.add(readSlot(rs));
            }

            int low = 0;
            int high = slots.size() - 1;
            int found = -1;

            while (low <= high) {
                int mid = (low + high) / 2;

                int comparison = slots.get(mid).number
                        .compareToIgnoreCase(target.trim());

                if (comparison == 0) {
                    found = mid;
                    break;
                } else if (comparison < 0) {
                    low = mid + 1;
                } else {
                    high = mid - 1;
                }
            }

            if (found >= 0) {
                Slot slot = slots.get(found);

                send(response, 200,
                        "{\"success\":true,\"found\":true,\"slot\":" +
                                slotJson(slot) + "}");
            } else {
                send(response, 200,
                        "{\"success\":true,\"found\":false," +
                                "\"message\":\"Parking slot not found\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(response, 500,
                    "{\"success\":false,\"message\":\"Slot search failed\"}");
        }
    }

    private void getQueue(HttpServletResponse response)
            throws IOException {

        String sql = "SELECT queue_id, full_name, mobile_number, " +
                "vehicle_name, vehicle_number, vehicle_seaters, " +
                "vehicle_type, request_time, queue_status " +
                "FROM booking_queue WHERE queue_status = 'PENDING' " +
                "ORDER BY queue_id ASC";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            StringBuilder json = new StringBuilder();
            json.append("{\"success\":true,\"queue\":[");

            boolean first = true;

            while (rs.next()) {
                if (!first) json.append(",");
                first = false;

                json.append("{")
                        .append("\"queueId\":").append(rs.getInt("queue_id")).append(",")
                        .append("\"fullName\":\"").append(esc(rs.getString("full_name"))).append("\",")
                        .append("\"mobileNumber\":\"").append(esc(rs.getString("mobile_number"))).append("\",")
                        .append("\"vehicleName\":\"").append(esc(rs.getString("vehicle_name"))).append("\",")
                        .append("\"vehicleNumber\":\"").append(esc(rs.getString("vehicle_number"))).append("\",")
                        .append("\"vehicleSeaters\":").append(rs.getInt("vehicle_seaters")).append(",")
                        .append("\"vehicleType\":\"").append(esc(rs.getString("vehicle_type"))).append("\",")
                        .append("\"requestTime\":\"").append(esc(String.valueOf(rs.getTimestamp("request_time")))).append("\"")
                        .append("}");
            }

            json.append("]}");
            send(response, 200, json.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            send(response, 500,
                    "{\"success\":false,\"message\":\"Could not load queue\"}");
        }
    }

    private void addToQueue(HttpServletRequest request,
                            HttpServletResponse response)
            throws IOException {

        String fullName = value(request, "fullName");
        String mobile = value(request, "mobileNumber");
        String vehicleName = value(request, "vehicleName");
        String vehicleNumber = value(request, "vehicleNumber").toUpperCase();
        String vehicleType = value(request, "vehicleType").toUpperCase();

        int seaters;

        try {
            seaters = Integer.parseInt(value(request, "vehicleSeaters"));
        } catch (NumberFormatException e) {
            send(response, 400,
                    "{\"success\":false,\"message\":\"Enter valid vehicle seats\"}");
            return;
        }

        if (fullName.isEmpty() || mobile.isEmpty() ||
                vehicleName.isEmpty() || vehicleNumber.isEmpty() ||
                vehicleType.isEmpty() || !mobile.matches("\\d{10}") ||
                seaters < 1 || seaters > 100) {

            send(response, 400,
                    "{\"success\":false,\"message\":\"Please enter valid details\"}");
            return;
        }

        if (!vehicleType.matches("CAR|BIKE|SUV")) {
            send(response, 400,
                    "{\"success\":false,\"message\":\"Invalid vehicle type\"}");
            return;
        }

        String sql = "INSERT INTO booking_queue " +
                "(full_name, mobile_number, vehicle_name, vehicle_number, " +
                "vehicle_seaters, vehicle_type, queue_status) " +
                "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, fullName);
            ps.setString(2, mobile);
            ps.setString(3, vehicleName);
            ps.setString(4, vehicleNumber);
            ps.setInt(5, seaters);
            ps.setString(6, vehicleType);

            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    send(response, 201,
                            "{\"success\":true,\"message\":\"Request added to queue\"," +
                                    "\"queueId\":" + keys.getInt(1) + "}");
                } else {
                    send(response, 500,
                            "{\"success\":false,\"message\":\"Queue request was not created\"}");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(response, 500,
                    "{\"success\":false,\"message\":\"Could not add request to queue\"}");
        }
    }

    private void processNextBooking(HttpServletResponse response)
            throws IOException {

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            try {
                int queueId;
                String fullName;
                String mobile;
                String vehicleName;
                String vehicleNumber;
                String vehicleType;
                int seaters;

                String queueSql = "SELECT * FROM booking_queue " +
                        "WHERE queue_status = 'PENDING' " +
                        "ORDER BY queue_id ASC LIMIT 1 FOR UPDATE";

                try (PreparedStatement ps = con.prepareStatement(queueSql);
                     ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        con.rollback();
                        send(response, 409,
                                "{\"success\":false,\"message\":\"No pending requests\"}");
                        return;
                    }

                    queueId = rs.getInt("queue_id");
                    fullName = rs.getString("full_name");
                    mobile = rs.getString("mobile_number");
                    vehicleName = rs.getString("vehicle_name");
                    vehicleNumber = rs.getString("vehicle_number");
                    vehicleType = rs.getString("vehicle_type");
                    seaters = rs.getInt("vehicle_seaters");
                }

                int userId = findOrCreateUser(con, fullName, mobile);

                int vehicleId = insertVehicle(
                        con, userId, vehicleName,
                        vehicleNumber, seaters, vehicleType
                );

                int slotId;
                String slotNumber;

                String slotSql = "SELECT slot_id, slot_number " +
                        "FROM parking_slots WHERE slot_status = 'AVAILABLE' " +
                        "ORDER BY distance_meters ASC, slot_id ASC " +
                        "LIMIT 1 FOR UPDATE";

                try (PreparedStatement ps = con.prepareStatement(slotSql);
                     ResultSet rs = ps.executeQuery()) {

                    if (!rs.next()) {
                        con.rollback();
                        send(response, 409,
                                "{\"success\":false,\"message\":\"No available parking slots\"}");
                        return;
                    }

                    slotId = rs.getInt("slot_id");
                    slotNumber = rs.getString("slot_number");
                }

                int bookingId;

                String bookingSql = "INSERT INTO bookings " +
                        "(user_id, vehicle_id, slot_id, entry_time, booking_status) " +
                        "VALUES (?, ?, ?, NOW(), 'BOOKED')";

                try (PreparedStatement ps = con.prepareStatement(
                        bookingSql, Statement.RETURN_GENERATED_KEYS)) {

                    ps.setInt(1, userId);
                    ps.setInt(2, vehicleId);
                    ps.setInt(3, slotId);

                    ps.executeUpdate();

                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Booking ID not generated");
                        }

                        bookingId = keys.getInt(1);
                    }
                }

                String updateSlot = "UPDATE parking_slots " +
                        "SET slot_status = 'OCCUPIED' " +
                        "WHERE slot_id = ? AND slot_status = 'AVAILABLE'";

                try (PreparedStatement ps = con.prepareStatement(updateSlot)) {
                    ps.setInt(1, slotId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Slot allocation failed");
                    }
                }

                String updateQueue = "UPDATE booking_queue " +
                        "SET queue_status = 'PROCESSED' " +
                        "WHERE queue_id = ? AND queue_status = 'PENDING'";

                try (PreparedStatement ps = con.prepareStatement(updateQueue)) {
                    ps.setInt(1, queueId);

                    if (ps.executeUpdate() != 1) {
                        throw new SQLException("Queue update failed");
                    }
                }

                con.commit();

                send(response, 200,
                        "{\"success\":true,\"message\":\"Booking successful\"," +
                                "\"queueId\":" + queueId +
                                ",\"bookingId\":" + bookingId +
                                ",\"slotNumber\":\"" + esc(slotNumber) + "\"}");

            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            send(response, 500,
                    "{\"success\":false,\"message\":\"Could not process booking\"}");
        }
    }

    private int findOrCreateUser(Connection con,
                                 String name,
                                 String mobile)
            throws SQLException {

        String findSql = "SELECT user_id FROM users WHERE mobile_number = ?";

        try (PreparedStatement ps = con.prepareStatement(findSql)) {
            ps.setString(1, mobile);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        }

        String insertSql = "INSERT INTO users " +
                "(full_name, mobile_number) VALUES (?, ?)";

        try (PreparedStatement ps = con.prepareStatement(
                insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, name);
            ps.setString(2, mobile);
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Could not create user");
    }

    private int insertVehicle(Connection con,
                              int userId,
                              String name,
                              String number,
                              int seaters,
                              String type)
            throws SQLException {

        String findSql = "SELECT vehicle_id, user_id " +
                "FROM vehicles WHERE vehicle_number = ? FOR UPDATE";

        try (PreparedStatement ps = con.prepareStatement(findSql)) {
            ps.setString(1, number);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int existingVehicleId = rs.getInt("vehicle_id");
                    int existingUserId = rs.getInt("user_id");

                    if (existingUserId != userId) {
                        throw new SQLException(
                                "Vehicle number is already registered to another user."
                        );
                    }

                    return existingVehicleId;
                }
            }
        }

        String insertSql = "INSERT INTO vehicles " +
                "(user_id, vehicle_name, vehicle_number, " +
                "vehicle_seaters, vehicle_type) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(
                insertSql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setString(2, name);
            ps.setString(3, number);
            ps.setInt(4, seaters);
            ps.setString(5, type);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        throw new SQLException("Could not register vehicle");
    }

    private Slot readSlot(ResultSet rs) throws SQLException {
        return new Slot(
                rs.getInt("slot_id"),
                rs.getString("slot_number"),
                rs.getInt("floor_number"),
                rs.getString("zone_name"),
                rs.getString("slot_status")
        );
    }

    private String slotJson(Slot slot) {
        return "{\"slotId\":" + slot.id +
                ",\"slotNumber\":\"" + esc(slot.number) +
                "\",\"floorNumber\":" + slot.floor +
                ",\"zoneName\":\"" + esc(slot.zone) +
                "\",\"status\":\"" + esc(slot.status) + "\"}";
    }

    private String value(HttpServletRequest request, String key) {
        String result = request.getParameter(key);
        return result == null ? "" : result.trim();
    }

    private String esc(String value) {
        if (value == null) return "";

        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void send(HttpServletResponse response,
                      int status,
                      String json)
            throws IOException {

        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(json);
    }

    private void forwardPythonApi(String pythonPath,
                                  HttpServletResponse response)
            throws IOException {

        HttpURLConnection connection = null;

        try {
            URL url = new URL("http://127.0.0.1:5000" + pythonPath);
            connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(15000);
            connection.setRequestProperty("Accept", "application/json");

            int status = connection.getResponseCode();

            InputStream inputStream = (status >= 200 && status < 400)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            if (inputStream == null) {
                send(response, 502,
                        "{\"success\":false,\"message\":\"Python API returned an empty response\"}");
                return;
            }

            StringBuilder result = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, "UTF-8"))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }
            }

            response.setStatus(status);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(result.toString());

        } catch (IOException e) {
            e.printStackTrace();
            send(response, 502,
                    "{\"success\":false,\"message\":\"Could not connect to Python Flask API. Make sure app.py is running on port 5000.\"}");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
