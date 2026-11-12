package com.smartparking;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    public static Connection getConnection() throws SQLException {
        String host = setting("SMARTPARKING_DB_HOST", "localhost");
        String port = setting("SMARTPARKING_DB_PORT", "3306");
        String database = setting("SMARTPARKING_DB_NAME", "smart_parking_db");
        String user = setting("SMARTPARKING_DB_USER", "root");
        String password = System.getenv("SMARTPARKING_DB_PASSWORD");

        if (password == null || password.isEmpty()) {
            throw new SQLException(
                    "SMARTPARKING_DB_PASSWORD is not configured."
            );
        }

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";

        try {
            Class<?> driverClass =
                    Class.forName("com.mysql.cj.jdbc.Driver");

            Driver driver =
                    (Driver) driverClass.getDeclaredConstructor().newInstance();

            Properties properties = new Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", password);

            Connection connection = driver.connect(url, properties);

            if (connection == null) {
                throw new SQLException(
                        "The MySQL driver did not accept the database URL."
                );
            }

            return connection;

        } catch (ClassNotFoundException e) {
            throw new SQLException(
                    "MySQL JDBC driver is unavailable.", e
            );
        } catch (ReflectiveOperationException e) {
            throw new SQLException(
                    "Could not initialize the MySQL JDBC driver.", e
            );
        }
    }

    private static String setting(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isEmpty() ? defaultValue : value;
    }
}