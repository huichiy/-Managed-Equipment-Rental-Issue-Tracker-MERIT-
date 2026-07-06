package merit.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the SQLite connection and the schema/seed bootstrap for the whole system.
 *
 * <p>This is the day-1 critical path: every {@code *DaoSqlite} takes the
 * {@link Connection} handed out by {@link #getConnection()}, so the tables must
 * exist and be seeded before any DAO smoke test can run. The DAOs never import
 * this class — they only depend on {@code java.sql.Connection} — so persistence
 * stays decoupled from the rest of the code.
 *
 * <p>Both {@code CREATE TABLE} and the seed are idempotent: tables use
 * {@code IF NOT EXISTS} and seed rows are inserted only when a table is empty,
 * so restarting the app never duplicates data or trips a primary-key clash.
 *
 * <p>Schema and seed values are copied verbatim from
 * {@code docs/MERIT-assumptions-and-data-dictionary.md} — do not invent numbers here.
 */
public final class Database {

    private static final String DB_DIR = "db";
    private static final String DB_URL = "jdbc:sqlite:db/merit.db";

    /** Single shared connection — adequate for a single-user desktop demo. */
    private static Connection connection;

    private Database() {
    }

    /** Lazily connects, creates tables, and seeds on first call. */
    public static synchronized Connection getConnection() {
        if (connection == null) {
            init();
        }
        return connection;
    }

    private static void init() {
        // SQLite will not create the parent directory for us.
        new File(DB_DIR).mkdirs();
        ensureDriverLoaded();
        try {
            connection = DriverManager.getConnection(DB_URL);
            try (Statement st = connection.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
            createTables();
            seedIfEmpty();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database at " + DB_URL, e);
        }
    }

    private static void ensureDriverLoaded() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "SQLite JDBC driver not found. Put sqlite-jdbc.jar in lib/ and add it to the classpath "
                            + "(e.g. -cp \"bin;lib/sqlite-jdbc.jar\").",
                    e);
        }
    }

    // ---- schema: four tables, verbatim from the data dictionary (Part 2) ----
    private static void createTables() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "user_id    TEXT PRIMARY KEY, "
                    + "username   TEXT UNIQUE NOT NULL, "
                    + "password   TEXT NOT NULL, "
                    + "name       TEXT NOT NULL, "
                    + "role       TEXT NOT NULL, "
                    + "final_year INTEGER NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS equipment ("
                    + "equipment_id       TEXT PRIMARY KEY, "
                    + "name               TEXT NOT NULL, "
                    + "category           TEXT NOT NULL, "
                    + "daily_rate         REAL NOT NULL, "
                    + "replacement_value  REAL NOT NULL, "
                    + "pricing_policy     TEXT NOT NULL, "
                    + "total_quantity     INTEGER NOT NULL DEFAULT 0, "
                    + "available_quantity INTEGER NOT NULL DEFAULT 0)");

            st.execute("CREATE TABLE IF NOT EXISTS rentals ("
                    + "rental_id    TEXT PRIMARY KEY, "
                    + "user_id      TEXT NOT NULL, "
                    + "equipment_id TEXT NOT NULL, "
                    + "rental_days  INTEGER NOT NULL, "
                    + "quantity     INTEGER NOT NULL DEFAULT 1, "
                    + "rent_date    TEXT NOT NULL, "
                    + "due_date     TEXT NOT NULL, "
                    + "return_date  TEXT, "
                    + "days_late    INTEGER DEFAULT 0, "
                    + "damaged      INTEGER DEFAULT 0, "
                    + "returned     INTEGER DEFAULT 0, "
                    + "FOREIGN KEY (user_id)      REFERENCES users(user_id), "
                    + "FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id))");

            st.execute("CREATE TABLE IF NOT EXISTS bills ("
                    + "bill_id     TEXT PRIMARY KEY, "
                    + "rental_id   TEXT NOT NULL, "
                    + "quantity    INTEGER NOT NULL DEFAULT 1, "
                    + "base_fee    REAL NOT NULL, "
                    + "discount    REAL NOT NULL, "
                    + "penalty     REAL NOT NULL, "
                    + "net_payable REAL NOT NULL, "
                    + "created_at  TEXT NOT NULL, "
                    + "FOREIGN KEY (rental_id) REFERENCES rentals(rental_id))");
        }
    }

    // ---- seed: only when empty (idempotent). Data from the data dictionary (Part 3) ----
    private static void seedIfEmpty() throws SQLException {
        if (!isEmpty("equipment")) {
            return;
        }
        seedEquipment();
        seedUsers();
    }

    private static boolean isEmpty(String table) throws SQLException {
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() && rs.getInt(1) == 0;
        }
    }

    private static void seedEquipment() throws SQLException {
        // id, name, category, daily_rate, replacement_value, pricing_policy, total_qty, available_qty
        Object[][] rows = {
                {"E001", "Laptop", "ELECTRONICS", 15.00, 2500.00, "STANDARD", 5, 5},
                {"E002", "Tablet", "ELECTRONICS", 10.00, 1500.00, "PROMOTIONAL", 3, 3},
                {"M001", "DSLR Camera", "MEDIA", 30.00, 3500.00, "STANDARD", 2, 2},
                {"M002", "Projector", "MEDIA", 25.00, 2000.00, "STANDARD", 2, 0},
                {"M003", "Microphone", "MEDIA", 10.00, 400.00, "STANDARD", 4, 4},
                {"M004", "Tripod", "MEDIA", 5.00, 200.00, "STANDARD", 6, 6},
                {"L001", "Microscope", "LAB", 40.00, 5000.00, "STANDARD", 2, 2},
                {"L002", "Oscilloscope", "LAB", 50.00, 8000.00, "STANDARD", 1, 1},
        };
        String sql = "INSERT INTO equipment "
                + "(equipment_id, name, category, daily_rate, replacement_value, pricing_policy, "
                + "total_quantity, available_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setDouble(4, (Double) r[3]);
                ps.setDouble(5, (Double) r[4]);
                ps.setString(6, (String) r[5]);
                ps.setInt(7, (Integer) r[6]);
                ps.setInt(8, (Integer) r[7]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private static void seedUsers() throws SQLException {
        // user_id, username, password, name, role, final_year (1 = final-year student → 10% discount)
        Object[][] rows = {
                {"U001", "admin", "admin", "Facilities Admin", "ADMIN", 0},
                {"U002", "staff", "staff", "Dr. Tan", "STAFF", 0},
                {"U003", "student", "student", "Regular Student", "STUDENT", 0},
                {"U004", "finalyear", "finalyear", "Final-Year Student", "STUDENT", 1},
        };
        String sql = "INSERT INTO users (user_id, username, password, name, role, final_year) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (Object[] r : rows) {
                ps.setString(1, (String) r[0]);
                ps.setString(2, (String) r[1]);
                ps.setString(3, (String) r[2]);
                ps.setString(4, (String) r[3]);
                ps.setString(5, (String) r[4]);
                ps.setInt(6, (Integer) r[5]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /**
     * Manual verification entry point (needs the jar on the classpath):
     * <pre>java -cp "bin;lib/sqlite-jdbc.jar" merit.dao.Database</pre>
     * Prints the row counts so you can confirm the seed ran and is idempotent
     * (run it twice — the counts must stay 8 / 4).
     */
    public static void main(String[] args) throws SQLException {
        Connection c = getConnection();
        try (Statement st = c.createStatement()) {
            print(st, "users");
            print(st, "equipment");
            System.out.println("PROMOTIONAL items:");
            try (ResultSet rs = st.executeQuery(
                    "SELECT equipment_id, name FROM equipment WHERE pricing_policy='PROMOTIONAL'")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1) + " " + rs.getString(2));
                }
            }
            System.out.println("Out-of-stock items (available_quantity = 0):");
            try (ResultSet rs = st.executeQuery(
                    "SELECT equipment_id, name FROM equipment WHERE available_quantity = 0")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString(1) + " " + rs.getString(2));
                }
            }
        }
    }

    private static void print(Statement st, String table) throws SQLException {
        try (ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) {
                System.out.println(table + " rows = " + rs.getInt(1));
            }
        }
    }
}
