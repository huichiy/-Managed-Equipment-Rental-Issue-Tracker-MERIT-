# MERIT — Assumptions Register & Data Dictionary

**课程:** CCP6224 OOAD Final Assignment · **系统:** MERIT — Smart Equipment Rental & Billing System

> **用途:** 这份文档有两个身份——(1)**假设登记表**,满足作业要求的 "clearly state and justify any assumptions";(2)**数据字典**,作为 Database Diagram(ER 图)的文字搭档。两部分都直接放进 **Report**。
>
> **需不需要 data dictionary?** 老师列的交付物里没有单独的 data dictionary,所以不是硬性必交件。但既然本次要做数据库 + ER 图 + 报告,把它收进报告是低成本、高回报的——它补全 ER 图的字段说明,也给报告提供实打实的内容。

---

## Part 1 — Assumptions Register(假设登记表)

> 以下数值均为**本组的合理假设**,非题目硬性规定。作业允许自定,关键是能 justify。面试时照这份讲即可。

### 1.1 角色 / Actors

| 角色 | 说明 | 折扣资格 |
|------|------|----------|
| Admin | Campus Facilities 操作员,管理设备目录、处理归还 | 不适用(不租借) |
| Staff | 学术/行政职员 | 20% |
| Student(regular) | 在册学生 | 0% |
| Student(final-year) | 毕业年学生,用 `final_year` 标记 | 10% |

> Final-year 不是新角色,而是 `Student` 上的 `final_year` 布尔标记(10% 折扣)。折扣仍由 `Student.getDiscountRate()` 多态返回 `finalYear ? 0.10 : 0`,DAO 重建时只有一个 `case "STUDENT"`。

**Justify:** 登录后按 role 路由到不同界面(Admin → 管理界面;Staff/Student → 租借界面)。登录是**角色访问控制**的手段,非安全功能;明文密码为 academic scope 的已知取舍。

### 1.2 日租费率 Daily Rental Rate

| Category | Item(example) | Daily rate (RM) |
|----------|---------------|-----------------|
| Electronics | Laptop | 15.00 |
| Electronics | Tablet | 10.00 |
| Media | DSLR Camera | 30.00 |
| Media | Projector | 25.00 |
| Media | Microphone | 10.00 |
| Media | Tripod | 5.00 |
| Lab | Microscope | 40.00 |
| Lab | Oscilloscope | 50.00 |

**Justify:** 费率按真实设备价值梯度设定(实验室仪器 > 影像设备 > 消耗型配件),体现 "different categories may apply different pricing"。

### 1.3 折扣率 Discount Rate

| User type | Discount | Applied to |
|-----------|----------|------------|
| Student (regular) | 0% | — |
| Student (final-year) | 10% | **Base rental fee** |
| Staff | 20% | **Base rental fee** |

**⚠️ 计费要点:** 折扣**只打在 base 上,不打在 penalty 上**。对应公式 `net = base − discount + penalty`——`discount = base × rate`,penalty 原样加。别写成 `(base+penalty) × rate`。
**Justify:** 折扣资格是 `User` 的属性,通过 `getDiscountRate()` 多态实现,在 `BillGenerator` 里施加,不混进 pricing policy。

### 1.4 租期 Rental Duration

| 项目 | 值 |
|------|-----|
| Default rental duration | 14 days |
| Maximum rental duration | 30 days |

**Justify:** 参照马来西亚大学(UM/USM/MMU)一般物品 7–14 天、项目类(FYP、影像项目)可延至 30 天的惯例,一个月为现实上限。
**落地:** 用作 `RentalPanel` 中 `JSpinner` 的默认值(14)与上限(30),强化 input validation。**注意:30 天只是校验上限,不实现"续租/renew"功能**(需求无此项,避免 scope creep)。

### 1.5 罚金规则 Penalty Rules

| Policy | 适用类别 | Late fee | Damage fee |
|--------|----------|----------|------------|
| `StandardPenalty` | Media | `0.5 × dailyRate × daysLate` | `0.3 × replacementValue` |
| `LabPenalty` | Lab | `1.0 × dailyRate × daysLate`(安全关键) | `0.3 × replacementValue` |
| `ElectronicsPenalty` | Electronics | `0.5 × dailyRate × daysLate` | `0.4 × replacementValue`(+10% 加价) |

**Justify:** 不同类别罚则不同,体现 "different categories may apply different penalty rules"。Lab 逾期罚更重,因其常有预约排期。

