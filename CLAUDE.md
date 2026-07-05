# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Status: feature-complete and runnable

This is a university group assignment (CCP6224 OOAD, MERIT — Smart Equipment Rental & Billing System) with a hard deadline of 2026-07-08 and a per-member individual defense. It is split by member A/B/C/D (see the team plan). **All four modules are implemented, integrated, and pass their smoke tests;** `merit.Main` launches the Swing app end-to-end against a code-seeded SQLite DB. The demo happy-path below is verified working.

What remains are **non-code deliverables** (not in this repo, still required): the three mandatory UML diagrams — Use Case, Class, Sequence (see the PDF rubric, 15 marks) — and packaging as `StudentNames.zip`. The code side of the rubric (all 4 features + Bridge pattern + future-proof OO design) is done. Marks also depend on the individual Q&A, which no code can satisfy.

The docs below are the binding spec; treat them as the source of truth for rates/rules.

**Deliverables status** (rubric-aligned; keep updated):
- [x] Source code — all 4 modules built, integrated, runnable via `merit.Main`
- [x] Feature Fulfillment (20) — equipment/rental mgmt, fee/discount/penalty, detailed billing, Swing UI
- [x] Design Pattern (10) — Bridge (`Equipment` × `PricingPolicy`), see Architecture
- [x] Future-proof OO design (15) — layering, polymorphism, single category/role switch
- [ ] **UML diagrams (15)** — Use Case, Class, Sequence: **not in repo, still required**
- [ ] **Packaging** — `StudentNames.zip` (source + diagrams; exclude `bin/`, `db/`)
- [ ] Q&A (40) — per-member individual defense; no code artifact

- `docs/MERIT-team-execution-plan.md` — module ownership (per-member), timeline, integration contracts, deliverables. Written in Chinese.
- `docs/MERIT-assumptions-and-data-dictionary.md` — assumptions register + SQLite schema/data dictionary + seed data. This defines the exact rates, discounts, penalty formulas, and table columns.

When implementing, do not invent rates/rules — pull the numbers from the data dictionary.

## Tech stack & build

- **Java + Swing** (desktop GUI), **SQLite via `sqlite-jdbc`** for persistence. Plain Java project (no Maven/Gradle) — the `sqlite-jdbc` jar lives in `lib/` and must be on the classpath.
- `.gitignore` excludes `*.class`, `*.jar`, and `*.zip` — so the JDBC jar is git-ignored; ensure teammates download it or document the download step. Build output (`bin/`) and the db file (`db/`) are excluded from the submission zip.

Run from **Git Bash** (`$(find ...)` is not PowerShell — PowerShell equivalents are in `HOW-TO-RUN.md`). Windows classpath separator is `;` (use `:` on macOS/Linux). The `sqlite-jdbc` jar must be in `lib/` (git-ignored; download once — see `HOW-TO-RUN.md` §2).

```bash
# run the app (creates + seeds db/merit.db on first run)
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.Main

# run the smoke tests — model/pricing/penalty/service compile against only the JDK, so NO jar needed
javac -d bin $(find src test -name "*.java")
java -cp bin EquipmentSmokeTest      # A: Bridge/Strategy pricing & penalty
java -cp bin BillGeneratorSmokeTest  # C: billing formula (722.00 / 699.60 / 710.80)
java -cp bin UserRentalSmokeTest     # B: polymorphic discount, due/late math, availability toggle
```

Verification is **home-grown smoke tests**, not a framework (no JUnit): each is a `main` with hand-rolled `[PASS]/[FAIL]` assertions that exits non-zero on failure. There is **no "run a single test"** — edit/add cases directly in the relevant `main`. Any change to the domain/service should keep all three green. `merit.dao.Database` also has a `main` that prints seeded row counts (needs the jar). A harmless JDK native-access `WARNING` from sqlite-jdbc can be silenced with `java --enable-native-access=ALL-UNNAMED ...`.

## Architecture (layered, OO-by-design)

Strict layering — the defense hinges on it, so preserve these boundaries:

- **UI never runs SQL; the domain never imports Swing.** UI → service → DAO → SQLite. `merit.Main` is the only place concrete implementations are wired; everything downstream depends on interfaces.
- Packages: `merit.model` (domain), `merit.pricing`, `merit.penalty`, `merit.service` (`AuthService`, `RentalManager`, `BillGenerator`), `merit.dao`, `merit.ui` (`LoginFrame` → role-routes to `AdminFrame` or `RentalFrame`), `merit.Main`.

