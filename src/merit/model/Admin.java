package merit.model;

/**
 * Facilities operator. Manages the catalog and processes returns; does not rent,
 * so the discount is not applicable ({@code 0}). Login routes an Admin to the admin panel.
 */
public class Admin extends User {

    public Admin(String id, String username, String password, String name) {
        super(id, username, password, name);
    }

    @Override
    public double getDiscountRate() {
        return 0.0;
    }
}
