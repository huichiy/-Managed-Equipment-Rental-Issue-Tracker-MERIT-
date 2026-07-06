package merit.pricing;

/** Standard pricing: {@code dailyRate × days}. */
public class StandardPricing implements PricingPolicy {

    @Override
    public double calculate(double dailyRate, int days) {
        return dailyRate * days;
    }

    @Override
    public String code() {
        return "STANDARD";
    }
}
