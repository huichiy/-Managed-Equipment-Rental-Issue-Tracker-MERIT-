package model;

public class Rental {
    private User user;
    private Equipment equipment;
    private int days;
    private int daysLate;
    private String damageLevel;

    public Rental(User user, Equipment equipment, int days,
                   int daysLate, String damageLevel) {
        this.user = user;
        this.equipment = equipment;
        this.days = days;
        this.daysLate = daysLate;
        this.damageLevel = damageLevel;
    }

    public User getUser() { return user; }
    public Equipment getEquipment() { return equipment; }
    public int getDays() { return days; }
    public int getDaysLate() { return daysLate; }
    public String getDamageLevel() { return damageLevel; }
}