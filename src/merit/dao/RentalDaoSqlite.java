package merit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import merit.model.Equipment;
import merit.model.Rental;
import merit.model.User;

/**
 * SQLite-backed {@link RentalDao}. A rental row is flat (just {@code user_id} /
 * {@code equipment_id}); this DAO rebuilds the aggregate by delegating to the
 * {@link UserDao} and {@link EquipmentDao} — layers collaborate through interfaces,
 * so no DAO re-implements another's row→object mapping.
 */
public class RentalDaoSqlite implements RentalDao {

    private final Connection connection;
    private final UserDao userDao;
    private final EquipmentDao equipmentDao;

    public RentalDaoSqlite(Connection connection, UserDao userDao, EquipmentDao equipmentDao) {
        this.connection = connection;
        this.userDao = userDao;
        this.equipmentDao = equipmentDao;
    }

    @Override
    public void insert(Rental r) {
        String sql = "INSERT INTO rentals "
                + "(rental_id, user_id, equipment_id, rental_days, quantity, rent_date, due_date, "
                + "return_date, days_late, damaged, returned) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getUser().getId());
            ps.setString(3, r.getEquipment().getId());
            ps.setInt(4, r.getRentalDays());
            ps.setInt(5, r.getQuantity());
            ps.setString(6, iso(r.getRentDate()));
            ps.setString(7, iso(r.getDueDate()));
            ps.setString(8, iso(r.getReturnDate()));
            ps.setInt(9, r.getDaysLate());
            ps.setInt(10, r.isDamaged() ? 1 : 0);
            ps.setInt(11, r.isReturned() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert rental: " + r.getId(), e);
        }
    }

    @Override
    public void update(Rental r) {
        String sql = "UPDATE rentals SET return_date = ?, days_late = ?, damaged = ?, returned = ? "
                + "WHERE rental_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, iso(r.getReturnDate()));
            ps.setInt(2, r.getDaysLate());
            ps.setInt(3, r.isDamaged() ? 1 : 0);
            ps.setInt(4, r.isReturned() ? 1 : 0);
            ps.setString(5, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update rental: " + r.getId(), e);
        }
    }

    @Override
    public Rental findById(String id) {
        String sql = "SELECT * FROM rentals WHERE rental_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? reconstruct(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load rental: " + id, e);
        }
    }

    @Override
    public List<Rental> findAll() {
        List<Rental> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM rentals");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(reconstruct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load rentals", e);
        }
        return result;
    }

    private Rental reconstruct(ResultSet rs) throws SQLException {
        String id = rs.getString("rental_id");
        User user = userDao.findById(rs.getString("user_id"));
        Equipment equipment = equipmentDao.findById(rs.getString("equipment_id"));
        int rentalDays = rs.getInt("rental_days");
        int quantity = rs.getInt("quantity");
        LocalDate rentDate = parse(rs.getString("rent_date"));

        Rental rental = new Rental(id, user, equipment, rentalDays, rentDate, quantity);
        rental.setReturnDate(parse(rs.getString("return_date")));
        rental.setDaysLate(rs.getInt("days_late"));
        rental.setDamaged(rs.getInt("damaged") == 1);
        rental.setReturned(rs.getInt("returned") == 1);
        return rental;
    }

    private static String iso(LocalDate d) {
        return d == null ? null : d.toString();
    }

    private static LocalDate parse(String s) {
        return s == null ? null : LocalDate.parse(s);
    }
}
