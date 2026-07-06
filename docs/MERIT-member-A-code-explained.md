# MERIT — Member A Code Explained (Equipment & the Bridge Pattern)

**Owner:** Member A · **Module M1 — Equipment & Rules** · **Nominated pattern you defend: Bridge**

This document explains every class you (Member A) own, why it is built this way, how it maps to the design docs, and what to say in the individual Q&A. Read it top to bottom once, then keep §11 (the cheat sheet) for the interview.

> Sources this implements: `Ignore/MERIT-design-decisions.md` (§2, §4), `docs/MERIT-assumptions-and-data-dictionary.md` (§1.5–1.7, Part 2), `Ignore/MERIT-implementation-plan.md` (§1, Phase 1). If any of those change, this code and doc must change with them.

---

## 1. What you own

| Package | Files | Role |
|---------|-------|------|
| `merit.model` | `Category`, `Equipment` (abstract), `ElectronicsEquipment`, `MediaEquipment`, `LabEquipment` | The equipment hierarchy — the **abstraction** side of the Bridge |
| `merit.pricing` | `PricingPolicy` (interface), `StandardPricing`, `PromotionalPricing` | Bridge **implementor** axis 1 (pricing) |
| `merit.penalty` | `PenaltyPolicy` (interface), `StandardPenalty`, `LabPenalty`, `ElectronicsPenalty` | Bridge **implementor** axis 2 (penalty) |
| `merit.dao` | `EquipmentDao` (interface), `EquipmentDaoSqlite`, `DataAccessException` | Persistence for equipment (DAO layering, **not** the nominated pattern) |
| `test/merit` | `EquipmentSmokeTest` | Standalone verification harness (no JUnit); excluded from the app |

You also own the **Class Diagram** (Bridge labelled) and the **design-pattern + OO-pillars** report section.

---

## 2. File / directory map

```
src/merit/
├── model/
│   ├── Category.java              enum: ELECTRONICS, MEDIA, LAB
│   ├── Equipment.java             abstract base — has-a PricingPolicy + PenaltyPolicy
│   ├── ElectronicsEquipment.java  extends Equipment (default ElectronicsPenalty)
│   ├── MediaEquipment.java        extends Equipment (default StandardPenalty)
│   └── LabEquipment.java          extends Equipment (default LabPenalty)
├── pricing/
│   ├── PricingPolicy.java         interface: calculateCharge(dailyRate, days)
│   ├── StandardPricing.java       rate × days
│   └── PromotionalPricing.java    rate × days × 0.8
├── penalty/
│   ├── PenaltyPolicy.java         interface: calculatePenalty(rate, replValue, daysLate, damaged)
│   ├── StandardPenalty.java       late 0.5×rate×daysLate + damage 0.3×replValue   (Media)
│   ├── LabPenalty.java            late 1.0×rate×daysLate + damage 0.3×replValue    (Lab)
│   └── ElectronicsPenalty.java    late 0.5×rate×daysLate + damage 0.4×replValue    (Electronics)
└── dao/
    ├── EquipmentDao.java          interface: add / findById / findAll / findAvailable / updateAvailability
    ├── EquipmentDaoSqlite.java    JDBC impl + the category→subclass reconstruction factory
    └── DataAccessException.java   unchecked wrapper so callers never see SQLException

test/merit/EquipmentSmokeTest.java   run this to verify the module in isolation
lib/sqlite-jdbc-3.42.0.0.jar         JDBC driver (see §13 for the version note)
```

---

## 3. Build & run (from the project root)

```sh
# compile production code + the smoke test, targeting Java 17
javac --release 17 -cp "lib/*" -d bin $(find src test -name "*.java")

# run the verification harness  (Windows: swap ':' for ';' in the classpath)
java -cp "lib/*:bin" merit.EquipmentSmokeTest
```

Member A's classes have **no `main`** in the app itself — `merit.Main` (Member D) wires everything together. The smoke test is your private proof the module works before integration.

---

## 4. The Bridge pattern — the heart of your module

