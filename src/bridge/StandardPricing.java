package bridge;

public class StandardPricing implements PricingStrategy {
    @Override
    public double baseFee(double dailyRate, int days) {
        return dailyRate * days ; 
    }

    @Override
    public double lateFee(double dailyRate, int daysLate) {
        return 10 * daysLate; 
    }

    @Override
    public double damageFee(String level, double replacementCost) {
        switch (level) {
            case "MINOR":
                return 50;
            case "MAJOR":
                return replacementCost * 0.5;
            case "TOTAL":
                return replacementCost;
            default:
                return 0;
        }
    }
}
