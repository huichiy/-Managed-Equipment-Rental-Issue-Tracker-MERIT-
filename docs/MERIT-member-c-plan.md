# 📋 Member C 执行计划 — Billing + Database 地基

> 本文档是 Member C 的详细执行计划。配套 spec:
> - `docs/MERIT-assumptions-and-data-dictionary.md`(schema / seed / 费率的**唯一权威来源**,数字一律照抄)
> - `docs/MERIT-team-execution-plan.md`(分工与排期)
>
> C 的口号:**谁掌管全组依赖的 DB 地基,谁就得最早出货。** `Database.java` 是 Day-1 关键路径。

---

## 一、为什么现在轮到 C —— 它是全组的地基

- A 的 `EquipmentDaoSqlite` 已写完,但它接一个 `Connection` 去查 `equipment` 表——**目前没有任何代码建这张表**,所以 A 的 DAO 能编译却跑不了真实读写。
- 团队计划(第 81 行)白纸黑字:`Database.java`(建表 + seed)**必须第一天出**,因为 A 和 B 的 DAO 冒烟测试都要靠它先把表建好、把种子数据塞进去。
- 结论:C 的 `Database.java` 一交付,就同时解锁了 A 的 DAO 真实验证 + B 的全部 DAO 开发。**这是当前依赖链上收益最高的一步。**

### C 的任务其实分两半,依赖不同 —— 关键要拆开做

| 半区 | 内容 | 依赖 | 何时做 |
|------|------|------|--------|
| **C-1(地基)** | `Database.java`:连接 + 建 4 张表 + seed | **零依赖**(只用 JDK `java.sql` + jar) | **立刻,最高优先级** |
| **C-2(计费)** | `Bill` + `BillGenerator` + `BillDao(Sqlite)` | 依赖 **B 的 `User` / `Rental`** 和 A 的 `Equipment` | B 出了 `Rental`/`User` 之后,或先按冻结签名写壳 |

> 本计划先把 **C-1 打穿**(独立、能马上验证),C-2 的接口先冻结、实现待 B 落地。

---

## 二、前置动作(开写前必须搞定)

1. **拿到 `sqlite-jdbc.jar` 放进 `lib/`。**
   - 现在 `lib/` 是空的,`.gitignore` 又排除 `*.jar` → jar 不会进 git。C 要么把 jar 传给全组,要么在 README 写清下载步骤(Maven Central:`org.xerial:sqlite-jdbc`)。
   - **没有这个 jar,`Database.java` 能编译(只 import `java.sql`)但连不上库、跑不起来。** 这是 C-1 能否验证的硬前提。
2. 确认 `db/` 目录被 `.gitignore` 排除(数据库文件不进提交包)——已排除,`Database.java` 首次运行时自己建这个目录。
3. 和全组对齐 **`BillDao` 接口签名**(Phase 0 冻结契约),对齐 **`BillGenerator.generate(...)` 的入参**(它吃 B 的 `Rental`)。

---

## 三、要创建的文件(共 4 个)

### src/merit/dao/

| 文件 | 说明 |
|------|------|
| `Database.java` | 连接管理 + 建表(`CREATE TABLE IF NOT EXISTS`)+ 幂等 seed。提供 `getConnection()` 给所有 DAO 用。**day-1 关键路径。** |
| `BillDao.java` | 接口(Phase 0 全组冻结共用) |
| `BillDaoSqlite.java` | 接 `Connection`,负责 `bills` 表的 insert / 查询 |

### src/merit/model/ 与 src/merit/service/

| 文件 | 说明 |
|------|------|
| `merit/model/Bill.java` | 值对象:`base / discount / penalty / net` 各行 + 元数据。**组合关系**(账单 own 这几段金额 → 实心菱形)。 |
| `merit/service/BillGenerator.java` | `generate(rental)`:调 A 的 `Equipment` + B 的 `User.getDiscountRate()` 算出四段,产出 `Bill`。**多态折扣的施加点。** |

---

## 四、C-1:`Database.java` 详细设计

### 4.1 职责边界

- **只做三件事:** 建连接、建表、seed。不写业务逻辑,不 import 任何 model/service。
- 对外暴露 `Connection getConnection()`(或 `static Connection connect()`),让 A/B/C 各自的 `*DaoSqlite` 构造函数接这个 `Connection`——**DAO 不 import `Database`,只 import JDK 的 `java.sql.Connection`**(和 A 现有写法一致,保持解耦)。

### 4.2 关键实现点(别踩坑)

- **JDBC URL:** `jdbc:sqlite:db/merit.db`。首次运行前先 `new File("db").mkdirs()`,否则连接会因目录不存在而失败。
- **幂等建表:** 全部用 `CREATE TABLE IF NOT EXISTS`,重复运行不炸。
- **幂等 seed:** seed 前先 `SELECT COUNT(*) FROM users`(或 equipment),**只有空表才插种子数据**,否则每次启动会重复插入 / 撞主键。
- **外键:** SQLite 默认不强制外键,建连接后执行 `PRAGMA foreign_keys = ON;`(想让 `rentals`/`bills` 的 FK 生效的话)。
- **日期:** 一律 ISO 字符串(`LocalDate.toString()` → `"2026-07-05"`),对应 schema 里的 TEXT。
- **布尔:** 存 0/1(`available`、`final_year`、`damaged`、`returned`)。
- **金额:** REAL(`double`)。四舍五入只在 UI 显示时做,入库存原值。

