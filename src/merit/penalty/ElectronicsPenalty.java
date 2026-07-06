package merit.penalty;

/**
 * Default penalty for Electronics equipment. Carries a higher damage fee
 * ({@code 0.4×} replacement value, +10% vs standard) reflecting repair/replacement cost.
 * Late fee: {@code 0.5 × dailyRate × daysLate}. Damage fee: {@code 0.4 × replacementValue}.
 */
public class ElectronicsPenalty implements PenaltyPolicy {

    @Override
    public double calculate(double dailyRate, double replacementValue, int daysLate, boolean damaged) {
        double late = 0.5 * dailyRate * daysLate;
        double damage = damaged ? 0.4 * replacementValue : 0.0;
        return late + damage;
    }
}
