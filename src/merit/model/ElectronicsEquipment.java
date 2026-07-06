package merit.model;

import merit.penalty.ElectronicsPenalty;
import merit.pricing.PricingPolicy;

/**
 * Electronics (laptops, tablets, ...). Defaults to {@link ElectronicsPenalty}
 * (higher damage surcharge); pricing is injected so it can be Standard or
 * Promotional independently of category.
 */
public class ElectronicsEquipment extends Equipment {

    public ElectronicsEquipment(String id, String name, double dailyRate, double replacementValue,
                                boolean available, PricingPolicy pricingPolicy) {
        super(id, name, dailyRate, replacementValue, available, pricingPolicy, new ElectronicsPenalty());
    }

    @Override
    public Category getCategory() {
        return Category.ELECTRONICS;
    }
}
