package model;

import bridge.PricingStrategy;

public class MediaEquipment extends Equipment {
    public MediaEquipment(String id, String name, double dailyRate,
                          double replacementCost, PricingStrategy pricing) {
        super(id, name, dailyRate, replacementCost, pricing);
    }
}
