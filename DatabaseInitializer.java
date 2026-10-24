
package com.example.expense_manager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {

        String createExpensesTable = """
            CREATE TABLE IF NOT EXISTS expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_name TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                created_at TEXT DEFAULT CURRENT_TIMESTAMP
            )
            """;

        try (Connection connection = DatabaseConfig.getConnection();
             Statement statement = connection.createStatement()) {

            // Create the table if it does not exist.
            statement.execute(createExpensesTable);

            // Add user_id only when it is missing.
            if (!hasColumn(connection, "user_id")) {
                statement.execute(
                    "ALTER TABLE expenses ADD COLUMN user_id INTEGER"
                );
                System.out.println("Added user_id column.");
            }

            // Assign unassigned expenses only if exactly one user exists.
            int userCount = countUsers(connection);

            if (userCount == 1) {
                int userId = getOnlyUserId(connection);

                String updateSql = """
                    UPDATE expenses
                    SET user_id = %d
                    WHERE user_id IS NULL
                    """.formatted(userId);

                int updated = statement.executeUpdate(updateSql);

                System.out.println(
                    "Assigned " + updated
                    + " existing expenses to the registered account."
                );

            } else if (userCount == 0) {
                System.out.println(
                    "No registered users found. Existing expenses were not assigned."
                );

            } else {
                System.out.println(
                    "Multiple users found. Existing expenses were not automatically assigned."
                );
            }

            System.out.println("SQLite database connected!");
            System.out.println("Expenses table is ready!");
        }
    }

    private boolean hasColumn(Connection connection, String columnName)
            throws SQLException {

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "PRAGMA table_info(expenses)")) {

            while (result.next()) {
                if (columnName.equalsIgnoreCase(
                        result.getString("name"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private int countUsers(Connection connection) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            return result.next() ? result.getInt(1) : 0;
        }
    }

    private int getOnlyUserId(Connection connection) throws SQLException {
        String sql = "SELECT id FROM users LIMIT 1";

        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {

            if (result.next()) {
                return result.getInt("id");
            }
        }

        throw new SQLException(
                "Expected one registered user but could not find their ID.");
    }
}
