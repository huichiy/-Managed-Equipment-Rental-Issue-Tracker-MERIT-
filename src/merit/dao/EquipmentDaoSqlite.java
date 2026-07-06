package merit.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import merit.model.Category;
import merit.model.ElectronicsEquipment;
import merit.model.Equipment;
import merit.model.LabEquipment;
import merit.model.MediaEquipment;
import merit.pricing.PricingPolicy;
import merit.pricing.PromotionalPricing;
import merit.pricing.StandardPricing;

/**
 * SQLite-backed {@link EquipmentDao}.
 *
 * <p>Takes a JDBC {@link Connection} in its constructor (supplied by Member C's
 * {@code Database} at integration time), so it has no compile-time dependency on
 * any concrete database bootstrap class and can be unit-tested against an
 * in-memory SQLite database.
 *
 * <p><b>The one permitted category switch.</b> {@link #mapRow} is the object-
 * relational reconstruction factory: it maps the flat {@code category} column
 * back to the right {@link Equipment} subclass (which carries that category's
 * default penalty policy) and the {@code pricing_policy} column back to the right
 * {@link PricingPolicy}. This is a necessary boundary between a flat table and a
 * polymorphic object graph — and it is the <i>only</i> place in the whole system
 * that branches on category. No business logic ever does.
 */
public class EquipmentDaoSqlite implements EquipmentDao {

    private final Connection connection;

    public EquipmentDaoSqlite(Connection connection) {
        if (connection == null) {
            throw new IllegalArgumentException("connection must not be null");
        }
        this.connection = connection;
    }

    @Override
    public void add(Equipment equipment) {
        String sql = "INSERT INTO equipment "
                + "(equipment_id, name, category, daily_rate, replacement_value, pricing_policy, available) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, equipment.getId());
            ps.setString(2, equipment.getName());
            ps.setString(3, equipment.getCategory().name());
            ps.setDouble(4, equipment.getDailyRate());
            ps.setDouble(5, equipment.getReplacementValue());
            ps.setString(6, pricingCode(equipment.getPricingPolicy()));
            ps.setInt(7, equipment.isAvailable() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to add equipment " + equipment.getId(), e);
        }
    }

    @Override
    public Equipment findById(String equipmentId) {
        String sql = "SELECT * FROM equipment WHERE equipment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, equipmentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to find equipment " + equipmentId, e);
        }
    }

    @Override
    public List<Equipment> findAll() {
        return query("SELECT * FROM equipment");
    }

    @Override
    public List<Equipment> findAvailable() {
        return query("SELECT * FROM equipment WHERE available = 1");
    }

    @Override
    public void updateAvailability(String equipmentId, boolean available) {
        String sql = "UPDATE equipment SET available = ? WHERE equipment_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, available ? 1 : 0);
            ps.setString(2, equipmentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update availability for " + equipmentId, e);
        }
    }

    private List<Equipment> query(String sql) {
        List<Equipment> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to query equipment", e);
        }
        return result;
    }

    /**
     * Rebuild one flat table row into the correct polymorphic {@link Equipment}.
     * The single, deliberate category switch in the system.
     */
    private Equipment mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("equipment_id");
        String name = rs.getString("name");
        double dailyRate = rs.getDouble("daily_rate");
        double replacementValue = rs.getDouble("replacement_value");
        boolean available = rs.getInt("available") == 1;
        PricingPolicy pricing = pricingFor(rs.getString("pricing_policy"));
        Category category = Category.valueOf(rs.getString("category"));

        switch (category) {
            case ELECTRONICS:
                return new ElectronicsEquipment(id, name, dailyRate, replacementValue, available, pricing);
            case MEDIA:
                return new MediaEquipment(id, name, dailyRate, replacementValue, available, pricing);
            case LAB:
                return new LabEquipment(id, name, dailyRate, replacementValue, available, pricing);
            default:
                throw new DataAccessException("Unknown category: " + category, null);
        }
    }

    /** Map the {@code pricing_policy} column value → a {@link PricingPolicy}. */
    private PricingPolicy pricingFor(String code) {
        if ("PROMOTIONAL".equals(code)) {
            return new PromotionalPricing();
        }
        return new StandardPricing();
    }

    /** Map a {@link PricingPolicy} → its {@code pricing_policy} column value. */
    private String pricingCode(PricingPolicy pricing) {
        return (pricing instanceof PromotionalPricing) ? "PROMOTIONAL" : "STANDARD";
    }
}
