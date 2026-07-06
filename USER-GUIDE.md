# User Guide — MERIT (by Role)

Smart Equipment Rental & Billing System. This guide explains what each type of user can do
and how to do it. To start the app, see `HOW-TO-RUN.md`.

---

## Getting in: Login & Register

When the app starts, the **Login** window appears.

- **Log in:** type a username and password, click **Login**. You are routed to the right
  screen for your role (see below).
- **Register a new account:** click **Register**, fill in **Name / Username / Password /
  Student ID**, pick a **Role** (Student · Final-year Student · Staff), and submit. You're
  returned to the login screen to sign in. *(Admin accounts cannot be self-registered — they
  are pre-seeded.)*

### Student ID rule (for Student registrations)
The **Student ID** is validated when you register as a Student:
- It must be a valid format — it starts with a 3-digit batch, e.g. `243UC246W0`.
- **Final-year** is only accepted if your ID is in the final-year batch (**starts with
  `243`**). Picking "Final-year Student" with a non-`243` ID is rejected, and you're asked to
  correct the form.
- A regular Student may use any valid ID. Staff registrations ignore the Student ID.

*(The ID is used for validation only — it is not stored.)*

### Seeded accounts (username = password)

| Username | Role | Goes to |
|----------|------|---------|
| `admin` | Admin | Admin panel |
| `staff` | Staff | Rental screen |
| `student` | Student (regular) | Rental screen |
| `finalyear` | Student (final-year) | Rental screen |

---

## The roles at a glance

| Role | Can do | Rental discount |
|------|--------|-----------------|
| **Admin** | Manage the equipment catalog (add items, restock quantity). Does **not** rent. | — |
| **Staff** | Browse, rent, and return equipment; gets a bill. | **20%** off the base fee |
| **Student** (regular) | Browse, rent, and return equipment; gets a bill. | **0%** |
| **Student** (final-year) | Same as student. | **10%** off the base fee |

The discount is applied automatically at billing time based on your role — you never choose it.

---

## Admin — managing the catalog

After logging in as an admin you see **"Admin — Equipment Catalog"**: a table of every
equipment item with its **ID, Name, Category, Rate, Replacement value, Pricing, Total** stock
and **Available** stock, plus a form along the bottom.

### Add a new equipment item
1. Fill in the form: **ID**, **Name**, **Category** (Electronics / Media / Lab),
   **Rate** (daily rate), **Replacement** (replacement value), **Pricing**
   (Standard or Promotional), and **Qty** (how many units).
2. Click **Add Equipment**.
3. The item appears in the table with all units available.

Rules the form enforces: ID and Name are required, the ID must be unique, Rate must be a
number `> 0`, Replacement `>= 0`, and Qty a whole number `>= 0`.

### Restock / change quantity
1. Click a row in the table to select an item.
2. Click **Update Quantity** and enter the new **total** quantity.
3. Available stock is recalculated automatically, keeping any units currently out on rental
   accounted for (e.g. if 5 total / 2 on loan and you set total to 8, available becomes 6).

> **Pricing note:** *Promotional* items are billed at 20% off the base rate for everyone.
> This is separate from the user discount and can be combined with it.

---

## Staff & Students — renting and returning

After logging in you see a window titled with your name and **two tabs**.

### Tab 1 — Catalog & Rent
1. The table lists every item that currently has stock available (**ID, Name, Category,
   Rate, Pricing, Available**).
2. Select the item you want.
3. Set **Days** (default 14, max 30) and **Quantity** (capped to the available stock).
4. Click **Rent selected**. A confirmation shows the **due date** (today + days), and the
   item's available stock drops by the quantity you took.

### Tab 2 — My Rentals & Return
1. This tab lists **your** active (not-yet-returned) rentals: **Rental ID, Equipment, Days,
   Due date**.
2. Select the rental you're returning.
3. Set **Days late** (0 if on time) and tick **Damaged** if the item came back damaged.
4. Click **Return & bill**. The stock is returned and an **itemised bill** pops up.

---

## Understanding your bill

When you return an item, the bill is shown as separate lines:

```
Base fee   : the rental charge  (daily rate × days × quantity, ×0.8 if Promotional)
Discount   : base × your role discount   (Staff 20% / final-year 10% / else 0%)
Penalty    : late fee + damage fee (only if late and/or damaged)
--------------------------------------------------
Net payable: base − discount + penalty
```

Key points:
- The **discount applies to the base fee only** — never to the penalty.
- **Penalty depends on the equipment category:**
  - *Lab* — late fee is the full daily rate per late day (lab gear is scheduled, so lateness
    blocks others); damage adds 30% of replacement value.
  - *Electronics* — late fee is half the daily rate per late day; damage adds 40% of
    replacement value (costlier to repair).
  - *Media* — late fee half the daily rate per late day; damage adds 30% of replacement value.
- An on-time, undamaged return has **no penalty** (penalty = 0).

### Worked example
Final-year student rents **E002 Tablet** (Electronics, Promotional, rate 10, replacement 1500)
for 14 days, returns it **2 days late and damaged**:

- Base = `10 × 14 × 0.8` = **112.00**
- Discount = `112 × 10%` = **11.20**
- Penalty = late `0.5 × 10 × 2` + damage `0.4 × 1500` = `10 + 600` = **610.00**
- **Net = 112 − 11.20 + 610 = 710.80**

---

## Logging out

Every screen has a top bar showing who you're logged in as and a **Logout** button that
returns you to the login screen.

---

*Rates, discounts, and penalty formulas are defined in
`docs/MERIT-assumptions-and-data-dictionary.md` (the source of truth). If the app and this
guide ever disagree, the data dictionary wins.*
