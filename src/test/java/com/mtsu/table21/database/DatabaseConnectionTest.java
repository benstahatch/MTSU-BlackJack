package com.mtsu.table21.database;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseConnectionTest {

    @Test
    void testDatabaseConnection() throws SQLException {

        try (Connection connection = DatabaseConnection.getConnection()) {

            assertNotNull(connection);
            assertFalse(connection.isClosed());
            assertTrue(connection.isValid(5));

            System.out.println("MySQL connection successful!");

        }
    }
}