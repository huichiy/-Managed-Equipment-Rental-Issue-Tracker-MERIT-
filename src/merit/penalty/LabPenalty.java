package merit.penalty;

/**
 * Penalty rule for Laboratory equipment — a heavier late fee because lab
 * instruments are usually booked on a schedule, so lateness blocks other users.
 *
 * <ul>
 *   <li>Late fee: {@code 1.0 * dailyRate * daysLate} (full daily rate, safety-critical)</li>
 *   <li>Damage fee: {@code 0.3 * replacementValue}</li>
 * </ul>
 */
public class LabPenalty implements PenaltyPolicy {

    private static final double LATE_RATE = 1.0;
    private static final double DAMAGE_RATE = 0.3;

    @Override
    public double calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double lateFee = LATE_RATE * dailyRate * daysLate;
        double damageFee = damaged ? DAMAGE_RATE * replacementValue : 0.0;
        return lateFee + damageFee;
    }
}
