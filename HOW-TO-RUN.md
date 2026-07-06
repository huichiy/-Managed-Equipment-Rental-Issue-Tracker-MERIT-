
# How to Run — MERIT

Smart Equipment Rental & Billing System · Java + Swing + SQLite (via `sqlite-jdbc`).

> **Current state (2026-07-05):** only part of the system is built. Member A's slice
> (`merit.model` / `merit.pricing` / `merit.penalty` / `merit.dao`) and the database
> bootstrap (`merit.dao.Database`) exist. There is **no `merit.Main` / Swing UI yet**,
> so the full app does not launch. What you *can* run today is the **smoke test** and the
> **database bootstrap** — see below.

---

## 1. Prerequisites

| Need | Notes |
|------|-------|
| **JDK 17+** | Uses `switch` expressions. Check with `java -version`. |
| **`sqlite-jdbc.jar` in `lib/`** | Required only for the DB parts. `.gitignore` excludes `*.jar`, so it is **not** in the repo — each teammate must place it. See §2. |
| **Git Bash** (Windows) | The `$(find ...)` command below is Bash syntax, not PowerShell. PowerShell equivalents are in §5. |

Classpath separator: **`;` on Windows**, `:` on macOS/Linux. Examples below use `;`.

---

## 2. Get the SQLite JDBC jar

The jar is git-ignored, so download it once and save it as `lib/sqlite-jdbc.jar`:

- Maven Central: search `org.xerial:sqlite-jdbc`, download the latest `.jar`.
- Direct: https://repo1.maven.org/maven2/org/xerial/sqlite-jdbc/
- Save it to: `lib/sqlite-jdbc.jar`

> No jar? The pure-logic smoke test (§3) still runs — it doesn't touch the database.
> Anything hitting SQLite (§4) will fail with a clear "put sqlite-jdbc.jar in lib/" message.

---

## 3. Run the smoke test (no jar needed)

The `model` / `pricing` / `penalty` / `dao` code compiles against only the JDK, so this
needs **no jar**. It asserts the exact seed-data billing numbers.

```bash
# from Git Bash, in the repo root
javac -d bin $(find src test -name "*.java")
java -cp bin EquipmentSmokeTest
```

Expected: `=== 8 passed, 0 failed ===`

---

## 4. Run the database bootstrap (needs the jar)

Creates `db/merit.db`, builds the 4 tables, and seeds 8 equipment + 4 users
(idempotent — safe to run repeatedly).

```bash
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.dao.Database
```

Expected output:

```
users rows = 4
equipment rows = 8
PROMOTIONAL items:
  E002 Tablet
Unavailable items:
  M002 Projector
```

**Run it twice** — the counts must stay `4 / 8`. That proves the seed is idempotent
(it only inserts into an empty table, so restarts never duplicate rows).

---

## 5. PowerShell equivalents (Windows, no Git Bash)

PowerShell has no `$(find ...)`. Gather the sources first, then compile:

```powershell
# smoke test (no jar)
$src = Get-ChildItem -Recurse src,test -Filter *.java | ForEach-Object { $_.FullName }
javac -d bin $src
java -cp bin EquipmentSmokeTest

# database bootstrap (needs lib\sqlite-jdbc.jar)
$src = Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName }
javac -cp "lib\sqlite-jdbc.jar" -d bin $src
java -cp "bin;lib\sqlite-jdbc.jar" merit.dao.Database
```

---

## 6. Running the full app (once it exists)

When Member D adds `merit.Main` and the Swing UI, launch with:

```bash
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.Main
```

**Demo happy-path:** Student logs in → rents the promotional Electronics item (E002 Tablet,
sees 20% off) → returns it 2 days late and damaged (sees the Electronics damage surcharge)
→ itemised bill shows base / discount / penalty / net. Admin logs in → adds equipment →
it appears in the catalog.

Default login accounts (username = password, plaintext — academic scope):

| username | role | discount |
|----------|------|----------|
| `admin` | ADMIN | — |
| `staff` | STAFF | 20% |
| `student` | STUDENT (regular) | 0% |
| `finalyear` | STUDENT (final-year) | 10% |

---

## 7. Notes

- `bin/` (build output) and `db/` (the SQLite file) are git-ignored and excluded from the
  submission zip. Delete `db/merit.db` if you want a clean re-seed.
- Never run `javac`/`find` from PowerShell with the Bash `$(...)` form — use §5 instead.
- Spec / source of truth for all rates, schema, and seed data:
  `docs/MERIT-assumptions-and-data-dictionary.md`.
