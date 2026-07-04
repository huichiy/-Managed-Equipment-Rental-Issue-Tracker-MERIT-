package merit.model;

import merit.penalty.ElectronicsPenalty;
import merit.pricing.PricingPolicy;

/** Electronics equipment (laptops, tablets). Uses {@link ElectronicsPenalty} by default. */
public class ElectronicsEquipment extends Equipment {

    public ElectronicsEquipment(String id, String name, double dailyRate, double replacementValue,
                                PricingPolicy pricingPolicy, int totalQuantity, int availableQuantity) {
        super(id, name, dailyRate, replacementValue, pricingPolicy, new ElectronicsPenalty(),
                totalQuantity, availableQuantity);
    }

    /** Single-unit convenience (used by pure-logic tests): total 1, available 1 or 0. */
    public ElectronicsEquipment(String id, String name, double dailyRate, double replacementValue,
                                boolean available, PricingPolicy pricingPolicy) {
        this(id, name, dailyRate, replacementValue, pricingPolicy, 1, available ? 1 : 0);
    }

    @Override
    public Category getCategory() {
        return Category.ELECTRONICS;
    }
}