Key design elements that require cross-file understanding:

- **Bridge pattern** decouples `Equipment` (category hierarchy: `ElectronicsEquipment` / `MediaEquipment` / `LabEquipment`) from `PricingPolicy` (`StandardPricing` = `rate×days`, `PromotionalPricing` = `×0.8`). Pricing is the *truly independent axis* — any category can take any pricing policy, and it persists in its own `pricing_policy` column. This is the marquee pattern for the defense; keep the two axes independent.
- **PenaltyPolicy** (`StandardPenalty` / `LabPenalty` / `ElectronicsPenalty`) is, by contrast, determined by category (no `penalty_policy` DB column) and constructed inside each `Equipment` subclass. Be aware this is a known tension with the "any equipment, any penalty" ideal — it's an accepted trade-off, not a bug to "fix" unless explicitly asked.
- **Polymorphic discount**: `User` (`Admin`/`Staff`/`Student`) exposes `getDiscountRate()`; `BillGenerator` calls it without any `instanceof`/`if(role)` switching.
- **The only place `category` is switched on** is `merit.model.EquipmentFactory` (category → correct `Equipment` subclass + its default penalty), reused by *both* DAO reconstruction and the admin add-equipment flow — so the switch exists exactly once. The analogous `role` → `User`-subclass switch lives only in `UserDaoSqlite` reconstruction. Business/service/UI logic must contain no such switch. Keep it that way.

## Domain invariants (do not get these wrong)

- **Billing formula:** `net = base − discount + penalty`, where `discount = base × user.getDiscountRate()`. The **discount applies to `base` only, never to `penalty`.** Do not write `(base + penalty) × rate`.
- Bills must display **base / discount / penalty / net as separate lines** ("detailed billing" is a rubric requirement).
- Money is stored as `double`, rounded to 2 dp only for display (accepted academic-scope trade-off vs `BigDecimal`).
- Rental duration: default 14 days, max 30 (validation cap only — no renew feature). Validate days > 0.
- Passwords are stored in plaintext (academic scope); login exists for **role routing** (Admin → admin panel; Staff/Student → rental flow), not security.
- The database is **schema-created and seeded from code on first run** (`Database.java`) so the demo always has data. `Database.java` is the day-1 critical path — the other DAOs depend on the tables existing.

## Data model

Four tables: `users`, `equipment`, `rentals`, `bills`. Relationships: `users 1 ──< rentals >── 1 equipment`, and `rentals 1 ── 1 bills`. SQLite types: `TEXT` / `REAL` / `INTEGER` (booleans as 0/1, dates as ISO strings via `LocalDate.toString()`). Full column definitions and seed data (8 equipment items incl. one PROMOTIONAL and one out-of-stock; 4 accounts covering every role) are in `docs/MERIT-assumptions-and-data-dictionary.md` and are already reproduced verbatim in `Database.java` (schema via `CREATE TABLE IF NOT EXISTS`, seed only when empty — both idempotent). If you change a rate/seed, change it in both places.

**Stock quantity:** `equipment` holds `total_quantity` + `available_quantity` (not a boolean). `isAvailable()`/`findAvailable()` mean `available_quantity > 0`. Admin sets the quantity when adding equipment and restocks via "Update Quantity". `RentalManager` persists both counts through `EquipmentDao.updateQuantities(id, total, available)`.

**Rent quantity:** a rental can take multiple units — `RentalManager.rent(id, user, equipment, days, quantity)` validates `1 ≤ K ≤ available` and decrements available by `K` (return restores `K`, capped at total). `Rental` and `Bill` carry `quantity`, the `rentals`/`bills` tables each have a `quantity` column, and billing scales `base` and `penalty` by `K` (discount is still base-only). The rental UI's Quantity spinner max is clamped to the selected item's `available_quantity`.

## Demo happy-path (the flow to keep working)

Student logs in → rents a promotional Electronics item (sees 20% off) → returns it 2 days late and damaged (sees Electronics damage surcharge) → itemized bill shows base/discount/penalty/net. Admin logs in → adds equipment → it appears in the catalog. Any change should keep this path runnable end-to-end.
