package merit.model;

/**
 * The three equipment categories the Facilities department manages.
 *
 * <p>Category is one of the two axes of variation in the {@code Bridge} pattern
 * (the other being {@link merit.pricing.PricingPolicy}). Each value maps to a
 * concrete {@link Equipment} subclass and a default
 * {@link merit.penalty.PenaltyPolicy}. The mapping lives in the DAO
 * ({@link merit.dao.EquipmentDaoSqlite}); business logic never switches on it.
 */
public enum Category {
    ELECTRONICS,
    MEDIA,
    LAB
}
