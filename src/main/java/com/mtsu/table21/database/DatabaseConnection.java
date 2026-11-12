package com.mtsu.table21.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = System.getenv("TABLE21_DB_URL");
        String username = System.getenv("TABLE21_DB_USER");
        String password = System.getenv("TABLE21_DB_PASSWORD");

        if (url == null || url.isBlank()) {
            throw new SQLException("TABLE21_DB_URL is not configured.");
        }

        if (username == null || username.isBlank()) {
            throw new SQLException("TABLE21_DB_USER is not configured.");
        }

        if (password == null) {
            throw new SQLException("TABLE21_DB_PASSWORD is not configured.");
        }

        return DriverManager.getConnection(url, username, password);
    }
}