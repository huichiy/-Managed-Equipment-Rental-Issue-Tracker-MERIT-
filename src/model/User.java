package model;

public abstract class User {
    protected String id;
    protected String name;

    public User(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public abstract double getDiscountRate();
    public String getName() { return name; }
}