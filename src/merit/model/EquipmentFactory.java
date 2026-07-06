package merit.model;

import merit.pricing.PricingPolicy;

/**
 * The single place in the system that maps a {@link Category} to its concrete
 * {@link Equipment} subclass (and thereby its default penalty policy). Reused by both
 * DAO reconstruction (flat row → object) and the admin "add equipment" flow, so the
 * {@code switch(category)} lives in exactly one location. No business/service/UI code
 * branches on category — they call this factory instead.
 */
public final class EquipmentFactory {

    private EquipmentFactory() {
    }

    public static Equipment create(Category category, String id, String name, double dailyRate,
                                   double replacementValue, PricingPolicy pricing,
                                   int totalQuantity, int availableQuantity) {
        return switch (category) {
            case ELECTRONICS -> new ElectronicsEquipment(id, name, dailyRate, replacementValue,
                    pricing, totalQuantity, availableQuantity);
            case MEDIA -> new MediaEquipment(id, name, dailyRate, replacementValue,
                    pricing, totalQuantity, availableQuantity);
            case LAB -> new LabEquipment(id, name, dailyRate, replacementValue,
                    pricing, totalQuantity, availableQuantity);
        };
    }
}
