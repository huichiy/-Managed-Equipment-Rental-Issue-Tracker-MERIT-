package merit.pricing;

/**
 * Promotional pricing: a flat 20% off the standard charge
 * ({@code dailyRate * days * 0.8}).
 *
 * <p>Assigned per item via the {@code pricing_policy} column, independently of
 * category — this is what proves the Bridge's two axes are truly independent.
 */
public class PromotionalPricing implements PricingPolicy {

    /** Promotional multiplier: 20% discount on the base charge. */
    private static final double PROMO_MULTIPLIER = 0.8;

    @Override
    public double calculateCharge(double dailyRate, int days) {
        return dailyRate * days * PROMO_MULTIPLIER;
    }
}
