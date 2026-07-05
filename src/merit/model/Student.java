package merit.model;

/**
 * Enrolled student. A <b>final-year</b> student gets a 10% rental discount; a regular
 * student gets none. The rate is chosen polymorphically via {@link #getDiscountRate()}
 * — {@code BillGenerator} never checks the role or the flag.
 */
public class Student extends User {

    private final boolean finalYear;

    /** Regular (non-final-year) student — 0% discount. */
    public Student(String id, String username, String password, String name) {
        this(id, username, password, name, false);
    }

    /** {@code finalYear == true} marks a final-year student (10% discount). */
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
