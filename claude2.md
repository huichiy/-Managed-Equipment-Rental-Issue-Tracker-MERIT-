# 📋 Member A 最终计划 v2 — Equipment 继承体系 + Bridge/Strategy

> 本文档是 Member A 的执行计划(v2),在原版基础上整合了评审建议。
> 改动点用 🆕 标出,方便对照原版。配套 spec:`docs/MERIT-assumptions-and-data-dictionary.md`(数值权威来源)、`docs/MERIT-team-execution-plan.md`(分工)。

---

## 一、为什么先做 A —— 依赖最干净

- `model` / `pricing` / `penalty` 三包**零依赖**,纯 OO 逻辑,不碰 DB、不碰别人代码。
- 唯一碰 DB 的 `EquipmentDaoSqlite` 构造函数接 **`java.sql.Connection`**(JDK 自带),不 import C 的 `Database` → A 完全独立可编译。
- 结论:A 可独立完整交付,不需要造任何占位骨架。

---

## 二、要创建的文件(共 14 个)

### src/merit/model/

| 文件 | 说明 | 🆕 v2 补充 |
|---|---|---|
| `Category.java` | enum:ELECTRONICS / MEDIA / LAB | — |
| `Equipment.java` | 抽象类。字段:id、name、dailyRate、replacementValue、available、pricingPolicy、penaltyPolicy | 🆕 必须有 `getPricingPolicy()` getter(DAO 存回时要用);`calculateRentalCharge(days)` 里加 `if(days<=0) throw` |
| `ElectronicsEquipment.java` | 构造注入 pricing;penalty 固定 `ElectronicsPenalty` | — |
| `MediaEquipment.java` | penalty 固定 `StandardPenalty` | — |
| `LabEquipment.java` | penalty 固定 `LabPenalty` | — |

### src/merit/pricing/(Bridge 的独立轴)

| 文件 | 说明 | 🆕 v2 补充 |
|---|---|---|
| `PricingPolicy.java` | 接口:`double calculate(double dailyRate, int days)` | 🆕 **加一个 `String code()`** 返回 `"STANDARD"`/`"PROMOTIONAL"`,DAO 存回 `pricing_policy` 列时用它,避免 DAO 里写 `instanceof` |
| `StandardPricing.java` | `rate × days`;`code()` 返回 `"STANDARD"` | — |
| `PromotionalPricing.java` | `rate × days × 0.8`;`code()` 返回 `"PROMOTIONAL"` | — |

### src/merit/penalty/(Strategy,按类别选)

| 文件 | 说明 |
|---|---|
| `PenaltyPolicy.java` | 接口:`double calculate(double dailyRate, double replacementValue, int daysLate, boolean damaged)` |
| `StandardPenalty.java` | late `0.5×rate×daysLate` + damage `0.3×replacement` |
| `LabPenalty.java` | late `1.0×rate×daysLate` + damage `0.3×replacement` |
| `ElectronicsPenalty.java` | late `0.5×rate×daysLate` + damage `0.4×replacement` |

### src/merit/dao/

| 文件 | 说明 |
|---|---|
| `EquipmentDao.java` | 接口(Phase 0 全组冻结共用) |
| `EquipmentDaoSqlite.java` | 构造接 `Connection`;含 **category→子类重建工厂** |

> 🆕 penalty **不需要** `code()`,因为它不入库(无 `penalty_policy` 列),由 category 在重建时决定。

---

## 三、模式设计要点(A 答辩核心)🆕 关键改动

**把两个模式拆开讲,别让 penalty 拖累 Bridge 的 10 分:**

- **Bridge = `Equipment × PricingPolicy`** —— 真正独立的两轴:任何类别都能配 Standard/Promotional,独立持久化在 `pricing_policy` 列。**Class Diagram 上只在 Equipment↔PricingPolicy 之间画那根 bridge 线。**
- **Strategy = `PenaltyPolicy`** —— 按 category 选默认策略,构造在子类里。这样它是"一个正确应用的 Strategy",而不是"残缺的 Bridge"。Class Diagram 上画成普通策略聚合。
- **唯一的 `switch(category)`** 只准出现在 `EquipmentDaoSqlite` 重建工厂;`model`/`pricing`/`penalty` 里一个 `if(category)` 都不能有。标准答辩词:"唯一按 category 分支的地方是 DAO 重建工厂,这是扁平数据库行还原对象的必要边界,业务逻辑里没有这种 switch。"

---

## 四、验证方式 🆕 断言确切数字

无 `sqlite-jdbc.jar` 时,DAO 能编译但跑不了真实读写;所以写**纯逻辑冒烟测试**(不碰 DB),用 JDK 直接跑,按 demo happy-path 种子数据断言:

**测试用例:E002 Tablet(rate=10, replacement=1500, PROMOTIONAL),租 14 天、逾期 2 天、损坏**

```
base    = 10 × 14 × 0.8         → assert 112.00
late    = 0.5 × 10 × 2          → assert  10.00
damage  = 0.4 × 1500            → assert 600.00
penalty = late + damage         → assert 610.00
net(普通学生0%) = 112 − 0 + 610 → assert 722.00
```

再加一个对照组(如 L001 Microscope + StandardPricing,验证 Lab late 是 1.0 倍率),证明 Bridge 两轴独立走通。

---

## 五、目录 & 编译

```bash
# 用 Git Bash 跑(不要在 PowerShell 跑 find)
javac -d bin $(find src -name "*.java")     # 无 DB 部分可直接编译+跑冒烟测试
```

🆕 Windows classpath 分隔符是 `;`;加 jar 后:`java -cp "bin;lib/sqlite-jdbc.jar" ...`

---

## 六、🆕 别忘的两个协作项(原版漏了)

1. **Class Diagram 是全系统的图,不只 A 的模块** —— rubric 要 "depicts system's structure"。7/6 前跟 B/C/D 收齐 User/Rental/Bill/UI 的类签名,别只画 Equipment 那几个类。
2. **`EquipmentDao` 接口是 Phase 0 全组冻结的共用契约**,A 只 own 实现;接口签名要在开写前和全组对齐。

---

## 决策(已拍板)

1. ✅ DAO 接 `Connection`,不 import C 的 `Database`。
2. ✅ 用 `src/merit/...` 标准布局。

---

## v2 相比原版的净增量(4 处)

1. 模式拆成 **Bridge + Strategy** 讲
2. `PricingPolicy` 加 `code()`
3. 冒烟测试断言 **722.00** 那组数
4. 提醒 Class Diagram 是全系统 + 接口要全组冻结