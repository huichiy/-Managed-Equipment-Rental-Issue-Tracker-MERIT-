package model;

import bridge.PricingStrategy;

    public abstract class Equipment {
    protected String id;
    protected String name;
    protected double dailyRate;
    protected double replacementCost;
    protected PricingStrategy pricing; 
    
    public Equipment(String id, String name, double dailyRate,
                  double replacementCost, PricingStrategy pricing) {
    this.id = id;
    this.name = name;
    this.dailyRate = dailyRate;
    this.replacementCost = replacementCost;
    this.pricing = pricing;
    }
    
    public double calculateBaseFee(int days) {
        return pricing.baseFee(dailyRate, days);
    }
    public double calculateLateFee(int daysLate) {
        return pricing.lateFee(dailyRate, daysLate);
    }

    public double calculateDamageFee(String level) {
        return pricing.damageFee(level, replacementCost);
    }

    @Override
    public String toString() {
        return name + " (RM" + dailyRate + "/day)";
    }
}