package merit.model;

import merit.penalty.StandardPenalty;
import merit.pricing.PricingPolicy;

/**
 * Media equipment (cameras, projectors, microphones, tripods). Defaults to
 * {@link StandardPenalty}; pricing is injected (Standard or Promotional).
 */
public class MediaEquipment extends Equipment {

    public MediaEquipment(String id, String name, double dailyRate, double replacementValue,
                          boolean available, PricingPolicy pricingPolicy) {
        super(id, name, dailyRate, replacementValue, available, pricingPolicy, new StandardPenalty());
    }

    @Override
    public Category getCategory() {
        return Category.MEDIA;
    }
}
