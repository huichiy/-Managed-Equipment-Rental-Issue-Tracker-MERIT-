package merit.penalty;

/**
 * Strategy — the penalty rule applied on return. Selected per category (each
 * {@code Equipment} subclass wires its own default), so it is NOT persisted: there
 * is no {@code penalty_policy} column.
 */
public interface PenaltyPolicy {

    /**
     * Total penalty = late fee (based on {@code daysLate}) + damage fee (only if
     * {@code damaged}).
     */
    double calculate(double dailyRate, double replacementValue, int daysLate, boolean damaged);
}
