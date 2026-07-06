package merit.penalty;

/**
 * Bridge <b>implementor</b> for how late-return and damage penalties are charged.
 *
 * <p>A second implementor axis alongside {@link merit.pricing.PricingPolicy}.
 * {@link merit.model.Equipment} delegates {@code calculatePenalty(...)} here, so
 * different categories can enforce different penalty rules without any
 * {@code if (category == ...)} branching in the domain or service layers.
 *
 * <p>In the current design each category is constructed with its default penalty
 * policy (see the {@code Equipment} subclasses); because the equipment merely
 * <i>has-a</i> {@code PenaltyPolicy}, swapping to a per-item penalty column later
 * requires no change to this interface or its implementations.
 *
 * <p>Frozen integration contract (Phase 0): shared with Member C's billing.
 */
public interface PenaltyPolicy {

    /**
     * @param dailyRate        the equipment's daily rental rate (RM)
     * @param replacementValue the equipment's replacement value (RM), used for damage
     * @param daysLate         days returned past the due date, assumed &gt;= 0
     * @param damaged          whether the item came back damaged
     * @return the total penalty (late fee + damage fee); 0 if on time and undamaged
     */
    double calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged);
}
