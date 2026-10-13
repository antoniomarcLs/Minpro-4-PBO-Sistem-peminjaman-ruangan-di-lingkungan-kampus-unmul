package com.mycompany.peminjamanruangan.database;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    public static Connection getConnection() throws SQLException {
        Properties props = muat();
        return DriverManager.getConnection(
                props.getProperty("db.url"),
                props.getProperty("db.user"),
                props.getProperty("db.password"));
    }

    private static Properties muat() {
        Properties props = new Properties();
        try (InputStream in = DatabaseConnection.class
                .getResourceAsStream("/db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                        "File db.properties tidak ditemukan. "
                        + "Salin db.properties.example menjadi db.properties.");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Gagal membaca db.properties.", e);
        }
        return props;
    }
}
