package dao;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {
    private static final String DB_PATH = "db/merit.db";
    private static final String SCHEMA = "db/schema.sql";
    private static Database instance;
    private Connection conn;

    private Database() {
        boolean fresh = !Files.exists(Path.of(DB_PATH));
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");
            if (fresh) loadSchema();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open DB: " + e.getMessage(), e);
        }
    }

    public static synchronized Database getInstance() {
        if (instance == null) instance = new Database();
        return instance;
    }

    public Connection get() {
        return conn;
    }

    private void loadSchema() throws SQLException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader r = new BufferedReader(new FileReader(SCHEMA))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append('\n');
        } catch (IOException e) {
            throw new RuntimeException("Cannot read " + SCHEMA, e);
        }
        try (Statement st = conn.createStatement()) {
            for (String sql : sb.toString().split(";")) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty()) st.execute(trimmed);
            }
        }
    }
}