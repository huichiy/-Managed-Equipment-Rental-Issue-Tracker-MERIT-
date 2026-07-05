# How to Run — MERIT

Smart Equipment Rental & Billing System · Java + Swing + SQLite (via `sqlite-jdbc`).

> **State:** all four modules are built and the app is runnable end-to-end —
> `merit.Main` launches the Swing login screen. Layering is strict: UI → service → DAO → SQLite.

---

## 1. Prerequisites

| Need | Notes |
|------|-------|
| **JDK 17+** | Uses `switch` expressions. Check with `java -version`. |
| **`sqlite-jdbc.jar` in `lib/`** | Required to run anything that touches the DB (the app + the DB bootstrap). `.gitignore` excludes `*.jar`, so it is **not** in the repo — each teammate places it once. See §2. |
| **Git Bash** (Windows) | The `$(find ...)` commands below are Bash syntax. PowerShell equivalents are in §6. |

Classpath separator: **`;` on Windows**, `:` on macOS/Linux. Examples below use `;`.

---

## 2. Get the SQLite JDBC jar (one-time)

The jar is git-ignored, so download it once and save it as `lib/sqlite-jdbc.jar`:

```bash
mkdir -p lib
curl -L -o lib/sqlite-jdbc.jar "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.47.1.0/sqlite-jdbc-3.47.1.0.jar"
```

PowerShell:
```powershell
New-Item -ItemType Directory -Force lib | Out-Null
Invoke-WebRequest "https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.47.1.0/sqlite-jdbc-3.47.1.0.jar" -OutFile lib\sqlite-jdbc.jar
```

---

## 3. Run the app

```bash
# from Git Bash, in the repo root
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.Main
```

On first run `merit.dao.Database` creates `db/merit.db`, builds the 4 tables, and seeds
8 equipment + 4 accounts. The login window opens.

> A harmless `WARNING: ... restricted method ... System::load` may print — it's a JDK
> native-access notice from sqlite-jdbc, not an error. Silence it with
> `java --enable-native-access=ALL-UNNAMED -cp "bin;lib/sqlite-jdbc.jar" merit.Main`.

### Login accounts (username = password, plaintext — academic scope)

| username | role | discount | lands on |
|----------|------|----------|----------|
| `admin` | ADMIN | — | Admin catalog panel |
| `staff` | STAFF | 20% | Rental flow |
| `student` | STUDENT | 0% | Rental flow |

### Demo happy-path

1. Log in as **`student`**.
2. **Catalog & Rent** tab → the **Available** column shows remaining stock per item. Select **E002 Tablet** (PROMOTIONAL). Set **Days** (default 14) and **Quantity** — the Quantity spinner's max is clamped to that item's available stock (E002 seeds with 3, so you can pick 1–3). → **Rent selected**. The Available count drops by the quantity you rented (it only leaves the catalog once available hits 0).
3. **My Rentals & Return** tab → select the rental → set **Days late = 2**, tick **Damaged** → **Return & bill**.
4. The itemised bill shows **Quantity** plus **Base / Discount / Penalty / Net**. Renting 1 → Base 112.00 / Discount 0.00 / Penalty 610.00 / **Net 722.00**; renting 2 scales base & penalty → **Net 1444.00** (promotional 20% off the base; Electronics damage surcharge in the penalty).
5. Log out, log in as **`admin`** → the catalog shows **Total** and **Available** columns. Use **Add Equipment** (with a **Qty** field for initial stock) to add an item, or select a row and **Update Quantity** to restock. Changes appear immediately.

---

## 4. Run the smoke tests (no jar needed)

The `model` / `pricing` / `penalty` / `service` logic compiles against only the JDK, so
these run with **no jar**. Each prints `[PASS]/[FAIL]` and exits non-zero on failure.

```bash
javac -d bin $(find src test -name "*.java")
java -cp bin EquipmentSmokeTest      # A: Bridge/Strategy pricing & penalty
java -cp bin BillGeneratorSmokeTest  # C: billing formula (722.00 / 699.60 / 710.80)
java -cp bin UserRentalSmokeTest     # B: polymorphic discount, due/late, stock & rent quantity
```

> There is no JUnit — these are plain `main` methods with hand-rolled assertions.
> To add a case, edit the relevant `main`.

---

## 5. Run the database bootstrap directly (optional, needs the jar)

Useful to confirm the DB seeds correctly without opening the UI:

```bash
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.dao.Database
```

Prints `users rows = 4` / `equipment rows = 8` plus the PROMOTIONAL (E002) and
out-of-stock (M002, `available_quantity = 0`) items. Run it twice — the counts stay
`4 / 8` (idempotent seed).

---

## 6. PowerShell equivalents (Windows, no Git Bash)

PowerShell has no `$(find ...)`. Gather the sources first, then compile:

```powershell
# run the app
$src = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "lib\sqlite-jdbc.jar" -d bin $src
java -cp "bin;lib\sqlite-jdbc.jar" merit.Main

# smoke tests (no jar)
$src = Get-ChildItem -Recurse src,test -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $src
java -cp bin EquipmentSmokeTest
java -cp bin BillGeneratorSmokeTest
java -cp bin UserRentalSmokeTest
```

---

## 7. Notes

- `bin/` (build output) and `db/` (the SQLite file) are git-ignored and excluded from the
  submission zip. **Delete `db/merit.db` for a clean re-seed — and you *must* delete it after
  any DB schema change.** Tables are created with `CREATE TABLE IF NOT EXISTS` and existing
  tables are never altered, so a stale DB fails with `no such column: ...`. **Close the app
  first** (a running app locks the file, so the delete will otherwise fail with "resource busy").
- Never run `javac`/`find` from PowerShell with the Bash `$(...)` form — use §6 instead.
- Architecture: UI never runs SQL; the domain never imports Swing. `merit.Main` is the only
  place concrete implementations are wired — everything downstream depends on interfaces.
- Spec / source of truth for all rates, schema, and seed data:
  `docs/MERIT-assumptions-and-data-dictionary.md`.
