package merit.service;

import java.time.LocalDateTime;

import merit.model.Bill;
import merit.model.Equipment;
import merit.model.Rental;
import merit.model.User;

/**
 * Produces an itemised {@link Bill} from a {@link Rental} (owned by Member C).
 *
 * <p>Billing formula (data dictionary §1.7), scaled by the rented quantity {@code K}:
 * <pre>
 *   base     = equipment.calculateRentalCharge(days) × K   // A's PricingPolicy, per unit × K
 *   discount = base × user.getDiscountRate()               // applied to BASE ONLY
 *   penalty  = equipment.calculatePenalty(daysLate, damaged) × K   // A's PenaltyPolicy, per unit × K
 *   net      = base − discount + penalty
 * </pre>
 *
 * <p>The discount is polymorphic: {@link User#getDiscountRate()} is called with no
 * {@code instanceof} / role switching. The discount never touches the penalty.
 */
public class BillGenerator {

    public Bill generate(Rental rental) {
        Equipment equipment = rental.getEquipment();
        User user = rental.getUser();
        int quantity = rental.getQuantity();

        double base = equipment.calculateRentalCharge(rental.getRentalDays()) * quantity;
        double discount = base * user.getDiscountRate();           // base only
        double penalty = equipment.calculatePenalty(rental.getDaysLate(), rental.isDamaged()) * quantity;
        double net = base - discount + penalty;

        String billId = "B-" + rental.getId();                     // one bill per rental (1-1)
        return new Bill(billId, rental.getId(), quantity, base, discount, penalty, net,
                LocalDateTime.now().toString());
    }
}
