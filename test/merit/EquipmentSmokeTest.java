package merit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import merit.dao.EquipmentDao;
import merit.dao.EquipmentDaoSqlite;
import merit.model.ElectronicsEquipment;
import merit.model.Equipment;
import merit.model.LabEquipment;
import merit.model.MediaEquipment;
import merit.pricing.PromotionalPricing;
import merit.pricing.StandardPricing;

/**
 * Standalone verification harness for Member A's module (no JUnit needed).
 * Run it to confirm the Bridge maths and the DAO's category→subclass
 * reconstruction match the design doc. Exits non-zero if anything fails.
 *
 * <p>Not part of the production app — exclude from the submission zip if desired.
 * The DAO test builds its own in-memory SQLite schema, so it does not depend on
 * Member C's {@code Database} class.
 */
public class EquipmentSmokeTest {

    private static int checks = 0;
    private static int failures = 0;
    private static final double EPS = 1e-9;

    public static void main(String[] args) throws Exception {
        testPricingPolicies();
        testPenaltyPolicies();
        testEquipmentDelegation();
        testValidation();
        testDaoRoundTrip();

        System.out.println();
        System.out.println(failures == 0
                ? "ALL PASSED (" + checks + " checks)"
                : failures + " FAILED out of " + checks + " checks");
        System.exit(failures == 0 ? 0 : 1);
    }

    // --- Bridge implementor axis 1: pricing -------------------------------

    private static void testPricingPolicies() {
        // StandardPricing: rate * days
        eq("StandardPricing 40x3", new StandardPricing().calculateCharge(40.0, 3), 120.0);
        // PromotionalPricing: rate * days * 0.8
        eq("PromotionalPricing 10x3", new PromotionalPricing().calculateCharge(10.0, 3), 24.0);
    }

    // --- Bridge implementor axis 2: penalty -------------------------------

    private static void testPenaltyPolicies() {
        // Lab: late 1.0*rate*daysLate + damage 0.3*replacementValue
        Equipment lab = new LabEquipment("L001", "Microscope", 40.0, 5000.0, true, new StandardPricing());
        eq("LabPenalty 2 late + damaged", lab.calculatePenalty(2, true), 80.0 + 1500.0);   // 1580
        eq("LabPenalty on-time undamaged", lab.calculatePenalty(0, false), 0.0);

        // Electronics: late 0.5*rate*daysLate + damage 0.4*replacementValue
        Equipment el = new ElectronicsEquipment("E001", "Laptop", 15.0, 2500.0, true, new StandardPricing());
        eq("ElectronicsPenalty 2 late + damaged", el.calculatePenalty(2, true), 15.0 + 1000.0); // 1015

        // Media (StandardPenalty): late 0.5*rate*daysLate + damage 0.3*replacementValue
        Equipment media = new MediaEquipment("M001", "DSLR Camera", 30.0, 3500.0, true, new StandardPricing());
        eq("StandardPenalty 2 late + damaged", media.calculatePenalty(2, true), 30.0 + 1050.0); // 1080
    }

    // --- Abstraction delegates to the injected policy (polymorphism) ------

    private static void testEquipmentDelegation() {
        Equipment promoTablet =
                new ElectronicsEquipment("E002", "Tablet", 10.0, 1500.0, true, new PromotionalPricing());
        // Same call, promotional behaviour because of the injected policy.
        eq("Promo tablet 3-day charge", promoTablet.calculateRentalCharge(3), 24.0);
    }

    // --- Encapsulation / input validation ---------------------------------

    private static void testValidation() {
        Equipment lab = new LabEquipment("L001", "Microscope", 40.0, 5000.0, true, new StandardPricing());
        expectIllegalArg("days <= 0 rejected", () -> lab.calculateRentalCharge(0));
        expectIllegalArg("negative daysLate rejected", () -> lab.calculatePenalty(-1, false));
        expectIllegalArg("negative dailyRate rejected",
                () -> new LabEquipment("X", "Bad", -1.0, 100.0, true, new StandardPricing()));
    }

    // --- DAO: the category -> subclass reconstruction ---------------------

    private static void testDaoRoundTrip() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            createSchema(conn);
            EquipmentDao dao = new EquipmentDaoSqlite(conn);

            // add() a promotional electronics item, then read it back.
            dao.add(new ElectronicsEquipment("E002", "Tablet", 10.0, 1500.0, true, new PromotionalPricing()));
            dao.add(new LabEquipment("L001", "Microscope", 40.0, 5000.0, true, new StandardPricing()));
            dao.add(new MediaEquipment("M002", "Projector", 25.0, 2000.0, false, new StandardPricing()));

            eq("findAll count", dao.findAll().size(), 3);
            eq("findAvailable count (projector unavailable)", dao.findAvailable().size(), 2);

            Equipment tablet = dao.findById("E002");
            check("E002 rebuilt as ElectronicsEquipment", tablet instanceof ElectronicsEquipment);
            // Promotional pricing survived the round-trip: 10 * 3 * 0.8 = 24.
            eq("E002 promotional charge after reload", tablet.calculateRentalCharge(3), 24.0);
            // Electronics default penalty survived: 0.5*10*2 + 0.4*1500 = 610.
            eq("E002 electronics penalty after reload", tablet.calculatePenalty(2, true), 610.0);

            Equipment scope = dao.findById("L001");
            check("L001 rebuilt as LabEquipment", scope instanceof LabEquipment);
            eq("L001 standard charge after reload", scope.calculateRentalCharge(3), 120.0);
            eq("L001 lab penalty after reload", scope.calculatePenalty(2, true), 1580.0);

            // updateAvailability round-trip.
            dao.updateAvailability("M002", true);
            eq("findAvailable count after enabling projector", dao.findAvailable().size(), 3);
        }
    }

    private static void createSchema(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE equipment ("
                    + "equipment_id TEXT PRIMARY KEY,"
                    + "name TEXT NOT NULL,"
                    + "category TEXT NOT NULL,"
                    + "daily_rate REAL NOT NULL,"
                    + "replacement_value REAL NOT NULL,"
                    + "pricing_policy TEXT NOT NULL,"
                    + "available INTEGER DEFAULT 1)");
        }
    }

    // --- tiny assertion helpers ------------------------------------------

    private static void eq(String label, double actual, double expected) {
        checks++;
        boolean ok = Math.abs(actual - expected) < EPS;
        if (!ok) {
            failures++;
        }
        System.out.printf("[%s] %s: expected %.4f, got %.4f%n", ok ? "PASS" : "FAIL", label, expected, actual);
    }

    private static void eq(String label, int actual, int expected) {
        checks++;
        boolean ok = actual == expected;
        if (!ok) {
            failures++;
        }
        System.out.printf("[%s] %s: expected %d, got %d%n", ok ? "PASS" : "FAIL", label, expected, actual);
    }

    private static void check(String label, boolean ok) {
        checks++;
        if (!ok) {
            failures++;
        }
        System.out.printf("[%s] %s%n", ok ? "PASS" : "FAIL", label);
    }

    private static void expectIllegalArg(String label, Runnable action) {
        checks++;
        try {
            action.run();
            failures++;
            System.out.printf("[FAIL] %s: expected IllegalArgumentException, none thrown%n", label);
        } catch (IllegalArgumentException expected) {
            System.out.printf("[PASS] %s%n", label);
        }
    }
}
