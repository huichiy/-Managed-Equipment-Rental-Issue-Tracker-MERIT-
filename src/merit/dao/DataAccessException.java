package merit.dao;

/**
 * Unchecked wrapper for persistence failures.
 *
 * <p>Keeps the DAO interfaces free of {@code java.sql.SQLException} so the
 * service and UI layers never import JDBC types — that one-way dependency
 * (UI/service → DAO interface, not JDBC) is what keeps the database swappable.
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
