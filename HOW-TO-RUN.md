# How to Run — MERIT

Smart Equipment Rental & Billing System · Java + Swing + SQLite (via `sqlite-jdbc`).

> **Current state:** the full app is built and runnable. `merit.Main` launches the Swing
> UI (login → role-based Admin / Rental screens), backed by a code-seeded SQLite database.
> Self-registration is available from the login screen.

---

## 1. Prerequisites

| Need | Notes |
|------|-------|
| **JDK 17+** | Project targets Java 17 (`--release 17`). Check with `java -version`. Newer JDKs (e.g. 21/25) work fine. |
| **`sqlite-jdbc` jar in `lib/`** | Required to run the app (persistence). `.gitignore` excludes `*.jar`, so it is **not** committed — each teammate must place it. See §2. |
| **Terminal** | macOS/Linux: any shell. Windows: **Git Bash** for the `$(find ...)` commands, or use the PowerShell version in §6. |

**Classpath separator:** `:` on macOS/Linux, `;` on Windows. The examples below are macOS/Linux (`:`).

---

## 2. Get the SQLite JDBC jar

The jar is git-ignored, so obtain it once and drop it in `lib/`:

- Direct: https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/3.42.0.0/sqlite-jdbc-3.42.0.0.jar
- Or search Maven Central for `org.xerial:sqlite-jdbc`.
- Save it into `lib/` (e.g. `lib/sqlite-jdbc-3.42.0.0.jar`).

> **Use version 3.42.0.0.** It is self-contained. Versions 3.43.0.0+ add a hard SLF4J
> dependency and will fail with a confusing `No suitable driver found` unless you also add
> `slf4j-api` to `lib/`. Staying on 3.42.0.0 keeps `lib/` to a single jar.
>
> The commands below use the `lib/*` wildcard, so they work regardless of the exact jar filename.

---

## 3. Run the app

From the repo root:

```bash
# compile the whole project into bin/
javac --release 17 -cp "lib/*" -d bin $(find src test -name "*.java")

# launch the Swing app (creates + seeds db/merit.db on first run)
java -cp "lib/*:bin" merit.Main
```

A **Login** window opens. Log in with a seeded account, or click **Register** to create one.

### Seeded accounts (username = password, plaintext — academic scope)

| Username | Role | Discount | Lands on |
|----------|------|----------|----------|
| `admin` | Admin | — | Admin panel (add / restock equipment) |
| `staff` | Staff | 20% | Rental screen |
| `student` | Student (regular) | 0% | Rental screen |
| `finalyear` | Student (final-year) | 10% | Rental screen |

### Register a new account
From the login screen click **Register**, fill in Name / Username / Password, pick a role
(Student · Final-year Student · Staff), and submit. You're returned to login to sign in with
the new account. (Admin accounts are seed-only and cannot be self-registered.)

### Demo happy-path (good for the presentation)
Student logs in → rents the promotional item **E002 Tablet** (sees 20% off the base) →
returns it 2 days late and damaged (sees the Electronics damage surcharge) → itemised bill
shows **base / discount / penalty / net**. Then Admin logs in → adds equipment → it appears
in the catalog.

---

## 4. Run the tests

The smoke tests are plain `main` methods with hand-rolled `[PASS]/[FAIL]` assertions (no
JUnit). After compiling (§3), run each directly:

```bash
java -cp "lib/*:bin" EquipmentSmokeTest       # Bridge pricing & penalty          → 8 passed
java -cp "lib/*:bin" BillGeneratorSmokeTest   # billing formula                   → 12 passed
java -cp "lib/*:bin" UserRentalSmokeTest      # discounts, due/late, availability → 21 passed
```

All three should report `0 failed` (41 checks total). Any domain/service change should keep
them green.

You can also verify just the database seed:

```bash
java -cp "lib/*:bin" merit.dao.Database        # prints row counts; run twice → stays 4 users / 8 equipment
```

---

## 5. Resetting / troubleshooting

- **Fresh data:** `rm db/merit.db` — it re-seeds on the next run.
- **Harmless warning:** on JDK 21+ you may see `WARNING: ... System::load ... SQLiteJDBCLoader`
  when the driver loads its native library. It does not affect results. Silence it with:
  `java --enable-native-access=ALL-UNNAMED -cp "lib/*:bin" merit.Main`.
- **`No suitable driver found`:** the jar is missing from `lib/`, or you're on sqlite-jdbc
  3.43.0.0+ without SLF4J — see §2.
- **Recompile from scratch:** `rm -rf bin && javac --release 17 -cp "lib/*" -d bin $(find src test -name "*.java")`.

---

## 6. Windows (PowerShell) equivalents

PowerShell has no `$(find ...)` and uses `;` in the classpath:

```powershell
# compile
$src = Get-ChildItem -Recurse src,test -Filter *.java | ForEach-Object { $_.FullName }
javac --release 17 -cp "lib/*" -d bin $src

# run the app
java -cp "lib/*;bin" merit.Main

# run a test
java -cp "lib/*;bin" EquipmentSmokeTest
```

---

## 7. Notes

- `bin/` (build output) and `db/` (the SQLite file) are git-ignored and excluded from the
  submission zip.
- Source of truth for all rates, schema, and seed data:
  `docs/MERIT-assumptions-and-data-dictionary.md`.
- App entry point: `merit.Main`. Layering: UI → service → DAO → SQLite; only `merit.Main`
  wires the concrete implementations together.
