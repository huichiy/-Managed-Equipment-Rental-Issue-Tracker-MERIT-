package frame;
import dao.Userdao;
import java.awt.*;
import javax.swing.*;

public class Registerframe extends JFrame {
    private JTextField NameField = new JTextField(15);
    private JTextField idField = new JTextField(15);
    private JPasswordField passField = new JPasswordField(15);
    private final JButton BackBtn = new JButton("Back");
    private final JButton registerBtn = new JButton("Register");
    private final JComboBox<String> roleBox =
        new JComboBox<>(new String[]{"Student", "Final-year Student", "Staff"});

    public Registerframe() {
        setTitle("MERIT - Register");
        setSize(360, 200);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(5,2,8,8));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(new JLabel("Name:"));
        panel.add(NameField);
        panel.add(new JLabel("ID:"));
        panel.add(idField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(new JLabel("Role:"));
        panel.add(roleBox);
        panel.add(registerBtn);
        panel.add(BackBtn);
        add(panel);

        BackBtn.addActionListener(e -> {
            new Loginframe().setVisible(true);
            dispose();
        });

        registerBtn.addActionListener(e -> {
            try {
                String name = NameField.getText().trim();
                String id = idField.getText().trim();
                String pass = new String(passField.getPassword());
                String role = (String) roleBox.getSelectedItem();

                if (name.isEmpty() || id.isEmpty() || pass.isEmpty() || role == null) {
                    JOptionPane.showMessageDialog(this, "Please fill in all fields");
                    return;
                }

            // role translation (see below)
            String type;
            boolean fy;
            if (role.equals("Staff")) {
                type = "STAFF";
                fy = false;
            } else if (role.equals("Final-year Student")) {
                type = "STUDENT";
                fy = true;
            } else {
                type = "STUDENT";
                fy = false;
            }
            // call UserDAO.register(...)
            boolean ok = Userdao.register(id, name, pass, type, fy);
            // if true  -> "Registered! Please log in." + go back to Loginframe
            // if false -> "ID already exists"
            if (ok) {
                JOptionPane.showMessageDialog(this, "Registered! Please log in.");
                new Loginframe().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "ID already exists");
            }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        
            });
    }
}