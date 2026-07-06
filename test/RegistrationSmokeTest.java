import java.util.ArrayList;
import java.util.List;

import merit.dao.UserDao;
import merit.model.Student;
import merit.model.StudentId;
import merit.model.User;
import merit.service.AuthService;

/**
 * Pure-logic smoke test for self-registration + the final-year student-ID rule
 * (no database, no sqlite-jdbc jar needed). Verifies that the "243" batch prefix
 * gates the final-year discount and that regular students are unaffected.
 *
 * Run:  javac -d bin $(find src test -name "*.java")  &&  java -cp bin RegistrationSmokeTest
 */
public class RegistrationSmokeTest {

    private static int passed = 0;
    private static int failed = 0;

    /** In-memory UserDao so this test needs no database. */
    private static class FakeUserDao implements UserDao {
        final List<User> users = new ArrayList<>();
        public User findByUsername(String username) {
            for (User u : users) {
                if (u.getUsername().equals(username)) {
                    return u;
                }
            }
            return null;
        }
        public User findById(String id) { return null; }
        public List<User> findAll() { return users; }
        public void insert(User user) { users.add(user); }
    }

    public static void main(String[] args) {
        System.out.println("=== MERIT — Registration / final-year student-ID rule smoke test ===\n");

        // --- the domain rule in isolation ---
        assertTrue("243... is a final-year batch", StudentId.isFinalYearBatch("243UC246W0"));
        assertTrue("241... is NOT a final-year batch", !StudentId.isFinalYearBatch("241UC246W0"));
        assertTrue("valid format accepted", StudentId.isValidFormat("243UC246W0"));
        assertTrue("blank id rejected", !StudentId.isValidFormat("  "));
        assertTrue("non-digit prefix rejected", !StudentId.isValidFormat("UC2430"));

        AuthService auth = new AuthService(new FakeUserDao());

        // --- final-year student with a 243 id: accepted, 10% discount ---
        User fy = auth.register("fy", "pw", "Final Yee", "243UC246W0", "STUDENT", true);
        assertTrue("243 final-year registered", fy != null);
        assertTrue("registered as Student", fy instanceof Student);
        assertEquals("final-year discount", 0.10, fy.getDiscountRate());

        // --- final-year claim with a non-243 id: rejected ---
        try {
            auth.register("fake", "pw", "Fake FY", "241UC246W0", "STUDENT", true);
            fail("expected rejection: final-year claim without a 243 id");
        } catch (IllegalArgumentException expected) {
            pass("rejects final-year claim when id is not 243");
        }

        // --- a 243 student who registers as a regular student: allowed, 0% ---
        User reg243 = auth.register("reg243", "pw", "Reg", "243UC999X0", "STUDENT", false);
        assertTrue("243 student may register as regular", reg243 != null);
        assertEquals("regular student discount", 0.00, reg243.getDiscountRate());

        // --- student with an invalid id: rejected ---
        try {
            auth.register("bad", "pw", "Bad Id", "", "STUDENT", false);
            fail("expected rejection: empty student id");
        } catch (IllegalArgumentException expected) {
            pass("rejects student with an invalid id");
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
