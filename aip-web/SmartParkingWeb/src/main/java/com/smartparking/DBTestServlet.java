package com.smartparking;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

@WebServlet("/dbTest")
public class DBTestServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws IOException {

        if (!ApiKeyAuth.authorize(request, response)) {
            return;
        }

        response.setContentType("text/plain;charset=UTF-8");

        try (PrintWriter out = response.getWriter()) {
            try (Connection con = DBConnection.getConnection()) {
                out.println("MySQL connection successful!");
                out.println("Database: " + con.getCatalog());
            } catch (SQLException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.println("MySQL connection failed.");
                getServletContext().log("MySQL connection test failed.", e);
            }
        }
    }
}