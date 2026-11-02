package com.uacam.minidbeaver.mini_dbeaver.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RemotoDbServicio {
    private static final RemotoDbServicio INSTANCE =
            new RemotoDbServicio();
    private Connection connection;
    private RemotoDbServicio() {
    }
    public static RemotoDbServicio getInstance() {
        return INSTANCE;
    }

    public synchronized void connect(
            int localPort,
            String dbUser,
            String dbPassword,
            String database) throws SQLException {

        disconnect();

        String url = "jdbc:mariadb://127.0.0.1:" + localPort + "/" + database;
        connection = DriverManager.getConnection(url, dbUser, dbPassword);
    }
    public synchronized List<String> getTables()
            throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException(
                    "No existe una conexión con MariaDB."
            );
        }
        List<String> tables = new ArrayList<>();
        try (var statement = connection.createStatement();
             var result = statement.executeQuery("SHOW TABLES")) {
            while (result.next()) {
                tables.add(result.getString(1));
            }
        }
        return tables;
    }
    public synchronized Connection getConnection()
            throws SQLException {

        if (connection == null || connection.isClosed()) {
            throw new SQLException(
                    "La conexión remota no está disponible"
            );
        }
        return connection;
    }
    public synchronized void disconnect() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
            }
            connection = null;
        }
    }
}