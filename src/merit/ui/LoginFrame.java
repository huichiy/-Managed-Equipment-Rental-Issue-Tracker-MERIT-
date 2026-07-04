package merit.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import merit.model.Admin;
import merit.model.User;

/**
 * Login screen. Authenticates via {@link merit.service.AuthService} and routes by role:
 * an {@link Admin} goes to the {@link AdminFrame}; everyone else to the {@link RentalFrame}.
 * The role check here is UI routing — the domain/service layers never branch on role.
 */
public class LoginFrame extends JFrame {

    private final AppServices services;
    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);

    public LoginFrame(AppServices services) {
        this.services = services;
        setTitle("MERIT — Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(380, 210);
        setLocationRelativeTo(null);
        setContentPane(buildUi());
    }

    private JPanel buildUi() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0; c.gridy = 0; panel.add(new JLabel("Username:"), c);
        c.gridx = 1; c.gridy = 0; panel.add(usernameField, c);
        c.gridx = 0; c.gridy = 1; panel.add(new JLabel("Password:"), c);
        c.gridx = 1; c.gridy = 1; panel.add(passwordField, c);

        JButton login = new JButton("Login");
        login.addActionListener(e -> attemptLogin());
        c.gridx = 1; c.gridy = 2; panel.add(login, c);

        getRootPane().setDefaultButton(login);
        return panel;
    }

    private void attemptLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        User user = services.authService.login(username, password);
        if (user == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password.",
                    "Login failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        JFrame next = (user instanceof Admin)
                ? new AdminFrame(services, user)
                : new RentalFrame(services, user);
        next.setVisible(true);
        dispose();
    }
}