### 4.3 建表 DDL —— 照抄数据字典 Part 2,一列都不能改

```sql
CREATE TABLE IF NOT EXISTS users (
    user_id    TEXT PRIMARY KEY,
    username   TEXT UNIQUE NOT NULL,
    password   TEXT NOT NULL,
    name       TEXT NOT NULL,
    role       TEXT NOT NULL,              -- ADMIN | STAFF | STUDENT
    final_year INTEGER DEFAULT 0           -- 1 = 毕业年学生(10% 折扣)
);

CREATE TABLE IF NOT EXISTS equipment (
    equipment_id      TEXT PRIMARY KEY,
    name              TEXT NOT NULL,
    category          TEXT NOT NULL,       -- ELECTRONICS | MEDIA | LAB
    daily_rate        REAL NOT NULL,
    replacement_value REAL NOT NULL,
    pricing_policy    TEXT NOT NULL,       -- STANDARD | PROMOTIONAL(Bridge 独立轴)
    available         INTEGER DEFAULT 1
);

CREATE TABLE IF NOT EXISTS rentals (
    rental_id    TEXT PRIMARY KEY,
    user_id      TEXT NOT NULL,
    equipment_id TEXT NOT NULL,
    rental_days  INTEGER NOT NULL,
    rent_date    TEXT NOT NULL,
    due_date     TEXT NOT NULL,
    return_date  TEXT,                     -- 未还为 NULL
    days_late    INTEGER DEFAULT 0,
    damaged      INTEGER DEFAULT 0,
    returned     INTEGER DEFAULT 0,
    FOREIGN KEY (user_id)      REFERENCES users(user_id),
    FOREIGN KEY (equipment_id) REFERENCES equipment(equipment_id)
);

CREATE TABLE IF NOT EXISTS bills (
    bill_id     TEXT PRIMARY KEY,
    rental_id   TEXT NOT NULL,
    base_fee    REAL NOT NULL,
    discount    REAL NOT NULL,
    penalty     REAL NOT NULL,
    net_payable REAL NOT NULL,
    created_at  TEXT NOT NULL,
    FOREIGN KEY (rental_id) REFERENCES rentals(rental_id)
);
```

### 4.4 Seed 数据 —— 照抄数据字典 Part 3

**设备(8 项;E002 是唯一 PROMOTIONAL、M002 是唯一 available=0):**

| equipment_id | name | category | daily_rate | replacement_value | pricing_policy | available |
|---|---|---|---|---|---|---|
| E001 | Laptop | ELECTRONICS | 15.00 | 2500.00 | STANDARD | 1 |
| E002 | Tablet | ELECTRONICS | 10.00 | 1500.00 | **PROMOTIONAL** | 1 |
| M001 | DSLR Camera | MEDIA | 30.00 | 3500.00 | STANDARD | 1 |
| M002 | Projector | MEDIA | 25.00 | 2000.00 | STANDARD | **0** |
| M003 | Microphone | MEDIA | 10.00 | 400.00 | STANDARD | 1 |
| M004 | Tripod | MEDIA | 5.00 | 200.00 | STANDARD | 1 |
| L001 | Microscope | LAB | 40.00 | 5000.00 | STANDARD | 1 |
| L002 | Oscilloscope | LAB | 50.00 | 8000.00 | STANDARD | 1 |

**账户(4 个,覆盖所有角色;密码 = 用户名,明文):**

| user_id | username | password | name | role | final_year |
|---|---|---|---|---|---|
| U001 | admin | admin | Facilities Admin | ADMIN | 0 |
| U002 | staff | staff | Dr. Tan | STAFF | 0 |
| U003 | student | student | Regular Student | STUDENT | 0 |
| U004 | finalyear | finalyear | Final-Year Student | STUDENT | 1 |

> `rentals` / `bills` **不 seed**(运行时由租借流程产生)。

---

## 五、C-2:Bill + BillGenerator + BillDao(依赖 B)

### 5.1 计费公式 —— 这里最容易被扣分,逐字对齐

```
base     = equipment.calculateRentalCharge(days)          // 走 A 的 PricingPolicy
discount = base × user.getDiscountRate()                  // ⚠️ 只作用于 base
penalty  = equipment.calculatePenalty(daysLate, damaged)  // 走 A 的 PenaltyPolicy
net      = base − discount + penalty
```

