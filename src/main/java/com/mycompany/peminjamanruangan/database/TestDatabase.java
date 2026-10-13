
package com.mycompany.peminjamanruangan.database;

import java.sql.Connection;
import java.sql.SQLException;

public class TestDatabase {

    public static void main(String[] args) {
        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            System.out.println("MariaDB connection successful!");
            System.out.println(
                    "Database: " +
                    connection.getCatalog()
            );

        } catch (SQLException e) {
            System.out.println("Connection failed!");
        }
    }
}
