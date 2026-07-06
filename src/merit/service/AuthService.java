package merit.service;

import merit.dao.UserDao;
import merit.model.Staff;
import merit.model.Student;
import merit.model.User;

/**
 * Authenticates a login and returns the typed {@link User} for <b>role routing</b>
 * (Admin → admin panel; Staff/Student → rental flow). This is access control, not a
 * security feature — passwords are compared in plaintext (academic scope).
 */
public class AuthService {

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /** @return the authenticated user, or {@code null} if the credentials do not match. */
    public User login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    /**
     * Register a new Staff or Student account. Admins are seeded, never self-registered.
     * Builds the correct {@link User} subtype so the DAO can derive {@code role} and
     * {@code final_year} polymorphically (the {@code user_id} is assigned on insert).
     *
     * @param role {@code "STAFF"} or {@code "STUDENT"}
     * @return the created user, or {@code null} if the username is already taken.
     */
    public User register(String username, String password, String name, String role, boolean finalYear) {
        if (userDao.findByUsername(username) != null) {
            return null;
        }
        User user = switch (role) {
            case "STAFF"   -> new Staff("", username, password, name);
            case "STUDENT" -> new Student("", username, password, name, finalYear);
            default -> throw new IllegalArgumentException("Cannot self-register role: " + role);
        };
        userDao.insert(user);
        return user;
    }
}
