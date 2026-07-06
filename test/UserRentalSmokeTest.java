import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import merit.dao.EquipmentDao;
import merit.dao.RentalDao;
import merit.model.Admin;
import merit.model.ElectronicsEquipment;
import merit.model.Equipment;
import merit.model.Rental;
import merit.model.Staff;
import merit.model.Student;
import merit.model.User;
import merit.pricing.StandardPricing;
import merit.service.RentalManager;

/**
 * Pure-logic smoke test for Member B (no database, no sqlite-jdbc jar needed).
 * Covers polymorphic discount, due-date / days-late math, availability toggling,
 * and rent validation. DAOs are replaced by in-memory fakes.
 *
 * Run:  javac -d bin $(find src test -name "*.java")  &&  java -cp bin UserRentalSmokeTest
 */
public class UserRentalSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    // ---- in-memory fakes so this test needs no DB ----
    private static class FakeRentalDao implements RentalDao {
        Rental inserted;
        Rental updated;
        public void insert(Rental r) { inserted = r; }
        public void update(Rental r) { updated = r; }
        public Rental findById(String id) { return null; }
        public List<Rental> findAll() { return new ArrayList<>(); }
    }

    private static class FakeEquipmentDao implements EquipmentDao {
        int lastTotal = -1;
        int lastAvailable = -1;
        public List<Equipment> findAll() { return new ArrayList<>(); }
        public Equipment findById(String id) { return null; }
        public List<Equipment> findAvailable() { return new ArrayList<>(); }
        public void insert(Equipment e) { }
        public void updateQuantities(String id, int totalQuantity, int availableQuantity) {
            lastTotal = totalQuantity;
            lastAvailable = availableQuantity;
        }
    }

    public static void main(String[] args) {
        System.out.println("=== MERIT Member B — Users / Auth / Rentals smoke test ===\n");

        // --- polymorphic discount ---
        assertEquals("Admin discount", 0.00, new Admin("U001", "admin", "admin", "Admin").getDiscountRate());
        assertEquals("Staff discount", 0.20, new Staff("U002", "staff", "staff", "Dr. Tan").getDiscountRate());
        assertEquals("Student discount (regular)", 0.00,
                new Student("U003", "student", "student", "Reg").getDiscountRate());
        assertEquals("Student discount (final-year)", 0.10,
                new Student("U004", "finalyear", "finalyear", "Final", true).getDiscountRate());

        User student = new Student("U003", "student", "student", "Reg");
        FakeEquipmentDao eqDao = new FakeEquipmentDao();
        FakeRentalDao rentalDao = new FakeRentalDao();
        RentalManager mgr = new RentalManager(rentalDao, eqDao);
        LocalDate rentDate = LocalDate.of(2026, 7, 1);

        // --- rent: due date + availability toggle ---
        Equipment tablet = new ElectronicsEquipment("E002", "Tablet", 10.0, 1500.0, true, new StandardPricing());
        Rental r1 = mgr.rent("R001", student, tablet, 14, 1, rentDate);
        assertEqualsStr("due date = rentDate + 14", "2026-07-15", r1.getDueDate().toString());
        assertTrue("rent persisted the rental", rentalDao.inserted == r1);
        assertEquals("rent decremented available stock (DAO)", 0.0, eqDao.lastAvailable);
        assertTrue("no unit available after single-unit rent (object)", !tablet.isAvailable());

        // --- return 2 days late + damaged ---
        mgr.returnEquipment(r1, LocalDate.of(2026, 7, 17), true);
        assertEquals("days late = 2", 2.0, r1.getDaysLate());
        assertTrue("damaged flag set", r1.isDamaged());
        assertTrue("returned flag set", r1.isReturned());
        assertEquals("return restored available stock (DAO)", 1.0, eqDao.lastAvailable);
        assertTrue("unit available again after return (object)", tablet.isAvailable());

        // --- early return clamps days late to 0 ---
        Equipment cam = new ElectronicsEquipment("E003", "Camera", 10.0, 1500.0, true, new StandardPricing());
        Rental r2 = mgr.rent("R002", student, cam, 14, 1, rentDate);   // due 2026-07-15
        mgr.returnEquipment(r2, LocalDate.of(2026, 7, 10), false);      // 5 days early
        assertEquals("early return days late = 0", 0.0, r2.getDaysLate());

        // --- multi-unit rent: 5 in stock, rent 3 -> 2 left ---
        Equipment batch = new ElectronicsEquipment("E010", "Batch", 10.0, 1500.0, new StandardPricing(), 5, 5);
        Rental r3 = mgr.rent("R010", student, batch, 7, 3, rentDate);
        assertEquals("rented quantity recorded", 3.0, r3.getQuantity());
        assertEquals("stock 5 - 3 = 2 (object)", 2.0, batch.getAvailableQuantity());
        assertEquals("stock 5 - 3 = 2 (DAO)", 2.0, eqDao.lastAvailable);
        // returning the batch restores all 3 units
        mgr.returnEquipment(r3, rentDate.plusDays(7), false);
        assertEquals("return restored all 3 -> 5", 5.0, batch.getAvailableQuantity());

        // --- validation: days out of range ---
        Equipment mic = new ElectronicsEquipment("E004", "Mic", 10.0, 400.0, true, new StandardPricing());
        try {
            mgr.rent("R003", student, mic, 31, 1, rentDate);
            fail("expected IllegalArgumentException for 31 days");
        } catch (IllegalArgumentException expected) {
            pass("rejects days > 30");
        }

        // --- validation: requesting more units than available ---
        Equipment few = new ElectronicsEquipment("E011", "Few", 10.0, 400.0, new StandardPricing(), 2, 2);
        try {
            mgr.rent("R011", student, few, 14, 3, rentDate);
            fail("expected IllegalStateException for over-request (3 of 2)");
        } catch (IllegalStateException expected) {
            pass("rejects renting more units than available");
        }

        // --- validation: out of stock (0 available) ---
        Equipment busy = new ElectronicsEquipment("E005", "Busy", 10.0, 400.0, false, new StandardPricing());
        try {
            mgr.rent("R004", student, busy, 14, 1, rentDate);
            fail("expected IllegalStateException for out-of-stock equipment");
        } catch (IllegalStateException expected) {
            pass("rejects renting out-of-stock equipment");
        }

        System.out.printf("%n=== %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertEquals(String label, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            pass(label + " = " + actual);
        } else {
            fail(label + " expected " + expected + " but got " + actual);
        }
    }

    private static void assertEqualsStr(String label, String expected, String actual) {
        if (expected.equals(actual)) {
            pass(label + " = " + actual);
        } else {
            fail(label + " expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(String label, boolean cond) {
        if (cond) {
            pass(label);
        } else {
            fail(label);
        }
    }

    private static void pass(String msg) { passed++; System.out.println("  [PASS] " + msg); }
    private static void fail(String msg) { failed++; System.out.println("  [FAIL] " + msg); }
}
