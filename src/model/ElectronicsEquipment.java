package model;

import bridge.PricingStrategy;

public class ElectronicsEquipment extends Equipment {
    public ElectronicsEquipment(String id, String name, double dailyRate,
                                 double replacementCost, PricingStrategy pricing) {
        super(id, name, dailyRate, replacementCost, pricing);
    }
}