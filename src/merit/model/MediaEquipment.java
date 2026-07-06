package merit.model;

import merit.penalty.StandardPenalty;
import merit.pricing.PricingPolicy;

/** Media equipment (cameras, projectors, mics, tripods). Uses {@link StandardPenalty} by default. */
public class MediaEquipment extends Equipment {

    public MediaEquipment(String id, String name, double dailyRate, double replacementValue,
                          PricingPolicy pricingPolicy, int totalQuantity, int availableQuantity) {
        super(id, name, dailyRate, replacementValue, pricingPolicy, new StandardPenalty(),
                totalQuantity, availableQuantity);
    }

    /** Single-unit convenience (used by pure-logic tests): total 1, available 1 or 0. */
    public MediaEquipment(String id, String name, double dailyRate, double replacementValue,
                          boolean available, PricingPolicy pricingPolicy) {
        this(id, name, dailyRate, replacementValue, pricingPolicy, 1, available ? 1 : 0);
    }

    @Override
    public Category getCategory() {
        return Category.MEDIA;
    }
}