**Intent (memorise, GoF wording):** *"Decouple an abstraction from its implementation so that the two can vary independently."*

MERIT equipment varies along **two independent axes**:

1. **Category** — Electronics / Media / Lab (the `Equipment` subclass hierarchy).
2. **Rules** — how it is priced and how it is penalised (`PricingPolicy` / `PenaltyPolicy`).

If you tried to capture both axes with inheritance alone, you would need a subclass for every *combination* — `PromoPricedLabEquipment`, `StandardPricedPromoPenaltyElectronics`, … the "class explosion" the lecture warns about. Bridge splits the axes: the `Equipment` **has-a** policy object (composition/delegation) instead of **being** a hard-coded combination.

```
        ABSTRACTION                              IMPLEMENTOR (axis 1)
   ┌────────────────────────┐   has-a    ┌──────────────────────────┐
   │ Equipment (abstract)   │──────────▶ │ «interface» PricingPolicy │
   │  id, name, dailyRate,  │            │  calculateCharge(rate,days)│
   │  replacementValue,     │            └───────────┬───────────────┘
   │  available             │              StandardPricing   PromotionalPricing
   │  pricing:  PricingPolicy│
   │  penalty:  PenaltyPolicy│  has-a     IMPLEMENTOR (axis 2)
   │  +calculateRentalCharge()│─────────▶ ┌──────────────────────────┐
   │  +calculatePenalty()     │           │ «interface» PenaltyPolicy │
   └──────────┬─────────────┘             │  calculatePenalty(...)     │
     extends  │                           └───────────┬───────────────┘
   ┌──────────┼───────────┐         StandardPenalty  LabPenalty  ElectronicsPenalty
Electronics  Media      Lab
Equipment   Equipment  Equipment
```

**The delegation, in one place (`Equipment`):**

```java
public double calculateRentalCharge(int days) {
    return pricingPolicy.calculateCharge(dailyRate, days);   // no pricing maths lives here
}
public double calculatePenalty(int daysLate, boolean damaged) {
    return penaltyPolicy.calculatePenalty(dailyRate, replacementValue, daysLate, damaged);
}
```

That's the whole trick: `Equipment` never contains an `if`/`switch` on category or rule — it just **delegates** to whichever policy object it was given.

---

## 5. File-by-file walkthrough

### `Category` (enum)
Three values: `ELECTRONICS`, `MEDIA`, `LAB`. Used only as data (stored in the DB, returned by `getCategory()`). It is **not** used for `switch`-based behaviour anywhere in business logic — the single exception is the DAO reconstruction factory (§6).

### `PricingPolicy` + implementations (Bridge implementor, axis 1)
- `PricingPolicy` — one method, `double calculateCharge(double dailyRate, int days)`. This is a **frozen Phase-0 contract** shared with Member C's billing.
- `StandardPricing` → `dailyRate * days`.
- `PromotionalPricing` → `dailyRate * days * 0.8` (20% off).

Pricing is the axis that is **truly independent of category**: the DB `pricing_policy` column decides it per item, so a promotional laptop and a promotional microscope share the *same* `PromotionalPricing` behaviour. This is the cleanest evidence your Bridge has two genuinely independent axes.

### `PenaltyPolicy` + implementations (Bridge implementor, axis 2)
- `PenaltyPolicy` — one method, `double calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged)`. Also a frozen Phase-0 contract.
- `StandardPenalty` (Media) → late `0.5×rate×daysLate` + damage `0.3×replacementValue`.
- `LabPenalty` (Lab) → late `1.0×rate×daysLate` (full rate — lab gear is booked on a schedule, so lateness blocks others) + damage `0.3×replacementValue`.
- `ElectronicsPenalty` (Electronics) → late `0.5×rate×daysLate` + damage `0.4×replacementValue` (+10% surcharge — electronics are costlier to repair).

Each penalty adds the damage fee only when `damaged` is true; an on-time, undamaged return is always `0`.

