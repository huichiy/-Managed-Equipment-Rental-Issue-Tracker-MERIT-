package merit.model;

import java.time.LocalDate;

/**
 * A rental links a {@link User} to a piece of {@link Equipment} for a number of days.
 *
 * <p><b>Aggregation:</b> the rental holds a User and an Equipment, but both outlive the
 * rental (hollow diamond) — contrast {@link Bill}'s composition of its amounts.
 *
 * <p>The frozen getters {@code getId / getUser / getEquipment / getRentalDays /
 * getDaysLate / isDamaged} and the 4-arg constructor are the Phase-0 contract that
 * {@code BillGenerator} binds to — kept stable. Member B added the date/return fields.
 */
public class Rental {

    private final String id;
    private final User user;
    private final Equipment equipment;
    private final int rentalDays;
    private final int quantity;         // number of units rented in this rental (>= 1)
    private final LocalDate rentDate;   // null only in the minimal (date-less) test usage
    private final LocalDate dueDate;    // derived: rentDate + rentalDays
    private LocalDate returnDate;       // null until returned
    private int daysLate;
    private boolean damaged;
    private boolean returned;

    /** Minimal constructor kept for {@code BillGenerator}'s pure-logic test (no dates, qty 1). */
    public Rental(String id, User user, Equipment equipment, int rentalDays) {
        this(id, user, equipment, rentalDays, null, 1);
    }

    /** Single-unit rent-time constructor (qty 1). */
    public Rental(String id, User user, Equipment equipment, int rentalDays, LocalDate rentDate) {
        this(id, user, equipment, rentalDays, rentDate, 1);
    }

    /** Rent-time constructor: computes {@code dueDate = rentDate + rentalDays}. */
    public Rental(String id, User user, Equipment equipment, int rentalDays, LocalDate rentDate, int quantity) {
        this.id = id;
        this.user = user;
        this.equipment = equipment;
        this.rentalDays = rentalDays;
        this.quantity = quantity;
        this.rentDate = rentDate;
        this.dueDate = (rentDate == null) ? null : rentDate.plusDays(rentalDays);
    }

    public String getId() { return id; }
    public User getUser() { return user; }
    public Equipment getEquipment() { return equipment; }
    public int getRentalDays() { return rentalDays; }
    public int getQuantity() { return quantity; }
    public int getDaysLate() { return daysLate; }
    public boolean isDamaged() { return damaged; }

    public LocalDate getRentDate() { return rentDate; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getReturnDate() { return returnDate; }
    public boolean isReturned() { return returned; }

    public void setDaysLate(int daysLate) { this.daysLate = daysLate; }
    public void setDamaged(boolean damaged) { this.damaged = damaged; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }
    public void setReturned(boolean returned) { this.returned = returned; }
}
