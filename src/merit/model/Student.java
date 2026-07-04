package merit.model;

/**
 * Enrolled student. Final-year students get 10%; regular students get 0%.
 * The rate is chosen polymorphically here — {@code BillGenerator} never checks the role.
 */
public class Student extends User {

    private final boolean finalYear;

    public Student(String id, String username, String password, String name, boolean finalYear) {
        super(id, username, password, name);
        this.finalYear = finalYear;
    }

    public boolean isFinalYear() {
        return finalYear;
    }

    @Override
    public double getDiscountRate() {
        return finalYear ? 0.10 : 0.0;
    }
}
