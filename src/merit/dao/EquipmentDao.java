package merit.dao;

import java.util.List;
import merit.model.Equipment;

/**
 * Persistence boundary for {@link Equipment} (DAO / Repository pattern).
 *
 * <p>This is <i>architectural</i> layering, not the nominated GoF pattern — the
 * nominated pattern is Bridge. Callers (Admin UI, catalog, rental service) depend
 * only on this interface, so the SQLite implementation can be replaced (e.g. with
 * MySQL) without touching them.
 *
 * <p>Frozen integration contract (Phase 0): the interface is shared; Member A
 * owns the SQLite implementation.
 */
public interface EquipmentDao {

    /** Insert a new equipment item (used by the Admin panel). */
    void add(Equipment equipment);

    /** @return the item with this id, or {@code null} if none exists. */
    Equipment findById(String equipmentId);

    /** @return every equipment item, each rebuilt as its concrete subclass. */
    List<Equipment> findAll();

    /** @return only items currently available to rent (for the catalog). */
    List<Equipment> findAvailable();

    /** Flip availability (called when an item is rented or returned). */
    void updateAvailability(String equipmentId, boolean available);
}
