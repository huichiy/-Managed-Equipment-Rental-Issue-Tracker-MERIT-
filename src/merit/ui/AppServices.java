package merit.ui;

import merit.dao.BillDao;
import merit.dao.EquipmentDao;
import merit.dao.RentalDao;
import merit.dao.UserDao;
import merit.service.AuthService;
import merit.service.BillGenerator;
import merit.service.RentalManager;

/**
 * Immutable bundle of wired services/DAOs handed to the UI frames, so each frame
 * gets what it needs without a sprawling constructor. Assembled once in {@code Main}.
 */
public class AppServices {

    public final EquipmentDao equipmentDao;
    public final UserDao userDao;
    public final RentalDao rentalDao;
    public final BillDao billDao;
    public final AuthService authService;
    public final RentalManager rentalManager;
    public final BillGenerator billGenerator;

    public AppServices(EquipmentDao equipmentDao, UserDao userDao, RentalDao rentalDao, BillDao billDao,
                       AuthService authService, RentalManager rentalManager, BillGenerator billGenerator) {
        this.equipmentDao = equipmentDao;
        this.userDao = userDao;
        this.rentalDao = rentalDao;
        this.billDao = billDao;
        this.authService = authService;
        this.rentalManager = rentalManager;
        this.billGenerator = billGenerator;
    }
}
