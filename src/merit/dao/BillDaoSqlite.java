package merit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import merit.model.Bill;

/**
 * SQLite-backed {@link BillDao}. Takes a {@link Connection} (from {@code Database})
 * so it never imports C's own {@code Database} — the only coupling is {@code java.sql}.
 *
 * <p>Note: {@code bills.rental_id} is a FK to {@code rentals} and {@code Database}
 * enables {@code PRAGMA foreign_keys = ON}, so the referenced rental must already
 * exist before a bill can be inserted.
 */
public class BillDaoSqlite implements BillDao {

    private final Connection connection;

    public BillDaoSqlite(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void insert(Bill bill) {
        String sql = "INSERT INTO bills "
                + "(bill_id, rental_id, quantity, base_fee, discount, penalty, net_payable, created_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bill.getBillId());
            ps.setString(2, bill.getRentalId());
            ps.setInt(3, bill.getQuantity());
            ps.setDouble(4, bill.getBaseFee());
            ps.setDouble(5, bill.getDiscount());
            ps.setDouble(6, bill.getPenalty());
            ps.setDouble(7, bill.getNetPayable());
            ps.setString(8, bill.getCreatedAt());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert bill: " + bill.getBillId(), e);
        }
    }

    @Override
    public Bill findByRentalId(String rentalId) {
        String sql = "SELECT * FROM bills WHERE rental_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, rentalId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? reconstruct(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load bill for rental: " + rentalId, e);
        }
    }

    @Override
    public List<Bill> findAll() {
        List<Bill> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM bills");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(reconstruct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load bills", e);
        }
        return result;
    }

    private Bill reconstruct(ResultSet rs) throws SQLException {
        return new Bill(
                rs.getString("bill_id"),
                rs.getString("rental_id"),
                rs.getInt("quantity"),
                rs.getDouble("base_fee"),
                rs.getDouble("discount"),
                rs.getDouble("penalty"),
                rs.getDouble("net_payable"),
                rs.getString("created_at"));
    }
}
