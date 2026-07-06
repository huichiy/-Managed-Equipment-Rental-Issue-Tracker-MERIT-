package merit.service;

import merit.dao.UserDao;
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
}
