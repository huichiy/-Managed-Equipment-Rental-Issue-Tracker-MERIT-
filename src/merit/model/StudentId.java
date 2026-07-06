package merit.model;

/**
 * Student-ID batch rule. The first three characters of a student ID encode the
 * intake batch (e.g. {@code 243UC246W0} → batch {@code 243}). Batch {@code 243}
 * denotes a <b>final-year</b> student (10% rental discount); any other batch is a
 * regular student.
 *
 * <p>Centralised here so the rule lives in the domain layer — the UI and services
 * never hard-code the {@code "243"} literal, keeping the "UI holds no business
 * rule" boundary intact and letting a smoke test assert the rule directly.
 */
public final class StudentId {

    /** Batch prefix that marks a final-year student. */
    public static final String FINAL_YEAR_PREFIX = "243";

    private StudentId() { }

    /** Light format check: a non-empty ID whose first three characters are digits. */
    public static boolean isValidFormat(String id) {
        if (id == null) {
            return false;
        }
        String s = id.trim();
        return s.length() >= 3
                && Character.isDigit(s.charAt(0))
                && Character.isDigit(s.charAt(1))
                && Character.isDigit(s.charAt(2));
    }

    /** @return {@code true} if the ID belongs to the final-year batch ({@value #FINAL_YEAR_PREFIX}). */
    public static boolean isFinalYearBatch(String id) {
        return id != null && id.trim().startsWith(FINAL_YEAR_PREFIX);
    }
}
