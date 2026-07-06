package merit.model;

import merit.penalty.LabPenalty;
import merit.pricing.PricingPolicy;

/** Lab equipment (microscopes, oscilloscopes). Uses {@link LabPenalty} by default. */
public class LabEquipment extends Equipment {

    public LabEquipment(String id, String name, double dailyRate, double replacementValue,
                        PricingPolicy pricingPolicy, int totalQuantity, int availableQuantity) {
        super(id, name, dailyRate, replacementValue, pricingPolicy, new LabPenalty(),
                totalQuantity, availableQuantity);
    }

    /** Single-unit convenience (used by pure-logic tests): total 1, available 1 or 0. */
    public LabEquipment(String id, String name, double dailyRate, double replacementValue,
                        boolean available, PricingPolicy pricingPolicy) {
        this(id, name, dailyRate, replacementValue, pricingPolicy, 1, available ? 1 : 0);
    }

    @Override
    public Category getCategory() {
        return Category.LAB;
    }
}
