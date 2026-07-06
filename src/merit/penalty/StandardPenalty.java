package merit.penalty;

/**
 * Default penalty rule, used by Media equipment.
 *
 * <ul>
 *   <li>Late fee: {@code 0.5 * dailyRate * daysLate}</li>
 *   <li>Damage fee: {@code 0.3 * replacementValue}</li>
 * </ul>
 */
public class StandardPenalty implements PenaltyPolicy {

    private static final double LATE_RATE = 0.5;
    private static final double DAMAGE_RATE = 0.3;

    @Override
    public double calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double lateFee = LATE_RATE * dailyRate * daysLate;
        double damageFee = damaged ? DAMAGE_RATE * replacementValue : 0.0;
        return lateFee + damageFee;
    }
}