### 1.6 定价策略 Pricing Policies(Bridge 独立轴)

| Policy | 规则 |
|--------|------|
| `StandardPricing` | `dailyRate × days` |
| `PromotionalPricing` | `dailyRate × days × 0.8`(20% 促销) |

**Justify:** pricing 独立于 category——任何类别的设备都可配 Standard 或 Promotional,这是 Bridge 模式"两轴独立"中**真正独立**的那条轴,持久化时独立存于 `pricing_policy` 列。

### 1.7 账单公式 Billing Formula

```
base    = equipment.calculateRentalCharge(days)     // 经 PricingPolicy
discount = base × user.getDiscountRate()            // 只作用于 base
penalty = equipment.calculatePenalty(daysLate, damaged)  // 经 PenaltyPolicy
net     = base − discount + penalty
```

账单需**逐行显示** base / discount / penalty / net(rubric 要求 "detailed billing output")。

### 1.8 其他假设

- **金额用 `double`**,仅在**显示时**四舍五入到 2 位小数。(面试若问:生产环境会用 `BigDecimal` 避免浮点误差,本 scope 可接受。)
- **密码明文存储**(academic scope,非安全实现)。
- **首次运行时以代码 seed 数据库**,保证 demo 有数据。
- **penalty 由 category 决定**(见 Part 2 说明),不设独立的 `penalty_policy` 列。
- **设备按库存数量管理**:每件设备有 `total_quantity`(总库存)与 `available_quantity`(当前可租)。`available_quantity > 0` 才可租、才出现在目录。admin 新增设备时设定初始数量,并可事后 restock 调整总量。此设计取代了早期的布尔 `available`(等价于 `available_quantity > 0`)。
- **一次可租多件**:租借时可选数量 `K`(UI 的 Quantity spinner 上限自动等于该设备 `available_quantity`,选不超)。租出 `available −= K`、归还 `+= K`(不超过总库存);账单的 `base` 与 `penalty` 均 `× K`,`discount` 仍只作用于 base。`rentals` 与 `bills` 各存一列 `quantity`。

---

## Part 2 — Data Dictionary(SQLite Schema)

> 与 Database Diagram(ER 图)配套。四张表:`users`、`equipment`、`rentals`、`bills`。
> 类型说明:SQLite 用 `TEXT`(字符串)、`REAL`(浮点)、`INTEGER`(整数;布尔以 0/1 表示)。日期以 ISO 字符串存(`LocalDate.toString()`)。

### 2.1 `users`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `user_id` | TEXT | **PK** | 用户唯一标识 |
| `username` | TEXT | UNIQUE, NOT NULL | 登录名 |
| `password` | TEXT | NOT NULL | 明文密码(academic scope) |
| `name` | TEXT | NOT NULL | 姓名 |
| `role` | TEXT | NOT NULL | `ADMIN` \| `STAFF` \| `STUDENT` |
| `final_year` | INTEGER | NOT NULL, DEFAULT 0 | 1 = 毕业年学生(10% 折扣),仅对 `STUDENT` 有意义 |

### 2.2 `equipment`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `equipment_id` | TEXT | **PK** | 设备唯一标识 |
| `name` | TEXT | NOT NULL | 设备名称 |
| `category` | TEXT | NOT NULL | `ELECTRONICS` \| `MEDIA` \| `LAB`;**决定重建的子类 + 默认 penalty policy** |
| `daily_rate` | REAL | NOT NULL | 日租金(RM) |
| `replacement_value` | REAL | NOT NULL | 重置价值(RM),用于损坏罚金计算 |
| `pricing_policy` | TEXT | NOT NULL | `STANDARD` \| `PROMOTIONAL`;**Bridge 的独立轴** |
| `total_quantity` | INTEGER | NOT NULL DEFAULT 0 | 库存总数(admin 设定 / restock) |
| `available_quantity` | INTEGER | NOT NULL DEFAULT 0 | 当前可租数量;租出 −1,归还 +1;目录只列 `> 0` 的项 |

> **设计说明(面试可讲):** `category` 在 DAO 重建对象时映射到对应 `Equipment` 子类,并附带该类的默认 `PenaltyPolicy`;`pricing_policy` 则独立映射到 `PricingPolicy`。这是全系统**唯一按 category 分支**的地方(DAO 重建工厂),业务逻辑中无此类 switch。penalty 未单列字段,因其由 category 决定。

