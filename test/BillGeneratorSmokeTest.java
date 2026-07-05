import merit.model.Bill;
import merit.model.ElectronicsEquipment;
import merit.model.Equipment;
import merit.model.Rental;
import merit.model.User;
import merit.pricing.PromotionalPricing;
import merit.service.BillGenerator;

/**
 * Pure-logic smoke test for Member C-2 (no database, no sqlite-jdbc jar needed).
 * Asserts the exact billing numbers from the C-2 plan against the seed data.
 *
 * Run:  javac -d bin $(find src test -name "*.java")  &&  java -cp bin BillGeneratorSmokeTest
 */
public class BillGeneratorSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    /** Test double for Member B's User — C does not own Admin/Staff/Student. */
    private static class FakeUser extends User {
        private final double rate;
        FakeUser(String id, double rate) {
            super(id, id, id, id);
            this.rate = rate;
        }
        @Override public double getDiscountRate() { return rate; }
    }

    public static void main(String[] args) {
        System.out.println("=== MERIT Member C-2 — BillGenerator smoke test ===\n");
        BillGenerator gen = new BillGenerator();

        // E002 Tablet (rate=10, replacement=1500, PROMOTIONAL), 14 days, 2 late, damaged.
        Equipment tablet = new ElectronicsEquipment("E002", "Tablet", 10.0, 1500.0, true, new PromotionalPricing());

        // Case 1: regular student (0%) → net 722.00.
        Bill b1 = gen.generate(rental("R001", new FakeUser("U003", 0.00), tablet));
        assertEquals("regular base (promo 10*14*0.8)", 112.00, b1.getBaseFee());
        assertEquals("regular discount (0%)", 0.00, b1.getDiscount());
        assertEquals("regular penalty (0.5*10*2 + 0.4*1500)", 610.00, b1.getPenalty());
        assertEquals("regular net (112 - 0 + 610)", 722.00, b1.getNetPayable());

        // Case 2: staff (20%) → discount 22.40 on base only, net 699.60.
        Bill b2 = gen.generate(rental("R002", new FakeUser("U002", 0.20), tablet));
        assertEquals("staff discount (112*0.20, base only)", 22.40, b2.getDiscount());
        assertEquals("staff penalty unchanged by discount", 610.00, b2.getPenalty());
        assertEquals("staff net (112 - 22.40 + 610)", 699.60, b2.getNetPayable());

        // Case 3: quantity 2 scales base and penalty (regular student, 0%).
        Rental multi = new Rental("R004", new FakeUser("U003", 0.00), tablet, 14, null, 2);
        multi.setDaysLate(2);
        multi.setDamaged(true);
        Bill b4 = gen.generate(multi);
        assertEquals("qty2 base (112 x 2)", 224.00, b4.getBaseFee());
        assertEquals("qty2 penalty (610 x 2)", 1220.00, b4.getPenalty());
        assertEquals("qty2 net (224 - 0 + 1220)", 1444.00, b4.getNetPayable());
        assertEquals("qty recorded on bill", 2.0, b4.getQuantity());

        // billId is derived from the rental id (1-1).
        assertEqualsStr("billId derived from rentalId", "B-R001", b1.getBillId());

        System.out.printf("%n=== %d passed, %d failed ===%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static Rental rental(String id, User user, Equipment equipment) {
        Rental r = new Rental(id, user, equipment, 14);
        r.setDaysLate(2);
        r.setDamaged(true);
        return r;
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

    private static void pass(String msg) { passed++; System.out.println("  [PASS] " + msg); }
    private static void fail(String msg) { failed++; System.out.println("  [FAIL] " + msg); }
}
