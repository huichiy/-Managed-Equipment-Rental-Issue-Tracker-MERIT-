package merit.dao;

import java.util.List;

import merit.model.Rental;

/**
 * Phase-0 frozen contract; Member B owns the SQLite implementation.
 */
public interface RentalDao {

    void insert(Rental rental);

    /** Persists the return-side fields (return_date / days_late / damaged / returned). */
    void update(Rental rental);

    Rental findById(String id);

    List<Rental> findAll();
}
