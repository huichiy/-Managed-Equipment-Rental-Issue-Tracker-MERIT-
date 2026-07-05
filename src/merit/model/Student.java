package merit.model;

/**
 * Enrolled student — no rental discount (0%).
 * The rate is chosen polymorphically here — {@code BillGenerator} never checks the role.
 */
public class Student extends User {

    public Student(String id, String username, String password, String name) {
        super(id, username, password, name);
    }

    @Override
    public double getDiscountRate() {
        return 0.0;
    }
}
