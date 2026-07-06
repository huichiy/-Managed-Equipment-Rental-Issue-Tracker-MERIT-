package merit.pricing;

/** Promotional pricing: {@code dailyRate × days × 0.8} (20% off). */
public class PromotionalPricing implements PricingPolicy {

    private static final double PROMO_MULTIPLIER = 0.8;

    @Override
    public double calculate(double dailyRate, int days) {
        return dailyRate * days * PROMO_MULTIPLIER;
    }

    @Override
    public String code() {
        return "PROMOTIONAL";
    }
}
