package com.smartparking;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

final class ApiKeyAuth {

    private ApiKeyAuth() {
    }

    static boolean authorize(HttpServletRequest request,
                             HttpServletResponse response)
            throws IOException {
        String expectedKey = System.getenv("SMARTPARKING_API_KEY");

        if (expectedKey == null || expectedKey.trim().isEmpty()) {
            sendError(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "API key is not configured");
            return false;
        }

        String suppliedKey = request.getHeader("X-API-Key");
        if (suppliedKey == null || !MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            sendError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Valid API key required");
            return false;
        }

        return true;
    }

    private static void sendError(HttpServletResponse response,
                                  int status,
                                  String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"success\":false,\"message\":\"" + message + "\"}"
        );
    }
}
