package model;

public class Student extends User {
    private boolean finalYear;

    public Student(String id, String name, boolean finalYear) {
        super(id, name);
        this.finalYear = finalYear;
    }

    @Override
    public double getDiscountRate() {
        return finalYear ? 0.10 : 0.0;
    }
}