import merit.model.ElectronicsEquipment;
import merit.model.Equipment;
import merit.model.LabEquipment;
import merit.pricing.PromotionalPricing;
import merit.pricing.StandardPricing;

/**
 * Pure-logic smoke test for Member A (no database, no sqlite-jdbc jar needed).
 * Asserts the exact numbers from claude2.md section four against the seed data.
 *
 * Run:  javac -d bin $(find src test -name "*.java")  &&  java -cp bin EquipmentSmokeTest
 */
public class EquipmentSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("=== MERIT Member A — Equipment / Bridge / Strategy smoke test ===\n");

        // Case 1: E002 Tablet (rate=10, replacement=1500, PROMOTIONAL), 14 days, 2 late, damaged.
        Equipment tablet = new ElectronicsEquipment("E002", "Tablet", 10.0, 1500.0, true, new PromotionalPricing());
        double base1 = tablet.calculateRentalCharge(14);
        double penalty1 = tablet.calculatePenalty(2, true);
        double net1 = base1 - 0.0 + penalty1;   // regular student → 0% discount
        assertEquals("E002 base (promotional 10*14*0.8)", 112.00, base1);
        assertEquals("E002 penalty (late 0.5*10*2 + damage 0.4*1500)", 610.00, penalty1);
        assertEquals("E002 net (regular student 0%)", 722.00, net1);

        // Case 2 (control): L001 Microscope, STANDARD — verify Lab late rate is 1.0x.
        Equipment microscope = new LabEquipment("L001", "Microscope", 40.0, 5000.0, true, new StandardPricing());
        assertEquals("L001 base (standard 40*14)", 560.00, microscope.calculateRentalCharge(14));
        assertEquals("L001 late penalty (lab 1.0*40*2, no damage)", 80.00, microscope.calculatePenalty(2, false));

        // Case 3: Bridge independence — Lab + PROMOTIONAL pricing still keeps the Lab penalty.
        Equipment promoLab = new LabEquipment("L009", "Promo Scope", 40.0, 5000.0, true, new PromotionalPricing());
        assertEquals("L009 base (lab + promotional 40*14*0.8)", 448.00, promoLab.calculateRentalCharge(14));
        assertEquals("L009 late penalty (still lab 1.0*40*3)", 120.00, promoLab.calculatePenalty(3, false));

        // Guard: non-positive rental days are rejected.
        try {
            tablet.calculateRentalCharge(0);
            fail("expected IllegalArgumentException for 0 days");
        } catch (IllegalArgumentException expected) {
            pass("rejects non-positive rental days");
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

    private static void pass(String msg) { passed++; System.out.println("  [PASS] " + msg); }
    private static void fail(String msg) { failed++; System.out.println("  [FAIL] " + msg); }
}
