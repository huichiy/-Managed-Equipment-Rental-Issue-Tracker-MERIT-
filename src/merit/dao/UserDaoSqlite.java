package merit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import merit.model.Admin;
import merit.model.Staff;
import merit.model.Student;
import merit.model.User;

/**
 * SQLite-backed {@link UserDao}. Takes a {@link Connection} so it never imports
 * {@code Database} — the only coupling is {@code java.sql}.
 *
 * <p>{@link #reconstruct} is the single place in this module that switches on
 * {@code role}: the DAO boundary that rebuilds a typed {@link User} from a flat row
 * (mirrors Member A's {@code category} switch). No business logic branches on role.
 */
public class UserDaoSqlite implements UserDao {

    private final Connection connection;

    public UserDaoSqlite(Connection connection) {
        this.connection = connection;
    }

    @Override
    public User findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username = ?", username);
    }

    @Override
    public User findById(String id) {
        return queryOne("SELECT * FROM users WHERE user_id = ?", id);
    }

    @Override
    public List<User> findAll() {
        List<User> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM users");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(reconstruct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users", e);
        }
        return result;
    }

    @Override
    public void insert(User user) {
        String sql = "INSERT INTO users (user_id, username, password, name, role, final_year) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nextUserId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getName());
            ps.setString(5, roleCode(user));
            ps.setInt(6, (user instanceof Student s && s.isFinalYear()) ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to register user: " + user.getUsername(), e);
        }
    }

    /** Next {@code U###} id, continuing the seeded sequence (U001, U002, ...). */
    private String nextUserId() throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTR(user_id, 2) AS INTEGER)), 0) "
                + "FROM users WHERE user_id LIKE 'U%'";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            int next = rs.next() ? rs.getInt(1) + 1 : 1;
            return String.format("U%03d", next);
        }
    }

    /** Map a {@link User} subtype → its {@code role} column value (the write-side mirror of {@link #reconstruct}). */
    private String roleCode(User user) {
        if (user instanceof Admin) {
            return "ADMIN";
        }
        if (user instanceof Staff) {
            return "STAFF";
        }
        if (user instanceof Student) {
            return "STUDENT";
        }
        throw new IllegalArgumentException("Unknown user type: " + user.getClass().getName());
    }

    private User queryOne(String sql, String param) {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? reconstruct(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load user: " + param, e);
        }
    }

    // ---- reconstruction factory: the ONLY place role is switched on ----
    private User reconstruct(ResultSet rs) throws SQLException {
        String id = rs.getString("user_id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String name = rs.getString("name");
        String role = rs.getString("role");
        return switch (role) {
            case "ADMIN" -> new Admin(id, username, password, name);
            case "STAFF" -> new Staff(id, username, password, name);
            case "STUDENT" -> new Student(id, username, password, name, rs.getInt("final_year") == 1);
            default -> throw new IllegalStateException("Unknown role: " + role);
        };
    }
}
