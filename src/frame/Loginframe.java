package frame;
import dao.Userdao;
import java.awt.*;
import javax.swing.*;

public class Loginframe extends JFrame {

    private JTextField idField = new JTextField(15);
    private JPasswordField passField = new JPasswordField(15);
    private final JButton loginBtn = new JButton("Login");
    private final JButton registerBtn = new JButton("Register");

    public Loginframe() {
        setTitle("MERIT - Login");
        setSize(360, 160);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridLayout(3,2,8,8));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        panel.add(new JLabel("ID:"));
        panel.add(idField);
        panel.add(new JLabel("Password:"));
        panel.add(passField);
        panel.add(registerBtn);
        panel.add(loginBtn);
        add(panel);

        loginBtn.addActionListener(e -> {
            try {
                String id = idField.getText();
                String pass = new String(passField.getPassword());
                String name = Userdao.login(id, pass);
                if (name == null) {
                    JOptionPane.showMessageDialog(this, "Wrong ID or password");
                } else {
                    JOptionPane.showMessageDialog(this, "Welcome, " + name + "!");
                    new Userframe(Userdao.getUserById(id)).setVisible(true);
                    dispose();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "DB error: " + ex.getMessage());
            }
        });

        registerBtn.addActionListener(e -> {
            new Registerframe().setVisible(true);
            dispose();
        });
    }
}