- ⚠️ **折扣只打 base,绝不打 penalty。** 别写成 `(base + penalty) × rate`。
- ⚠️ `BillGenerator` 里**不准出现 `if(user instanceof ...)` 或 `if(role)`**——折扣靠 `user.getDiscountRate()` 多态拿。这是 C 答辩的多态卖点。
- `Bill` 必须把 **base / discount / penalty / net 四行分开存、分开显示**(rubric 明确要 "detailed billing output")。

### 5.2 冻结对照数(和 A 的冒烟测试同源,方便端到端校验)

E002 Tablet(PROMOTIONAL,rate=10,replacement=1500),租 14 天、逾期 2 天、损坏:

```
base    = 10 × 14 × 0.8            → 112.00
discount(普通学生 0%) = 112 × 0    →   0.00
discount(staff 20%)   = 112 × 0.2  →  22.40
penalty = 0.5×10×2 + 0.4×1500      → 610.00
net(普通学生) = 112 − 0 + 610      → 722.00
net(staff)   = 112 − 22.40 + 610   → 699.60
```

### 5.3 BillDao

- `BillDao` 接口:至少 `void insert(Bill bill)`;按需加 `findByRentalId` / `findAll`。
- `BillDaoSqlite`:接 `Connection`,把 `Bill` 写进 `bills` 表(`created_at` 用 `LocalDateTime.now().toString()` 或 `Instant`)。
- 和 A 的 DAO 同款:构造接 `Connection`,SQL 不外泄,`SQLException` 包成 `RuntimeException`。

---

## 六、验证方式

### C-1 验证(不依赖任何人,立刻能做)

写一个临时 `main`(或复用 A 的冒烟测试模式)——**这一步需要 `lib/sqlite-jdbc.jar` 就位**:

```bash
# Git Bash;带上 jar
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src -name "*.java")
java  -cp "bin;lib/sqlite-jdbc.jar" merit.dao.DatabaseSmokeMain   # 临时验证入口
```

断言点:
1. 首次运行:`db/merit.db` 被创建,4 张表存在,`users`=4 行、`equipment`=8 行。
2. **再跑一次**:行数仍是 4 / 8(幂等 seed 生效,没有重复插入 / 撞主键)。
3. `SELECT * FROM equipment WHERE pricing_policy='PROMOTIONAL'` → 只有 E002。
4. `SELECT * FROM equipment WHERE available=0` → 只有 M002。
5. **交叉验证 A 的 DAO**:`new EquipmentDaoSqlite(Database.getConnection()).findAvailable()` 返回 7 项(排除 M002),且 E002 重建出来带 `PromotionalPricing`。这一步同时证明 A、C 两块接上了。

### C-2 验证(等 B 的 Rental/User 出来后)

用 B 的 `Rental`(student 租 E002,14 天,逾期 2 天,损坏)喂给 `BillGenerator.generate(...)`,断言 §5.2 的 722.00;换 staff 账户断言 699.60。

---

## 七、依赖与交接顺序(一句话记住)

```
C-1 (Database.java)  ← 现在做,零依赖,解锁所有人
      ↓ 建好表 + seed
A 的 EquipmentDao 真实跑通  +  B 开始写 UserDao/RentalDao
      ↓ B 交出 User / Rental
C-2 (Bill + BillGenerator + BillDao)  ← 计费收口
      ↓
D 的 UI 调 BillGenerator 显示逐条账单
```

---

## 八、答辩要点(C 必背)

1. **分层架构 / 为什么 DAO 要隔离 SQL:** UI 不跑 SQL、域不 import Swing;把 SQL 关在 DAO 层 → 域/服务与数据库解耦,换库或换存储不动业务代码。
2. **账单四段怎么算:** 背 §5.1 公式 + "折扣只打 base" 的理由(折扣是 `User` 的属性,penalty 是设备损坏/逾期的后果,两者不该混)。
3. **组合关系:** `Bill` 组合 base/discount/penalty/net(它们不脱离 Bill 独立存在 → 实心菱形);对比 B 的 `Rental` 聚合(空心菱形)。
4. **`double` 存钱的 gotcha:** "本 scope 用 double + 显示时四舍五入可接受,生产会用 `BigDecimal` 避免浮点误差。"
5. **首次运行 seed 的理由:** demo 永远有数据,不靠人工建库;幂等设计保证重复启动不脏数据。

---

## 九、Checklist

- [ ] `lib/sqlite-jdbc.jar` 就位 + 全组能拿到
- [ ] `Database.java`:连接 / 建 4 表(IF NOT EXISTS)/ 幂等 seed(空表才插)
- [ ] seed 数字逐格对齐数据字典 Part 3(8 设备 + 4 账户)
- [ ] 冒烟验证:两次运行行数不变 + A 的 `findAvailable()` 返回 7 项
- [ ] `BillDao` 接口 Phase 0 冻结、全组对齐
- [ ] `Bill.java`(四段分行)
- [ ] `BillGenerator`:公式对、无 `instanceof`、折扣只打 base
- [ ] `BillDaoSqlite`:写 `bills` 表
- [ ] C-2 端到端断言 722.00 / 699.60
