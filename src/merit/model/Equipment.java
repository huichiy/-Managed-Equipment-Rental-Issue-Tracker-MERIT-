package merit.model;

import merit.penalty.PenaltyPolicy;
import merit.pricing.PricingPolicy;

/**
 * Abstract rentable equipment. Each row is a <b>type</b> of item held in stock:
 * {@code totalQuantity} units exist and {@code availableQuantity} are currently free
 * to rent. {@link #isAvailable()} is derived ({@code availableQuantity > 0}).
 *
 * <p>Two policy axes are wired in here:
 * <ul>
 *   <li><b>Bridge</b> — {@link PricingPolicy} is injected via the constructor and is
 *       independent of category (any category can be Standard or Promotional). It is
 *       persisted in the {@code pricing_policy} column.</li>
 *   <li><b>Strategy</b> — {@link PenaltyPolicy} is fixed per subclass (category default),
 *       so it is not persisted.</li>
 * </ul>
 *
 * <p>Billing logic never branches on category; the only {@code switch(category)} in the
 * whole system lives in {@link EquipmentFactory}.
 */
public abstract class Equipment {

    private final String id;
    private final String name;
    private final double dailyRate;
    private final double replacementValue;
    private final PricingPolicy pricingPolicy;   // Bridge: independent axis
    private final PenaltyPolicy penaltyPolicy;   // Strategy: category default
    private int totalQuantity;
    private int availableQuantity;

    protected Equipment(String id, String name, double dailyRate, double replacementValue,
                        PricingPolicy pricingPolicy, PenaltyPolicy penaltyPolicy,
                        int totalQuantity, int availableQuantity) {
        this.id = id;
        this.name = name;
        this.dailyRate = dailyRate;
        this.replacementValue = replacementValue;
        this.pricingPolicy = pricingPolicy;
        this.penaltyPolicy = penaltyPolicy;
        this.totalQuantity = totalQuantity;
        this.availableQuantity = availableQuantity;
    }

    /** Base rental charge via the injected pricing policy (Bridge). */
    public double calculateRentalCharge(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Rental days must be positive, got: " + days);
        }
        return pricingPolicy.calculate(dailyRate, days);
    }

    /** Penalty via the category's penalty strategy. Non-positive lateness counts as 0. */
    public double calculatePenalty(int daysLate, boolean damaged) {
        int effectiveDaysLate = Math.max(0, daysLate);
        return penaltyPolicy.calculate(dailyRate, replacementValue, effectiveDaysLate, damaged);
    }

    public abstract Category getCategory();

    public String getId() { return id; }
    public String getName() { return name; }
    public double getDailyRate() { return dailyRate; }
    public double getReplacementValue() { return replacementValue; }

    public int getTotalQuantity() { return totalQuantity; }
    public int getAvailableQuantity() { return availableQuantity; }
    /** True while at least one unit is free to rent. */
    public boolean isAvailable() { return availableQuantity > 0; }

    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }

    /** Exposed so the DAO can persist {@code pricingPolicy.code()} back to the row. */
    public PricingPolicy getPricingPolicy() { return pricingPolicy; }
}
