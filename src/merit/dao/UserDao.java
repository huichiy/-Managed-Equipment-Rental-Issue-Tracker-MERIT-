package merit.dao;

import java.util.List;

import merit.model.User;

/**
 * Phase-0 frozen contract; Member B owns the SQLite implementation.
 * Read-only — user accounts are seeded by {@code Database}, not created at runtime.
 */
public interface UserDao {

    /** @return the user, or {@code null} if no account matches (used by login). */
    User findByUsername(String username);

    User findById(String id);

    List<User> findAll();
}
