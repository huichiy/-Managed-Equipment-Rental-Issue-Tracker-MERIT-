package merit.model;

/** Academic / administrative staff — flat 20% discount on the base rental fee. */
public class Staff extends User {

    public Staff(String id, String username, String password, String name) {
        super(id, username, password, name);
    }

    @Override
    public double getDiscountRate() {
        return 0.20;
    }
}
