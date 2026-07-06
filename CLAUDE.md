# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MERIT is a **CCP6224 OOAD final assignment** (academic, graded 40%) — a Java Swing equipment-rental & billing system. Grading is done **individually**: each team member owns a module they must be able to explain and defend in a solo Q&A. This affects how the code is written: it deliberately foregrounds textbook OO pillars and a nominated GoF pattern (**Bridge**), and comments are unusually explanatory because they double as exam prep. Preserve that intent — don't "clean up" pattern-illustrating comments or collapse the abstraction, and don't introduce `if (category == ...)` branching in business logic (see below).

The four-person division and schedule live in `docs/MERIT-team-execution-plan.md` (in Chinese). `docs/MERIT-member-A-code-explained.md` is the definitive deep-dive on the currently-implemented module and the Q&A model answers.

## Current state

Only **Member A's module** is in `src/` so far (Equipment hierarchy + pricing/penalty policies + Equipment DAO). Members B/C/D modules are planned but not yet committed:
- **B** — `merit.model` Users/`Rental`, `merit.service` `AuthService`/`RentalManager`, `UserDao`/`RentalDao`
- **C** — `merit.model` `Bill`, `merit.service` `BillGenerator`, `merit.dao` `Database` (schema + seed), `BillDao`
- **D** — all `merit.ui` Swing panels + `merit.Main` (the app entry point wiring everything together)

There is currently **no `main` in the app** — `merit.EquipmentSmokeTest` is the only runnable entry point.

## Build, run, test

Plain `javac` (no Maven/Gradle). Run from the project root. Java **17** is the target; always pass `--release 17` even on a newer JDK.

```sh
# compile production code + smoke test into bin/
javac --release 17 -cp "lib/*" -d bin $(find src test -name "*.java")

# run the smoke test (the "single test" for this module)
java -cp "lib/*:bin" merit.EquipmentSmokeTest
```

- `EquipmentSmokeTest` is a **dependency-free JUnit-less harness** (`public static void main`) that prints `PASS`/`FAIL` per check and exits non-zero on any failure. It builds its own in-memory SQLite schema, so it needs nothing from Member C. To run a subset, comment out calls in its `main`.
- **Classpath separator:** `:` on macOS/Linux, `;` on Windows.
- `bin/` and `*.jar` are gitignored; recompile after cloning.

### SQLite driver gotcha (do not "upgrade")

`lib/sqlite-jdbc-3.42.0.0.jar` is pinned deliberately — it is the last **self-contained** release. Versions **3.43.0.0+ add a hard SLF4J dependency**; with only the sqlite jar on the classpath the driver throws `NoClassDefFoundError: org/slf4j/LoggerFactory`, which surfaces confusingly as `SQLException: No suitable driver found`. Staying on 3.42.0.0 keeps `lib/` to a single jar.

## Architecture: the Bridge pattern

The whole design hinges on separating two **independent axes of variation** so neither causes a class explosion:

1. **Category** (abstraction side) — `abstract Equipment` → `ElectronicsEquipment` / `MediaEquipment` / `LabEquipment`.
2. **Rules** (implementor side) — `PricingPolicy` (`StandardPricing`, `PromotionalPricing`) and `PenaltyPolicy` (`StandardPenalty`, `LabPenalty`, `ElectronicsPenalty`).

`Equipment` **has-a** `PricingPolicy` and a `PenaltyPolicy` (composition/delegation) and its two calculate-methods contain **no maths** — they delegate:

```java
public double calculateRentalCharge(int days)      { return pricingPolicy.calculateCharge(dailyRate, days); }
public double calculatePenalty(int daysLate, ...)   { return penaltyPolicy.calculatePenalty(dailyRate, replacementValue, daysLate, damaged); }
```

- **Pricing is the truly independent axis**: assigned per item via the DB `pricing_policy` column, so any category can be Standard or Promotional.
- **Penalty is the category default** (Option 1 in the data dictionary): injected by each subclass constructor (`new LabPenalty()` etc.), with **no `penalty_policy` DB column**. This is a known, deliberate simplification of the "any equipment, any penalty" ideal — see `docs/MERIT-member-A-code-explained.md` §10 before changing it.

### The one permitted `switch` on category

`EquipmentDaoSqlite.mapRow(...)` is the **only** place that branches on category — it's the object-relational reconstruction factory turning a flat table row back into the right `Equipment` subclass. This is a table→object boundary, **not** business logic. Keep all other layers (pricing, penalty, billing, UI) category-agnostic via polymorphism. Adding a new category should mean one new `Equipment` subclass plus a `case` here — nothing else.

### DAO layering

`EquipmentDao` (interface) is the persistence boundary; callers depend only on it, never on JDBC. `EquipmentDaoSqlite` takes a `java.sql.Connection` via constructor injection (Member C's `Database` supplies the real one; the smoke test supplies an in-memory one). `DataAccessException` is an unchecked wrapper so `SQLException` / `java.sql.*` never leaks past the DAO. Note: DAO/Repository is **architectural layering, not** the nominated pattern — the nominated pattern is Bridge.

## Frozen integration contracts (Phase 0)

These signatures are shared across members and were frozen before implementation — treat them as stable APIs; changing one ripples to other members' modules:
- `PricingPolicy.calculateCharge(double dailyRate, int days)`
- `PenaltyPolicy.calculatePenalty(double dailyRate, double replacementValue, int daysLate, boolean damaged)`
- The DAO interfaces (`EquipmentDao`, and planned `UserDao`/`RentalDao`/`BillDao`)
- Planned service signatures: `AuthService`, `RentalManager`, `BillGenerator`

## Formula reference

```
Pricing:  Standard = rate×days        Promotional = rate×days×0.8
Penalty:              late fee              + damage fee (only if damaged)
  Standard (Media)    0.5×rate×daysLate     + 0.3×replacementValue
  Lab                 1.0×rate×daysLate     + 0.3×replacementValue
  Electronics         0.5×rate×daysLate     + 0.4×replacementValue
Bill (Member C):  net = base − (base × userDiscount) + penalty   (discount on base only)
```

Money uses `double` (rounded on display) — an accepted academic-scope tradeoff; production would use `BigDecimal`.
