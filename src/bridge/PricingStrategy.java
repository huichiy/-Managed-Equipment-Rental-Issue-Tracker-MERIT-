package bridge;


public interface PricingStrategy {
    double baseFee(double dailyRate, int days);
    double lateFee(double dailyRate, int daysLate);
    double damageFee(String level, double replacementCost);
}