package merit.model;

import merit.penalty.PenaltyPolicy;
import merit.pricing.PricingPolicy;

/**
 * Abstract base of the equipment hierarchy and the <b>abstraction</b> side of the
 * Bridge pattern.
 *
 * <p><b>Bridge:</b> an {@code Equipment} <i>has-a</i> {@link PricingPolicy} and a
 * {@link PenaltyPolicy} (composition/delegation), rather than baking pricing and
 * penalty logic into subclasses. This decouples the "category" axis (the subclass
 * hierarchy below) from the "rules" axis (the policy interfaces), so each can be
 * extended independently:
 * <ul>
 *   <li>new category  → add an {@code Equipment} subclass (policies untouched)</li>
 *   <li>new rule      → add a {@code PricingPolicy}/{@code PenaltyPolicy} (equipment untouched)</li>
 * </ul>
 *
 * <p><b>OO pillars shown here:</b> abstraction (cannot instantiate a raw
 * {@code Equipment}), inheritance (subclasses share this state/behaviour),
 * encapsulation (private fields, constructor validation), and polymorphism
 * ({@link #calculateRentalCharge}/{@link #calculatePenalty} run the assigned
 * policy chosen at runtime; {@link #getCategory()} is overridden per subclass).
 */
public abstract class Equipment {

    private final String id;
    private final String name;
    private final double dailyRate;
    private final double replacementValue;
    private boolean available;

    // The two Bridge implementors this equipment delegates to.
    private final PricingPolicy pricingPolicy;
    private final PenaltyPolicy penaltyPolicy;

    /**
     * @throws IllegalArgumentException if a rate/value is negative or a policy is null
     */
    protected Equipment(String id, String name, double dailyRate, double replacementValue,
                        boolean available, PricingPolicy pricingPolicy, PenaltyPolicy penaltyPolicy) {
        if (dailyRate < 0) {
            throw new IllegalArgumentException("dailyRate must be >= 0");
        }
        if (replacementValue < 0) {
            throw new IllegalArgumentException("replacementValue must be >= 0");
        }
        if (pricingPolicy == null || penaltyPolicy == null) {
            throw new IllegalArgumentException("pricing and penalty policies must not be null");
        }
        this.id = id;
        this.name = name;
        this.dailyRate = dailyRate;
        this.replacementValue = replacementValue;
        this.available = available;
        this.pricingPolicy = pricingPolicy;
        this.penaltyPolicy = penaltyPolicy;
    }

    /**
     * Base rental charge for {@code days} days, delegated to the assigned
     * {@link PricingPolicy} (Bridge in action — no pricing maths lives here).
     *
     * @throws IllegalArgumentException if {@code days <= 0}
     */
    public double calculateRentalCharge(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("days must be > 0");
        }
        return pricingPolicy.calculateCharge(dailyRate, days);
    }

    /**
     * Late/damage penalty, delegated to the assigned {@link PenaltyPolicy}.
     *
     * @throws IllegalArgumentException if {@code daysLate < 0}
     */
    public double calculatePenalty(int daysLate, boolean damaged) {
        if (daysLate < 0) {
            throw new IllegalArgumentException("daysLate must be >= 0");
        }
        return penaltyPolicy.calculatePenalty(dailyRate, replacementValue, daysLate, damaged);
    }

    /** @return the category of this equipment (overridden by each subclass). */
    public abstract Category getCategory();

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getDailyRate() {
        return dailyRate;
    }

    public double getReplacementValue() {
        return replacementValue;
    }

    public boolean isAvailable() {
        return available;
    }

    /** Toggled by rent/return (Member B) via the service layer. */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /** Exposed so the persistence layer can record which pricing policy is assigned. */
    public PricingPolicy getPricingPolicy() {
        return pricingPolicy;
    }

    /** Exposed for completeness/persistence; penalty is category-defaulted in this design. */
    public PenaltyPolicy getPenaltyPolicy() {
        return penaltyPolicy;
    }
}
