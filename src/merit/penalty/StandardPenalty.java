package merit.penalty;

/**
 * Default penalty for Media equipment.
 * Late fee: {@code 0.5 × dailyRate × daysLate}. Damage fee: {@code 0.3 × replacementValue}.
 */
public class StandardPenalty implements PenaltyPolicy {

    @Override
    public double calculate(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double late = 0.5 * dailyRate * daysLate;
        double damage = damaged ? 0.3 * replacementValue : 0.0;
        return late + damage;
    }
}
