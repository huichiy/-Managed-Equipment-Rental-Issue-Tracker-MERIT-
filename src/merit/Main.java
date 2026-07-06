package merit;

import java.sql.Connection;

import javax.swing.SwingUtilities;

import merit.dao.BillDao;
import merit.dao.BillDaoSqlite;
import merit.dao.Database;
import merit.dao.EquipmentDao;
import merit.dao.EquipmentDaoSqlite;
import merit.dao.RentalDao;
import merit.dao.RentalDaoSqlite;
import merit.dao.UserDao;
import merit.dao.UserDaoSqlite;
import merit.service.AuthService;
import merit.service.BillGenerator;
import merit.service.RentalManager;
import merit.ui.AppServices;
import merit.ui.LoginFrame;

/**
 * Application entry point. Wires the layers together — Database → DAOs → services → UI —
 * and launches the login screen on the Swing event dispatch thread. The wiring is the
 * only place concrete implementations are chosen; everything downstream depends on interfaces.
 */
public class Main {

    public static void main(String[] args) {
        Connection connection = Database.getConnection();   // creates schema + seeds on first run

        EquipmentDao equipmentDao = new EquipmentDaoSqlite(connection);
        UserDao userDao = new UserDaoSqlite(connection);
        RentalDao rentalDao = new RentalDaoSqlite(connection, userDao, equipmentDao);
        BillDao billDao = new BillDaoSqlite(connection);

        AppServices services = new AppServices(
                equipmentDao, userDao, rentalDao, billDao,
                new AuthService(userDao),
                new RentalManager(rentalDao, equipmentDao),
                new BillGenerator());

        SwingUtilities.invokeLater(() -> new LoginFrame(services).setVisible(true));
    }
}
