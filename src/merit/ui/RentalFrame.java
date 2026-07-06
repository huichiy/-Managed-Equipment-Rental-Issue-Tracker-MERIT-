package merit.ui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import merit.model.Bill;
import merit.model.Equipment;
import merit.model.Rental;
import merit.model.User;

/**
 * Rental flow for Staff/Student: browse available equipment and rent it, then return
 * an active rental and see the itemised bill. Every action goes UI → service → DAO;
 * no SQL and no pricing/penalty logic lives here.
 */
public class RentalFrame extends JFrame {

    private final AppServices services;
    private final User user;

    private final DefaultTableModel catalogModel = new DefaultTableModel(
            new Object[]{"ID", "Name", "Category", "Rate", "Pricing", "Available"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable catalogTable = new JTable(catalogModel);
    private final List<Equipment> catalog = new ArrayList<>();
    private final JSpinner daysSpinner = new JSpinner(new SpinnerNumberModel(14, 1, 30, 1));
    // max is updated to the selected item's available stock (see updateQtyMax)
    private final JSpinner qtySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));

    private final DefaultTableModel rentalModel = new DefaultTableModel(
            new Object[]{"Rental", "Equipment", "Days", "Due date"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JTable rentalTable = new JTable(rentalModel);
    private final List<Rental> activeRentals = new ArrayList<>();
    private final JSpinner lateSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 60, 1));
    private final JCheckBox damagedBox = new JCheckBox("Damaged");

    public RentalFrame(AppServices services, User user) {
        this.services = services;
        this.user = user;
        setTitle("MERIT — " + user.getName());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(720, 470);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Catalog & Rent", buildCatalogTab());
        tabs.addTab("My Rentals & Return", buildRentalTab());
        add(UiSupport.topBar(user, this::logout), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        // cap the quantity spinner to whatever row is selected in the catalog
        catalogTable.getSelectionModel().addListSelectionListener(e -> updateQtyMax());

        refreshCatalog();
        refreshRentals();
    }

    private void logout() {
        new LoginFrame(services).setVisible(true);
        dispose();
    }

    private JPanel buildCatalogTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(catalogTable), BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(new JLabel("Days (max 30):"));
        south.add(daysSpinner);
        south.add(new JLabel("Quantity:"));
        south.add(qtySpinner);
        JButton rent = new JButton("Rent selected");
        rent.addActionListener(e -> rent());
        south.add(rent);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    /** Clamp the quantity spinner's maximum to the selected item's available stock. */
    private void updateQtyMax() {
        int row = catalogTable.getSelectedRow();
        int max = (row >= 0 && row < catalog.size()) ? catalog.get(row).getAvailableQuantity() : 1;
        if (max < 1) {
            max = 1;
        }
        SpinnerNumberModel m = (SpinnerNumberModel) qtySpinner.getModel();
        m.setMaximum(max);
        if ((Integer) qtySpinner.getValue() > max) {
            qtySpinner.setValue(max);
        }
    }

    private JPanel buildRentalTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(rentalTable), BorderLayout.CENTER);
        JPanel south = new JPanel();
        south.add(new JLabel("Days late:"));
        south.add(lateSpinner);
        south.add(damagedBox);
        JButton ret = new JButton("Return & bill");
        ret.addActionListener(e -> returnSelected());
        south.add(ret);
        panel.add(south, BorderLayout.SOUTH);
        return panel;
    }

    private void rent() {
        int row = catalogTable.getSelectedRow();
        if (row < 0) {
            warn("Select an item to rent.");
            return;
        }
        Equipment equipment = catalog.get(row);
        int days = (int) daysSpinner.getValue();
        int quantity = (int) qtySpinner.getValue();
        try {
            Rental rental = services.rentalManager.rent(newRentalId(), user, equipment, days, quantity);
            JOptionPane.showMessageDialog(this,
                    "Rented " + quantity + " x " + equipment.getName() + "\nDue date: " + rental.getDueDate(),
                    "Rented", JOptionPane.INFORMATION_MESSAGE);
            refreshCatalog();
            refreshRentals();
        } catch (RuntimeException ex) {
            warn(ex.getMessage());
        }
    }

    private void returnSelected() {
        int row = rentalTable.getSelectedRow();
        if (row < 0) {
            warn("Select a rental to return.");
            return;
        }
        Rental rental = activeRentals.get(row);
        int daysLate = (int) lateSpinner.getValue();
        boolean damaged = damagedBox.isSelected();
        LocalDate returnDate = rental.getDueDate().plusDays(daysLate);

        services.rentalManager.returnEquipment(rental, returnDate, damaged);
        Bill bill = services.billGenerator.generate(rental);
        services.billDao.insert(bill);
        showBill(bill);

        damagedBox.setSelected(false);
        lateSpinner.setValue(0);
        refreshCatalog();
        refreshRentals();
    }

    private void refreshCatalog() {
        catalog.clear();
        catalogModel.setRowCount(0);
        for (Equipment e : services.equipmentDao.findAvailable()) {
            catalog.add(e);
            catalogModel.addRow(new Object[]{
                    e.getId(), e.getName(), e.getCategory(),
                    String.format("%.2f", e.getDailyRate()), e.getPricingPolicy().code(),
                    e.getAvailableQuantity()
            });
        }
    }

    private void refreshRentals() {
        activeRentals.clear();
        rentalModel.setRowCount(0);
        for (Rental r : services.rentalDao.findAll()) {
            if (r.isReturned() || r.getUser() == null || !user.getId().equals(r.getUser().getId())) {
                continue;
            }
            activeRentals.add(r);
            rentalModel.addRow(new Object[]{
                    r.getId(), r.getEquipment().getName(), r.getRentalDays(), r.getDueDate()
            });
        }
    }

    private void showBill(Bill bill) {
        JTextArea area = new JTextArea(bill.toDetailedString());
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, area, "Bill " + bill.getBillId(),
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void warn(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Rental", JOptionPane.WARNING_MESSAGE);
    }

    private String newRentalId() {
        return "R" + System.nanoTime();
    }
}