### 2.3 `rentals`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `rental_id` | TEXT | **PK** | 租借记录标识 |
| `user_id` | TEXT | NOT NULL, **FK → users(user_id)** | 租借人 |
| `equipment_id` | TEXT | NOT NULL, **FK → equipment(equipment_id)** | 所租设备 |
| `rental_days` | INTEGER | NOT NULL | 租借天数(1–30) |
| `quantity` | INTEGER | NOT NULL DEFAULT 1 | 本次租借的件数(`1 ≤ K ≤` 该设备 `available_quantity`) |
| `rent_date` | TEXT | NOT NULL | 起租日期(ISO) |
| `due_date` | TEXT | NOT NULL | 应还日期(ISO) |
| `return_date` | TEXT | 可空 | 实际归还日期;未还为 NULL |
| `days_late` | INTEGER | DEFAULT 0 | 逾期天数 |
| `damaged` | INTEGER | DEFAULT 0 | 1 = 归还时损坏 |
| `returned` | INTEGER | DEFAULT 0 | 1 = 已归还 |

### 2.4 `bills`

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `bill_id` | TEXT | **PK** | 账单标识 |
| `rental_id` | TEXT | NOT NULL, **FK → rentals(rental_id)** | 对应租借 |
| `quantity` | INTEGER | NOT NULL DEFAULT 1 | 件数(冗余自 rental,便于账单独立显示) |
| `base_fee` | REAL | NOT NULL | 基础租金(已 × 数量) |
| `discount` | REAL | NOT NULL | 折扣额(只基于 base) |
| `penalty` | REAL | NOT NULL | 罚金额(逾期 + 损坏) |
| `net_payable` | REAL | NOT NULL | 应付净额 = base − discount + penalty |
| `created_at` | TEXT | NOT NULL | 生成时间戳(ISO) |

**关系概览(给 ER 图):**
`users 1 ──< rentals >── 1 equipment`(一个用户/一件设备可有多条租借);`rentals 1 ── 1 bills`(每条租借对应一张账单)。

---

## Part 3 — Seed Data(启动时载入)

> `Database.java` 首次运行时插入。以下为建议默认值;`replacement_value` 与 `pricing_policy` 为本组假设,可自由调整。

### 3.1 设备(8 项,覆盖三类;含 1 个 promotional、1 个已租出用于展示 availability)

| equipment_id | name | category | daily_rate | replacement_value | pricing_policy | total_quantity | available_quantity |
|--------------|------|----------|-----------|-------------------|----------------|----------------|--------------------|
| E001 | Laptop | ELECTRONICS | 15.00 | 2500.00 | STANDARD | 5 | 5 |
| E002 | Tablet | ELECTRONICS | 10.00 | 1500.00 | **PROMOTIONAL** | 3 | 3 |
| M001 | DSLR Camera | MEDIA | 30.00 | 3500.00 | STANDARD | 2 | 2 |
| M002 | Projector | MEDIA | 25.00 | 2000.00 | STANDARD | 2 | 0 |
| M003 | Microphone | MEDIA | 10.00 | 400.00 | STANDARD | 4 | 4 |
| M004 | Tripod | MEDIA | 5.00 | 200.00 | STANDARD | 6 | 6 |
| L001 | Microscope | LAB | 40.00 | 5000.00 | STANDARD | 2 | 2 |
| L002 | Oscilloscope | LAB | 50.00 | 8000.00 | STANDARD | 1 | 1 |

> **为什么 E002 设为 PROMOTIONAL:** demo 时租它可展示 20% 折扣,也证明 pricing 独立于 category。
> **为什么 M002 设为 available_quantity=0(库存 2 但 0 可租):** 展示库存耗尽的设备不出现在租借目录里(目录只列 `available_quantity > 0`)。

### 3.2 账户(4 个,覆盖所有角色)

| user_id | username | password | name | role | final_year |
|---------|----------|----------|------|------|------------|
| U001 | admin | admin | Facilities Admin | ADMIN | 0 |
| U002 | staff | staff | Dr. Tan | STAFF | 0 |
| U003 | student | student | Regular Student | STUDENT | 0 |
| U004 | finalyear | finalyear | Final-Year Student | STUDENT | 1 |

---

## 附:这份文档在交付物里的位置

- **Report** — Part 1 放"Assumptions"章节;Part 2 放"Database Design / Data Dictionary"章节。
- **Database Diagram(ER)** — Part 2 的字段与关系即 ER 图的依据。
- **面试** — Part 1 用来 justify 数值选择;Part 2 的 `category` / `pricing_policy` 说明用来讲 Bridge 与 DAO 边界。
