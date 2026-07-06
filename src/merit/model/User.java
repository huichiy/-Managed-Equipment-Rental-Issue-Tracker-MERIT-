package merit.model;

/**
 * Phase-0 contract skeleton — <b>Member B owns the full implementation.</b>
 *
 * <p>Created by Member C so {@code BillGenerator} can compile and be tested against
 * a frozen contract. {@code BillGenerator} depends only on {@link #getDiscountRate()}
 * (the polymorphic discount) and the identity getters.
 *
 * <p>Member B will add the {@code Admin} / {@code Staff} / {@code Student} subclasses
 * (each returning {@code 0 / 0.20 / 0}) and the {@code UserDao}
 * mapping. <b>Do not change {@link #getDiscountRate()}'s signature</b> — C's billing
 * depends on it.
 */
public abstract class User {

    private final String id;
    private final String username;
    private final String password;
    private final String name;

    protected User(String id, String username, String password, String name) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.name = name;
    }

    /** Polymorphic discount rate in [0.0, 1.0], applied to the base fee only. */
    public abstract double getDiscountRate();

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getName() { return name; }
}
