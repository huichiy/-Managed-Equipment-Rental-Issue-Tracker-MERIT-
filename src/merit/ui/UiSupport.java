package merit.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import merit.model.User;

/** Small shared UI helpers so frames stay consistent. */
final class UiSupport {

    private UiSupport() {
    }

    /** A top bar showing who is logged in, with a Log out button on the right. */
    static JPanel topBar(User user, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.add(new JLabel("  Logged in as: " + user.getName()), BorderLayout.WEST);

        JButton logout = new JButton("Log out");
        logout.addActionListener(e -> onLogout.run());
        JPanel right = new JPanel();
        right.add(logout);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }
}
