package com.uacam.minidbeaver.mini_dbeaver.model;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LocalDbServicio {
    private static final LocalDbServicio INSTANCE = new LocalDbServicio();
    private final String url = "jdbc:sqlite:" + Path.of("local_db.db").toAbsolutePath();
    private LocalDbServicio() {
    }
    public static LocalDbServicio getInstance() {
        return INSTANCE;
    }
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
    public boolean tableExists(String tableName) throws SQLException {
        String sql = """
                SELECT name
                FROM sqlite_master
                WHERE type = 'table' AND name = ?
                """;

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
    public void replicateTable(Connection remote, String tableName)
            throws SQLException {

        if (tableExists(tableName)) {
            throw new SQLException("La tabla '" + tableName + "' ya fue descargada. No se sobrescribieron tus datos locales.");
        }
        List<ColumnInfo> columns = new ArrayList<>();
        String database = remote.getCatalog();

        try {
            DatabaseMetaData meta = remote.getMetaData();
            try (ResultSet rs = meta.getColumns(database, null, tableName, null)) {
                while (rs.next()) {
                    columns.add(new ColumnInfo(rs.getString("COLUMN_NAME"), sqliteType(rs.getInt("DATA_TYPE"))));
                }
            }
            if (columns.isEmpty()) {
                try (ResultSet rs = meta.getColumns(null, database, tableName, null)) {
                    while (rs.next()) {columns.add(new ColumnInfo(rs.getString("COLUMN_NAME"), sqliteType(rs.getInt("DATA_TYPE"))));
                    }
                }
            }
        } catch (SQLException e) {
            throw new SQLException("Error obteniendo columnas de MariaDB: " + e.getMessage(), e);
        }
        if (columns.isEmpty()) {
            throw new SQLException("No se encontraron columnas para la tabla: " + tableName);
        }
        String selectSQL = "SELECT * FROM " + quoteMariaDB(tableName);

        try (Statement remoteStatement = remote.createStatement();
             ResultSet rows = remoteStatement.executeQuery(selectSQL);
             Connection local = getConnection()) {

            try (Statement st = local.createStatement()) {
                StringBuilder create = new StringBuilder("CREATE TABLE " + quoteSQLite(tableName) + " (");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) {
                        create.append(", ");
                    }
                    create.append(quoteSQLite(columns.get(i).name)).append(" ").append(columns.get(i).type);
                }
                create.append(")");
                try {
                    st.execute(create.toString());
                } catch (SQLException e) {
                    throw new SQLException("Error creando tabla SQLite. SQL: " + create, e);
                }
            }
            StringBuilder insert = new StringBuilder("INSERT INTO " + quoteSQLite(tableName) + " VALUES (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    insert.append(", ");
                }
                insert.append("?");
            }
            insert.append(")");

            try (PreparedStatement ps = local.prepareStatement(insert.toString())) {
                int count = 0;
                while (rows.next()) {
                    for (int i = 0; i < columns.size(); i++) {
                        ps.setObject(i + 1, rows.getObject(i + 1));
                    }
                    ps.addBatch();
                    count++;
                    if (count % 500 == 0) {
                        ps.executeBatch();
                    }
                }
                ps.executeBatch();
            } catch (SQLException e) {throw new SQLException("Error insertando registros en SQLite: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new SQLException("Error durante la descarga de la tabla " + tableName + ": " + e.getMessage(), e);
        }
    }
    public List<Map<String, Object>> readTable(String tableName)
            throws SQLException {
        List<Map<String, Object>> data = new ArrayList<>();
        String sql = "SELECT * FROM " + quoteSQLite(tableName);

        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            int count = md.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= count; i++) {
                    row.put(md.getColumnLabel(i), rs.getObject(i));
                }
                data.add(row);
            }
        }
        return data;
    }
    public List<String> getColumns(String tableName)
            throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "PRAGMA table_info(" + quoteSQLite(tableName) + ")")) {
            while (rs.next()) {
                columns.add(rs.getString("name"));
            }
        }
        return columns;
    }
    public int executeUpdate(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        }
    }
    public QueryResult executeQuery(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData md = rs.getMetaData();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                columns.add(md.getColumnLabel(i));
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    row.put(columns.get(i - 1), rs.getObject(i));
                }
                rows.add(row);
            }
            return new QueryResult(columns, rows);
        }
    }
    private String sqliteType(int sqlType) {
        return switch (sqlType) {
            case Types.INTEGER, Types.SMALLINT, Types.TINYINT, Types.BIGINT, Types.BIT, Types.BOOLEAN -> "INTEGER";
            case Types.FLOAT, Types.REAL, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> "REAL";
            case Types.BLOB, Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY -> "BLOB";
            default -> "TEXT";
        };
    }
    private static String quoteSQLite(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
    private static String quoteMariaDB(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }
    public static String quote(String identifier) {
        return quoteSQLite(identifier);
    }

    private record ColumnInfo(String name, String type) {
    }

    public record QueryResult(List<String> columns, List<Map<String, Object>> rows) {
    }
}
