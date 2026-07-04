package merit.penalty;

/**
 * Default penalty for Lab equipment. Late fee is charged at the full daily rate
 * ({@code 1.0×}) because lab instruments are typically booked on a schedule.
 * Late fee: {@code 1.0 × dailyRate × daysLate}. Damage fee: {@code 0.3 × replacementValue}.
 */
public class LabPenalty implements PenaltyPolicy {

    @Override
    public double calculate(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double late = 1.0 * dailyRate * daysLate;
        double damage = damaged ? 0.3 * replacementValue : 0.0;
        return late + damage;
    }
}