### `Equipment` (abstract) + 3 subclasses
`Equipment` holds the shared state (`id`, `name`, `dailyRate`, `replacementValue`, `available`) plus the two policy references, and exposes the two delegating methods above. Key design points to defend:

- **Abstraction** — `abstract`, so you can never `new Equipment(...)`; callers work through the base type.
- **Encapsulation** — all fields `private`; the constructor validates (`dailyRate >= 0`, `replacementValue >= 0`, policies non-null), and `calculateRentalCharge`/`calculatePenalty` validate their inputs (`days > 0`, `daysLate >= 0`).
- **Inheritance** — the three subclasses share all of that and add only their identity.
- **Polymorphism** — `getCategory()` is `abstract` and overridden per subclass; and the two calculate-methods run different policy behaviour depending on the injected object.
- **How the subclasses differ:** each subclass constructor injects **its category's default `PenaltyPolicy`** and accepts the `PricingPolicy` as a parameter. So category fixes the penalty default, while pricing stays free — exactly the two-axis split.

```java
public class LabEquipment extends Equipment {
    public LabEquipment(String id, String name, double dailyRate, double replacementValue,
                        boolean available, PricingPolicy pricingPolicy) {
        super(id, name, dailyRate, replacementValue, available, pricingPolicy, new LabPenalty());
    }
    @Override public Category getCategory() { return Category.LAB; }
}
```

