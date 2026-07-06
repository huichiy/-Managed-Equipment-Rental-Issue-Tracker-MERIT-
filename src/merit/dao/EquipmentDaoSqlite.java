package merit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import merit.model.Category;
import merit.model.Equipment;
import merit.model.EquipmentFactory;
import merit.pricing.PricingPolicy;
import merit.pricing.PromotionalPricing;
import merit.pricing.StandardPricing;

/**
 * SQLite-backed {@link EquipmentDao}. Takes a {@link Connection} (provided by
 * Member C's {@code Database}) so this class never imports C's code — the only
 * coupling is the JDK's {@code java.sql} API.
 *
 * <p>{@link #reconstruct} rebuilds a typed object from a flat row, delegating the
 * category→subclass mapping to {@link EquipmentFactory} (the one place that switches
 * on category).
 */
public class EquipmentDaoSqlite implements EquipmentDao {

    private final Connection connection;

    public EquipmentDaoSqlite(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<Equipment> findAll() {
        return query("SELECT * FROM equipment", "load equipment list");
    }

    @Override
    public List<Equipment> findAvailable() {
        return query("SELECT * FROM equipment WHERE available_quantity > 0", "load available equipment");
    }

    @Override
    public Equipment findById(String id) {
        String sql = "SELECT * FROM equipment WHERE equipment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? reconstruct(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load equipment: " + id, e);
        }
    }

    @Override
    public void insert(Equipment equipment) {
        String sql = "INSERT INTO equipment "
                + "(equipment_id, name, category, daily_rate, replacement_value, pricing_policy, "
                + "total_quantity, available_quantity) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, equipment.getId());
            ps.setString(2, equipment.getName());
            ps.setString(3, equipment.getCategory().name());
            ps.setDouble(4, equipment.getDailyRate());
            ps.setDouble(5, equipment.getReplacementValue());
            ps.setString(6, equipment.getPricingPolicy().code());
            ps.setInt(7, equipment.getTotalQuantity());
            ps.setInt(8, equipment.getAvailableQuantity());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert equipment: " + equipment.getId(), e);
        }
    }

    @Override
    public void updateQuantities(String id, int totalQuantity, int availableQuantity) {
        String sql = "UPDATE equipment SET total_quantity = ?, available_quantity = ? WHERE equipment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, totalQuantity);
            ps.setInt(2, availableQuantity);
            ps.setString(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update quantities: " + id, e);
        }
    }

    private List<Equipment> query(String sql, String action) {
        List<Equipment> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(reconstruct(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to " + action, e);
        }
        return result;
    }

    // ---- rebuild a typed object from a flat row; category mapping lives in EquipmentFactory ----
    private Equipment reconstruct(ResultSet rs) throws SQLException {
        String id = rs.getString("equipment_id");
        String name = rs.getString("name");
        double dailyRate = rs.getDouble("daily_rate");
        double replacementValue = rs.getDouble("replacement_value");
        int totalQuantity = rs.getInt("total_quantity");
        int availableQuantity = rs.getInt("available_quantity");
        PricingPolicy pricing = reconstructPricing(rs.getString("pricing_policy"));
        Category category = Category.valueOf(rs.getString("category"));
        return EquipmentFactory.create(category, id, name, dailyRate, replacementValue, pricing,
                totalQuantity, availableQuantity);
    }

    private PricingPolicy reconstructPricing(String code) {
        return switch (code) {
            case "STANDARD" -> new StandardPricing();
            case "PROMOTIONAL" -> new PromotionalPricing();
            default -> throw new IllegalStateException("Unknown pricing policy code: " + code);
        };
    }
}
