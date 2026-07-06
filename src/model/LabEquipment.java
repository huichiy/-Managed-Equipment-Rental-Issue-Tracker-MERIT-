package model;

import bridge.PricingStrategy;

public class LabEquipment extends Equipment {
    public LabEquipment(String id, String name, double dailyRate,
                        double replacementCost, PricingStrategy pricing) {
        super(id, name, dailyRate, replacementCost, pricing);
    }
}