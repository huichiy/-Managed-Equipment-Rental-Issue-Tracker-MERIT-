package frame;

import bridge.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import model.*;

public class Userframe extends JFrame {

    // === 所有字段声明放最前面 ===
    private User currentUser;
    private List<Equipment> equipmentList = new ArrayList<>();
    private JComboBox<Equipment> equipmentBox = new JComboBox<>();
    private JTextField daysField = new JTextField("14", 5);
    private JTextField lateField = new JTextField("0", 5);
    private JComboBox<String> damageBox = new JComboBox<>(
            new String[]{"NONE", "MINOR", "MAJOR", "TOTAL"});
    private JButton generateBtn = new JButton("Generate Bill");
    private JTextArea billArea = new JTextArea(8, 30);

    // === 构造器:所有可执行语句都在这对大括号里面 ===
    public Userframe(User user) {
        this.currentUser = user;
        setupEquipmentList();

        setTitle("MERIT - Main");
        setSize(1000, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        for (Equipment eq : equipmentList) {
            equipmentBox.addItem(eq);
        }

        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        form.add(new JLabel("Equipment:"));
        form.add(equipmentBox);
        form.add(new JLabel("Rental Days:"));
        form.add(daysField);
        form.add(new JLabel("Days Late:"));
        form.add(lateField);
        form.add(new JLabel("Damage Level:"));
        form.add(damageBox);

        billArea.setEditable(false);

        setLayout(new BorderLayout());
        add(new JLabel("Welcome, " + user.getName()
                + " (" + (int)(user.getDiscountRate() * 100) + "% discount)"), BorderLayout.NORTH);
        add(form, BorderLayout.CENTER);
        add(generateBtn, BorderLayout.SOUTH);
        add(new JScrollPane(billArea), BorderLayout.EAST);

        generateBtn.addActionListener(e -> {
            try {
                Equipment selected = (Equipment) equipmentBox.getSelectedItem();
                int days = Integer.parseInt(daysField.getText().trim());
                int late = Integer.parseInt(lateField.getText().trim());
                String damage = (String) damageBox.getSelectedItem();

                Rental rental = new Rental(currentUser, selected, days, late, damage);
                Bill bill = new Bill(rental);

                billArea.setText(
                        "---- BILL ----\n" +
                        "Equipment: " + selected + "\n" +
                        "Base fee:    RM" + bill.getBaseFee() + "\n" +
                        "Discount:   -RM" + bill.getDiscount() + "\n" +
                        "Late fee:   +RM" + bill.getLateFee() + "\n" +
                        "Damage fee: +RM" + bill.getDamageFee() + "\n" +
                        "Net payable: RM" + bill.getNetPayable());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Days must be a number");
            }
        });
    }

    // === 普通方法放最后 ===
    private void setupEquipmentList() {
        equipmentList.add(new ElectronicsEquipment(
                "ELEC-001", "Dell Laptop", 15.0, 3000.0, new StandardPricing()));
        equipmentList.add(new MediaEquipment(
                "MEDIA-001", "Canon DSLR", 30.0, 5000.0, new PercentLatePricing()));
        equipmentList.add(new LabEquipment(
                "LAB-001", "Microscope", 40.0, 8000.0, new HighValuePricing()));
    }
}