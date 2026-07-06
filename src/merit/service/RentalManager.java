package merit.service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import merit.dao.EquipmentDao;
import merit.dao.RentalDao;
import merit.model.Equipment;
import merit.model.Rental;
import merit.model.User;

/**
 * Orchestrates the rent / return flow and keeps equipment availability in sync.
 *
 * <p>Renting flips the item to unavailable; returning flips it back and computes the
 * late days. Duration is validated (1..{@value #MAX_RENTAL_DAYS}); the 30-day cap is a
 * validation limit only — there is no renew feature.
 */
public class RentalManager {

    public static final int MAX_RENTAL_DAYS = 30;

    private final RentalDao rentalDao;
    private final EquipmentDao equipmentDao;

    public RentalManager(RentalDao rentalDao, EquipmentDao equipmentDao) {
        this.rentalDao = rentalDao;
        this.equipmentDao = equipmentDao;
    }

    /** Rents {@code quantity} units today. */
    public Rental rent(String rentalId, User user, Equipment equipment, int days, int quantity) {
        return rent(rentalId, user, equipment, days, quantity, LocalDate.now());
    }

    public Rental rent(String rentalId, User user, Equipment equipment, int days, int quantity,
                       LocalDate rentDate) {
        if (days <= 0 || days > MAX_RENTAL_DAYS) {
            throw new IllegalArgumentException(
                    "Rental days must be 1.." + MAX_RENTAL_DAYS + ", got: " + days);
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive, got: " + quantity);
        }
        if (quantity > equipment.getAvailableQuantity()) {
            throw new IllegalStateException("Only " + equipment.getAvailableQuantity()
                    + " unit(s) available for " + equipment.getId() + ", requested " + quantity);
        }
        Rental rental = new Rental(rentalId, user, equipment, days, rentDate, quantity);
        rentalDao.insert(rental);
        equipment.setAvailableQuantity(equipment.getAvailableQuantity() - quantity);   // units taken
        equipmentDao.updateQuantities(equipment.getId(), equipment.getTotalQuantity(),
                equipment.getAvailableQuantity());
        return rental;
    }

    public void returnEquipment(Rental rental, LocalDate returnDate, boolean damaged) {
        long late = ChronoUnit.DAYS.between(rental.getDueDate(), returnDate);
        rental.setDaysLate((int) Math.max(0, late));   // early returns are not negative
        rental.setDamaged(damaged);
        rental.setReturnDate(returnDate);
        rental.setReturned(true);
        rentalDao.update(rental);

        Equipment equipment = rental.getEquipment();
        // give the rented units back, but never exceed total stock
        int restored = Math.min(equipment.getTotalQuantity(),
                equipment.getAvailableQuantity() + rental.getQuantity());
        equipment.setAvailableQuantity(restored);
        equipmentDao.updateQuantities(equipment.getId(), equipment.getTotalQuantity(), restored);
    }
}
