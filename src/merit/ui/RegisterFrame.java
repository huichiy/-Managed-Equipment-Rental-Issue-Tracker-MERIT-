package merit.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import merit.model.User;

/**
 * Self-registration screen (ported from Member T's prototype into the layered
 * {@code merit.*} app). Collects an account and delegates to
 * {@link merit.service.AuthService#register}; the service builds the right
 * {@link User} subtype and the DAO assigns the {@code user_id}.
 *
 * <p>Only Staff and Student can self-register — Admin accounts are seeded. The
 * "Final-year Student" choice maps to a {@code Student} with the 10% discount flag.
 */
public class RegisterFrame extends JFrame {

    /** Roles offered for self-registration → the model role the service expects. */
    private static final String STUDENT = "Student";
    private static final String FINAL_YEAR = "Final-year Student";
    private static final String STAFF = "Staff";

    private final AppServices services;
    private final JTextField nameField = new JTextField(16);
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JComboBox<String> roleBox =
            new JComboBox<>(new String[] {STUDENT, FINAL_YEAR, STAFF});

    public RegisterFrame(AppServices services) {
        this.services = services;
        setTitle("MERIT — Register");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 260);
        setLocationRelativeTo(null);
        setContentPane(buildUi());
    }

    private JPanel buildUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Name:"), c);
        c.gridx = 1; c.gridy = 0; panel.add(nameField, c);
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Username:"), c);
        c.gridx = 1; c.gridy = 1; panel.add(usernameField, c);
        c.gridx = 0; c.gridy = 2; panel.add(new JLabel("Password:"), c);
        c.gridx = 1; c.gridy = 2; panel.add(passwordField, c);
        c.gridx = 0; c.gridy = 3; panel.add(new JLabel("Role:"), c);
        c.gridx = 1; c.gridy = 3; panel.add(roleBox, c);

        JButton register = new JButton("Register");
        register.addActionListener(e -> attemptRegister());
        JButton back = new JButton("Back");
        back.addActionListener(e -> backToLogin());
        c.gridx = 0; c.gridy = 4; panel.add(back, c);
        c.gridx = 1; c.gridy = 4; panel.add(register, c);

        getRootPane().setDefaultButton(register);
        return panel;
    }

    private void attemptRegister() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String choice = (String) roleBox.getSelectedItem();

        if (name.isEmpty() || username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields.",
                    "Missing information", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String role = STAFF.equals(choice) ? "STAFF" : "STUDENT";
        boolean finalYear = FINAL_YEAR.equals(choice);

        User created = services.authService.register(username, password, name, role, finalYear);
        if (created == null) {
            JOptionPane.showMessageDialog(this, "That username is already taken.",
                    "Registration failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Registered! Please log in.");
        backToLogin();
    }

    private void backToLogin() {
        new LoginFrame(services).setVisible(true);
        dispose();
    }
}