### `EquipmentDao` + `EquipmentDaoSqlite` + `DataAccessException`
- `EquipmentDao` (interface) — `add`, `findById`, `findAll`, `findAvailable`, `updateAvailability`. Everyone (Admin UI, catalog, rental service) depends on this interface, never on the SQLite class.
- `EquipmentDaoSqlite` — the JDBC implementation. It takes a `java.sql.Connection` in its constructor (Member C's `Database` supplies the real one at integration; the smoke test supplies an in-memory one). It uses `PreparedStatement` for every query (safe, parameterised) and maps rows with `mapRow` (§6).
- `DataAccessException` — an unchecked exception that wraps `SQLException`, so the interface and its callers never import `java.sql.*`. That one-way dependency (UI/service → DAO interface, never JDBC) is what keeps the database swappable and is a talking point for "separation of concerns."

> **Note on layering:** the DAO/Repository pattern is *architectural* layering, **not** your nominated GoF pattern. If an examiner asks "what design pattern did you apply?", the answer is **Bridge** — mention DAO only as good layering.

### `EquipmentSmokeTest`
A dependency-free harness (`public static void main`) that asserts the documented numbers and the DAO round-trip, printing `PASS`/`FAIL` and exiting non-zero on any failure. It builds its own in-memory SQLite schema, so it does **not** wait on Member C. Keep it as evidence you tested your module in isolation (Phase 1's "unit-check"); you may exclude it from the final zip.

---

## 6. The one place category is allowed to branch (know this cold)

The design promises **no `if (category == …)` in business logic**. There is exactly one deliberate exception, and it is *not* business logic — it is the DAO's object-relational reconstruction factory, `EquipmentDaoSqlite.mapRow(...)`:

```java
switch (category) {
    case ELECTRONICS: return new ElectronicsEquipment(id, name, rate, replValue, available, pricing);
    case MEDIA:       return new MediaEquipment(id, name, rate, replValue, available, pricing);
    case LAB:         return new LabEquipment(id, name, rate, replValue, available, pricing);
}
```

**Model answer if challenged ("you said no category switch — but here's one!"):**
> "The *only* place we branch on category is the DAO's reconstruction factory, which turns a flat database row back into the correct polymorphic object. That is a necessary boundary between a relational table and an object graph. No business logic — pricing, penalty, billing, UI — ever switches on category; they all go through polymorphism and the injected policies."

The DAO also maps the `pricing_policy` column (`STANDARD`/`PROMOTIONAL`) to the right `PricingPolicy` on read, and back to the column value on write.

---

## 7. OO pillars — where each one lives (report table)

| Pillar | In your code | One-line proof |
|--------|--------------|----------------|
| **Abstraction** | `abstract class Equipment`, `interface PricingPolicy`, `interface PenaltyPolicy` | Can't instantiate `Equipment`; callers code to interfaces |
| **Inheritance** | `Equipment` → `Electronics/Media/Lab` | Subclasses share state + delegation |
| **Polymorphism** | `getCategory()` override; `calculateRentalCharge`/`calculatePenalty` run the injected policy | Same call, different behaviour by runtime type — no category switch |
| **Encapsulation** | private fields + constructor/method validation | State only changes through validated methods |
| **Aggregation/Composition** | `Equipment` **has-a** `PricingPolicy`/`PenaltyPolicy` (delegation) | The Bridge link — swappable at runtime |

---

## 8. Integration points — the contracts other members build on

Freeze these first; then everyone compiles independently behind them.

| Contract you provide | Consumed by | Signature |
|----------------------|-------------|-----------|
| `PricingPolicy.calculateCharge(double, int)` | Member C (billing) via `Equipment.calculateRentalCharge` | returns base fee |
| `PenaltyPolicy.calculatePenalty(double, double, int, boolean)` | Member C via `Equipment.calculatePenalty` | returns penalty |
| `Equipment` getters (`getId`, `getName`, `getDailyRate`, `getCategory`, `isAvailable`, …) | Members C, D (bill + UI display) | read-only view |
| `Equipment.setAvailable(boolean)` | Member B (rent/return toggles availability) | mutate state |
| `EquipmentDao` interface | Members B & D (catalog, admin, rental service) | CRUD boundary |
| `EquipmentDaoSqlite(Connection)` | Member C's `Database` supplies the `Connection` | constructor injection |

**What you rely on from others:** only a JDBC `Connection` (Member C's `Database` creates the schema + supplies it). Everything else in M1 is self-contained.

---

## 9. Formula quick-reference

```
Pricing
  Standard      = dailyRate × days
  Promotional   = dailyRate × days × 0.8

Penalty  (late fee)                 (damage fee, only if damaged)
  Standard(Media)     0.5×rate×late   + 0.3×replacementValue
  Lab                 1.0×rate×late   + 0.3×replacementValue
  Electronics         0.5×rate×late   + 0.4×replacementValue
```

These feed Member C's bill: `net = base − (base × userDiscount) + penalty` (discount applies to **base only**).

---

## 10. The penalty-policy design decision (and how to upgrade)

**What was chosen (Option 1, matches the committed data dictionary):** penalty policy is the **category default**, injected by each subclass's constructor. There is **no `penalty_policy` DB column** — on load, the DAO gives each item its category's default penalty. Pricing remains the fully independent, DB-driven axis.

Why this is still a valid Bridge: the code *structure* is unchanged either way — `Equipment` **has-a** `PenaltyPolicy` interface and delegates to it. Only the *source* of the policy differs (category default vs. a DB column).

**If you want the stronger demo (Option 2 — "any equipment, any penalty"):**
1. Add a `penalty_policy TEXT` column to the `equipment` table (coordinate with Member C's schema + seed).
2. Add a `PenaltyPolicy` parameter to each `Equipment` subclass constructor (drop the hard-coded `new XxxPenalty()`), or add a `penaltyFor(String)` mapper in the DAO like `pricingFor`.
3. Seed one deliberately "mismatched" row (e.g. an Electronics item with `StandardPenalty`) to show on-screen that the two axes are independent.

Either is defensible; Option 1 is the lower-risk choice for the deadline and needs zero changes from Members B/C/D.

---

## 11. Interview cheat sheet (your §6 row + guaranteed questions)

- **Which pattern and why?** Bridge — two independent axes (category × pricing/penalty); delegation decouples them so each extends without touching the other (Open/Closed).
- **Show me polymorphism.** `calculateRentalCharge` runs `StandardPricing` or `PromotionalPricing` depending on the object injected — same call site, different result. Also `getCategory()` overrides.
- **Show me abstraction / composition.** `abstract Equipment`; it *has-a* `PricingPolicy`/`PenaltyPolicy` rather than *being* a fixed combination.
- **Why not put pricing in each Equipment subclass?** Binds category to pricing → class explosion + you'd edit existing classes for each new rule (breaks Open/Closed). Delegation lets rules be swapped/added freely.
- **Bridge vs Adapter vs Strategy?** Adapter retrofits existing incompatible code (we're greenfield); Strategy varies *one* algorithm and isn't in the allowed list; we have *two* independent axes → Bridge.
- **You said "no category switch" — explain the DAO.** See §6 model answer: the reconstruction factory is a table→object boundary, not business logic.
- **Add a "Sports" category next semester?** One new `SportsEquipment extends Equipment`; pricing, penalty, billing, and UI are untouched.
- **Downside of your design?** More classes/indirection up front — the accepted cost of the extensibility the rubric rewards.

---

## 12. Verification (smoke-test evidence)

Command: `java -cp "lib/*:bin" merit.EquipmentSmokeTest`

Result — **all 19 checks pass** (captured 2026-07-05):

```
[PASS] StandardPricing 40x3: expected 120.0000, got 120.0000
[PASS] PromotionalPricing 10x3: expected 24.0000, got 24.0000
[PASS] LabPenalty 2 late + damaged: expected 1580.0000, got 1580.0000
[PASS] LabPenalty on-time undamaged: expected 0.0000, got 0.0000
[PASS] ElectronicsPenalty 2 late + damaged: expected 1015.0000, got 1015.0000
[PASS] StandardPenalty 2 late + damaged: expected 1080.0000, got 1080.0000
[PASS] Promo tablet 3-day charge: expected 24.0000, got 24.0000
[PASS] days <= 0 rejected
[PASS] negative daysLate rejected
[PASS] negative dailyRate rejected
[PASS] findAll count: expected 3, got 3
[PASS] findAvailable count (projector unavailable): expected 2, got 2
[PASS] E002 rebuilt as ElectronicsEquipment
[PASS] E002 promotional charge after reload: expected 24.0000, got 24.0000
[PASS] E002 electronics penalty after reload: expected 610.0000, got 610.0000
[PASS] L001 rebuilt as LabEquipment
[PASS] L001 standard charge after reload: expected 120.0000, got 120.0000
[PASS] L001 lab penalty after reload: expected 1580.0000, got 1580.0000
[PASS] findAvailable count after enabling projector: expected 3, got 3

ALL PASSED (19 checks)
```

(On JDK 25 you'll also see a harmless `WARNING: ... System::load ... SQLiteJDBCLoader` line when the driver loads its native library — it does not appear on JDK 17 and does not affect results.)


The checks cover: both pricing formulas, all three penalty rules (incl. on-time = 0), the abstraction's delegation to an injected promotional policy, input validation (rejects `days<=0`, `daysLate<0`, negative rate), and a full DAO round-trip (`add` → `findById`/`findAll`/`findAvailable` → correct subclass rebuilt → promotional pricing and category penalty survive the reload → `updateAvailability`).

---

## 13. Environment notes (important for the whole team)

- **JDK:** code is compiled with `--release 17` (project target). The lab machine here happens to run JDK 25, which compiles/runs it fine, but always pass `--release 17` so nobody accidentally uses a newer-than-17 API.
- **SQLite driver version — real gotcha:** we use **`sqlite-jdbc-3.42.0.0.jar`**, the last *self-contained* release. Versions **3.43.0.0 and newer (incl. 3.45.x) add a hard SLF4J dependency** — with only the sqlite jar on the classpath the driver's class initializer throws `NoClassDefFoundError: org/slf4j/LoggerFactory`, which surfaces confusingly as `SQLException: No suitable driver found`. If the team prefers 3.45.x, they must also add `slf4j-api-2.0.x.jar` (and, to silence the warning, `slf4j-nop-2.0.x.jar`) to `lib/`. Staying on 3.42.0.0 keeps `lib/` to a single jar — simpler for four people on plain `javac`.
- **Classpath separator:** `:` on macOS/Linux, `;` on Windows.
```
