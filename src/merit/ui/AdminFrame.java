package merit.ui;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import merit.model.Category;
import merit.model.Equipment;
import merit.model.EquipmentFactory;
import merit.model.User;
import merit.pricing.PricingPolicy;
import merit.pricing.PromotionalPricing;
import merit.pricing.StandardPricing;

/** Admin panel: view the catalog with stock counts, add equipment, and restock quantity. */
public class AdminFrame extends JFrame {

    private static final int COL_ID = 0;
    private static final int COL_TOTAL = 6;
    private static final int COL_AVAILABLE = 7;

    private final AppServices services;
    private final User user;

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Name", "Category", "Rate", "Replacement", "Pricing", "Total", "Available"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable table = new JTable(model);

    private final JTextField idField = new JTextField(6);
    private final JTextField nameField = new JTextField(9);
    private final JComboBox<Category> categoryBox = new JComboBox<>(Category.values());
    private final JTextField rateField = new JTextField(4);
    private final JTextField replacementField = new JTextField(6);
    private final JComboBox<String> pricingBox = new JComboBox<>(new String[]{"STANDARD", "PROMOTIONAL"});
    private final JTextField qtyField = new JTextField(3);

    public AdminFrame(AppServices services, User user) {
        this.services = services;
        this.user = user;
        setTitle("MERIT — Admin (Equipment Catalog)");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(880, 500);
        setLocationRelativeTo(null);
        add(UiSupport.topBar(user, this::logout), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(buildForm(), BorderLayout.SOUTH);
        refresh();
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.add(new JLabel("ID:")); form.add(idField);
        form.add(new JLabel("Name:")); form.add(nameField);
        form.add(new JLabel("Category:")); form.add(categoryBox);
        form.add(new JLabel("Rate:")); form.add(rateField);
        form.add(new JLabel("Replacement:")); form.add(replacementField);
        form.add(new JLabel("Pricing:")); form.add(pricingBox);
        form.add(new JLabel("Qty:")); form.add(qtyField);

        JButton add = new JButton("Add Equipment");
        add.addActionListener(e -> addEquipment());
        form.add(add);

        JButton restock = new JButton("Update Quantity");
        restock.addActionListener(e -> updateQuantity());
        form.add(restock);
        return form;
    }

    private void addEquipment() {
        String id = idField.getText().trim();
        String name = nameField.getText().trim();
        if (id.isEmpty() || name.isEmpty()) {
            warn("ID and Name are required.");
            return;
        }
        if (services.equipmentDao.findById(id) != null) {
            warn("Equipment ID already exists: " + id);
            return;
        }
        double rate;
        double replacement;
        int qty;
        try {
            rate = Double.parseDouble(rateField.getText().trim());
            replacement = Double.parseDouble(replacementField.getText().trim());
            qty = Integer.parseInt(qtyField.getText().trim());
        } catch (NumberFormatException ex) {
            warn("Rate/Replacement must be numbers and Qty a whole number.");
            return;
        }
        if (rate <= 0 || replacement < 0 || qty < 0) {
            warn("Rate must be > 0, Replacement >= 0, Qty >= 0.");
            return;
        }

        Category category = (Category) categoryBox.getSelectedItem();
        PricingPolicy pricing = pricingFor((String) pricingBox.getSelectedItem());
        // a fresh item starts with all units available
        Equipment equipment = EquipmentFactory.create(category, id, name, rate, replacement, pricing, qty, qty);
        services.equipmentDao.insert(equipment);
        clearForm();
        refresh();
    }

    /** Admin restock: set a new total quantity; keeps units currently out on rental accounted for. */
    private void updateQuantity() {
        int row = table.getSelectedRow();
        if (row < 0) {
            warn("Select an equipment row first.");
            return;
        }
        String id = (String) model.getValueAt(row, COL_ID);
        int oldTotal = (Integer) model.getValueAt(row, COL_TOTAL);
        int oldAvailable = (Integer) model.getValueAt(row, COL_AVAILABLE);

        String input = JOptionPane.showInputDialog(this,
                "New total quantity for " + id + ":", oldTotal);
        if (input == null) {
            return;   // cancelled
        }
        int newTotal;
        try {
            newTotal = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            warn("Quantity must be a whole number.");
            return;
        }
        if (newTotal < 0) {
            warn("Quantity cannot be negative.");
            return;
        }
        int onLoan = oldTotal - oldAvailable;                 // units currently rented out
        int newAvailable = Math.max(0, newTotal - onLoan);
        services.equipmentDao.updateQuantities(id, newTotal, newAvailable);
        refresh();
    }

    private void refresh() {
        model.setRowCount(0);
        List<Equipment> all = services.equipmentDao.findAll();
        for (Equipment e : all) {
            model.addRow(new Object[]{
                    e.getId(), e.getName(), e.getCategory(),
                    String.format("%.2f", e.getDailyRate()),
                    String.format("%.2f", e.getReplacementValue()),
                    e.getPricingPolicy().code(),
                    e.getTotalQuantity(), e.getAvailableQuantity()
            });
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        rateField.setText("");
        replacementField.setText("");
        qtyField.setText("");
    }

    private void logout() {
        new LoginFrame(services).setVisible(true);
        dispose();
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Admin", JOptionPane.WARNING_MESSAGE);
    }

    private static PricingPolicy pricingFor(String code) {
        return "PROMOTIONAL".equals(code) ? new PromotionalPricing() : new StandardPricing();
    }
}
