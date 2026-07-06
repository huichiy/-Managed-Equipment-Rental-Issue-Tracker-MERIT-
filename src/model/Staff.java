package model;

public class Staff extends User {
    public Staff(String id, String name) {
        super(id, name);
    }

    @Override
    public double getDiscountRate() {
        return 0.20;
    }
}