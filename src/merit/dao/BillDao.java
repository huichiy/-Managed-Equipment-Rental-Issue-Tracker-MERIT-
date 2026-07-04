package merit.dao;

import java.util.List;

import merit.model.Bill;

/**
 * Phase-0 frozen contract shared by the whole team; Member C owns the SQLite
 * implementation. Keeps SQL out of the service/UI layers — no checked
 * {@code SQLException} leaks through this interface.
 */
public interface BillDao {

    void insert(Bill bill);

    /** @return the bill for the rental, or {@code null} if none exists (1-1 relationship). */
    Bill findByRentalId(String rentalId);

    List<Bill> findAll();
}
