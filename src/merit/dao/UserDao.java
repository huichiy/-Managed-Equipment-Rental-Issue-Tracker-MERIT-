package merit.dao;

import java.util.List;

import merit.model.User;

/**
 * Phase-0 frozen contract; Member B owns the SQLite implementation.
 * Accounts are seeded by {@code Database}; new ones are added at runtime via
 * self-registration ({@link #insert(User)}).
 */
public interface UserDao {

    /** @return the user, or {@code null} if no account matches (used by login). */
    User findByUsername(String username);

    User findById(String id);

    List<User> findAll();

    /**
     * Persist a newly registered account. The {@code user_id} primary key is
     * assigned by the DAO; {@code role} and {@code final_year} are derived from the
     * concrete {@link User} subtype. Callers must ensure the username is free first
     * (see {@link merit.service.AuthService#register}).
     */
    void insert(User user);
}
