package merit.pricing;

/**
 * Default pricing: {@code dailyRate * days}.
 *
 * <p>Adding a new pricing rule (e.g. weekend or bulk pricing) means writing a
 * new {@code PricingPolicy} class — no existing class is edited (Open/Closed).
 */
public class StandardPricing implements PricingPolicy {

    @Override
    public double calculateCharge(double dailyRate, int days) {
        return dailyRate * days;
    }
}
