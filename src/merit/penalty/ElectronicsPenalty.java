package merit.penalty;

/**
 * Penalty rule for Electronics equipment — a higher damage surcharge because
 * electronics are costlier to repair/replace.
 *
 * <ul>
 *   <li>Late fee: {@code 0.5 * dailyRate * daysLate}</li>
 *   <li>Damage fee: {@code 0.4 * replacementValue} (a +10% surcharge over the standard 0.3)</li>
 * </ul>
 */
public class ElectronicsPenalty implements PenaltyPolicy {

    private static final double LATE_RATE = 0.5;
    private static final double DAMAGE_RATE = 0.4;

    @Override
    public double calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double lateFee = LATE_RATE * dailyRate * daysLate;
        double damageFee = damaged ? DAMAGE_RATE * replacementValue : 0.0;
        return lateFee + damageFee;
    }
}
