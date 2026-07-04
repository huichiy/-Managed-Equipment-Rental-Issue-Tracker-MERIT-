package merit.pricing;

/**
 * Bridge — the independent pricing axis. Any {@code Equipment} category can be
 * configured with any pricing policy; the choice is persisted in the
 * {@code pricing_policy} column.
 */
public interface PricingPolicy {

    /** Base rental charge for the given daily rate over the given number of days. */
    double calculate(double dailyRate, int days);

    /**
     * Persistence code (e.g. {@code "STANDARD"} / {@code "PROMOTIONAL"}) stored in
     * the {@code pricing_policy} column. Lets the DAO save the policy back without
     * an {@code instanceof} check.
     */
    String code();
}
