package merit.dao;

import java.util.List;

import merit.model.Equipment;

/**
 * Phase-0 frozen contract shared by the whole team; Member A owns the SQLite
 * implementation. Keeps SQL out of the service/UI layers — no checked
 * {@code SQLException} leaks through this interface.
 */
public interface EquipmentDao {

    List<Equipment> findAll();

    /** @return the equipment, or {@code null} if no row matches the id. */
    Equipment findById(String id);

    List<Equipment> findAvailable();

    void insert(Equipment equipment);

    /** Persists the stock counts (used by renting/returning and by admin restocking). */
    void updateQuantities(String id, int totalQuantity, int availableQuantity);
}
