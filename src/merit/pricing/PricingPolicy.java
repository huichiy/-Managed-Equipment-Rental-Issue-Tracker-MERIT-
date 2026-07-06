package merit.pricing;

/**
 * Bridge <b>implementor</b> for how a rental's base charge is calculated.
 *
 * <p>This is the genuinely independent axis of the Bridge: any equipment
 * category may be assigned any pricing policy, so a promotional laptop and a
 * promotional microscope reuse the exact same {@link PromotionalPricing} object.
 * {@link merit.model.Equipment} holds a {@code PricingPolicy} by composition and
 * delegates {@code calculateRentalCharge(days)} to it — callers depend on this
 * interface, never on a concrete pricing class (program-to-an-interface).
 *
 * <p>Frozen integration contract (Phase 0): shared with Member C's billing.
 */
public interface PricingPolicy {

    /**
     * @param dailyRate the equipment's daily rental rate (RM), assumed &gt;= 0
     * @param days      the rental duration in days, assumed &gt; 0
     * @return the base rental charge before discounts and penalties
     */
    double calculateCharge(double dailyRate, int days);
}
