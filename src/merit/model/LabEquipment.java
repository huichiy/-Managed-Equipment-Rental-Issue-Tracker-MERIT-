package merit.model;

import merit.penalty.LabPenalty;
import merit.pricing.PricingPolicy;

/**
 * Laboratory equipment (microscopes, oscilloscopes). Defaults to
 * {@link LabPenalty} (full-rate late fee, safety-critical); pricing is injected.
 */
public class LabEquipment extends Equipment {

    public LabEquipment(String id, String name, double dailyRate, double replacementValue,
                        boolean available, PricingPolicy pricingPolicy) {
        super(id, name, dailyRate, replacementValue, available, pricingPolicy, new LabPenalty());
    }

    @Override
    public Category getCategory() {
        return Category.LAB;
    }
}
