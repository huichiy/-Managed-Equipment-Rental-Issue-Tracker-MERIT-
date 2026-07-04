# MERIT — 四人分工执行计划(人人写代码版)

**课程:** CCP6224 OOAD Final Assignment (40%)
**系统:** MERIT — Smart Equipment Rental & Billing System
**截止:** 2026 年 7 月 8 日 23:59(硬性,无延期) · **Feature freeze:** 7 月 7 日 · **面试窗口:** 7 月 10–26 日

> 这份计划只管**分工 / 排期 / 交付物归属 / 答辩准备**。代码骨架(接口、JDBC 模板、Swing 写法)直接用你们已有的 `2026-07-04-merit-team-division.md`,不在这里重复。

---

## 为什么是这个分法(先读这段)

作业写死了两条约束,决定了怎么分工:

1. **"There must be work division among team members"** — 必须按模块分,不能"一起做"。
2. **"Evaluation will be done individually. Every group member must be able to explain and defend the design and implementation."** — 每个人只对**自己那块代码**被单独提问。

所以核心原则是:**每个人都 own 一块自己能 defend 的真代码,报告和图也分给写代码的人——没有人"只画图"或"只写报告"。** 谁做了纯文档活,谁就会在个人 Q&A 被高分问题(OO 原则、Bridge 模式)问到哑口。

**和你旧计划的最大区别:** 旧计划里 Member D 一个人扛「全部 DAO + 全部 UI」,是明显瓶颈。这份把 **4 个 DAO 拆回 A/B/C**(谁设计的对象,谁写它的 DAO——你最懂自己对象的映射),D 因此腾出来**专注 UI + 集成**。同时把老师口头加的 **ER 图**和**报告**明确派给了写代码的人。

---

## 分工总览

| 成员 | 代码模块 | 负责的 DAO | 负责的图 | 报告章节 | 面试主打 |
|------|----------|-----------|----------|----------|----------|
| **A** | Equipment 继承体系 + Bridge(Pricing/Penalty policies) | `EquipmentDao` (+Sqlite) | **Class Diagram** | 设计模式 + OO 四支柱 | **Bridge 模式**(记名 pattern) |
| **B** | User 体系 + `AuthService` + `Rental` + `RentalManager` | `UserDao` + `RentalDao` (+Sqlite) | **Sequence Diagram**(rent / return) | 继承 / 多态 / 聚合 | 多态折扣、角色登录 |
| **C** | `Bill` + `BillGenerator` + `Database`(schema/seed) | `BillDao` (+Sqlite) | **ER / Database Diagram** | 分层架构 + 持久化 + 计费 | 账单计算、DB 设计 |
| **D** | 全部 Swing UI + `Main` + 集成接线 | —(不写 DAO,专注 UI) | **Use Case Diagram** | 集成 + UI + 模块如何连接 | 分层、关注点分离、集成 |

**四个人代码量大致均衡:** A 代码中等但脑力最重(pattern);B 稍多(2 个 DAO,但 User/Rental 的映射比 Equipment 简单);C 掌管全组依赖的 DB 地基(要最早出);D 无 DAO 但扛全部 UI + 集成。

---

## 每人详细任务

### Member A — Equipment & Bridge(记名模式的门面)

**代码(`merit.model` 的 Equipment* / `merit.pricing` / `merit.penalty` / `merit.dao` 的 Equipment):**
- `Category` enum;`Equipment` 抽象类 + `ElectronicsEquipment` / `MediaEquipment` / `LabEquipment`(构造注入 pricing)。
- `PricingPolicy` 接口 + `StandardPricing`(`rate×days`)、`PromotionalPricing`(`×0.8`)。
- `PenaltyPolicy` 接口 + `StandardPenalty` / `LabPenalty`(late 100%)/ `ElectronicsPenalty`(damage +10%)。
- `EquipmentDao` + `EquipmentDaoSqlite`,含 **category→子类重建**(骨架已在 team-division 文档 Task D2)。

**答辩关键(A 必背):**
- **Bridge 用 pricing 打头讲**——它是真独立轴:任何类别都能配 Standard 或 Promotional,连数据库都独立存 `pricing_policy` 列。这是干净的 Bridge。
- penalty 老实说成"**按类别的默认规则**"。⚠️ **注意**:现有代码里 penalty 是硬编码在子类构造函数里的(没有 `penalty_policy` 列),和设计文档"any equipment can get any penalty"那句**冲突**。二选一:
  - (省事)接受现状,面试只用 pricing 证明两轴独立,penalty 讲默认值;或
  - (想演示)给 `equipment` 表加 `penalty_policy` 列 + 构造函数接 penalty 参数,并在 seed 里放一个错配例子(如某 Electronics 配 StandardPenalty)。
- 被问"你不是说没有 `if(category)` 吗?DAO 里明明有"——标准答:"**唯一按 category 分支的地方是 DAO 的重建工厂,这是从扁平数据库行还原对象的必要边界;业务逻辑里没有任何这种 switch。**"

---

### Member B — Users, Auth & Rentals

**代码(`merit.model` 的 User*/Rental · `merit.service` 的 Auth/RentalManager · `merit.dao` 的 User/Rental):**
- `User` 抽象 + `Admin` / `Staff` / `Student`,`getDiscountRate()` 返回 `0 / 0.20 / (finalYear?0.10:0)`。
- `AuthService.login(username, password)` → `User`(经 `UserDao`)。
- `Rental`(**聚合** User + Equipment;算 dueDate、daysLate)。
- `RentalManager.rent(...)` / `.returnEquipment(...)`,切换 availability。
- `UserDao`(role 分支)+ `RentalDao`(靠 User/Equipment 的 DAO 重建)+ 各自 Sqlite。

**答辩关键(B 必背):**
- **多态**:`user.getDiscountRate()` 同一调用、按运行时子类返回不同折扣,billing 里不写 `if(user instanceof ...)`。
- **聚合 vs 组合**:Rental 聚合 User+Equipment(它们比 rental 活得久 → 空心菱形)。
- **login 的正当理由**:是拿来做**角色路由**(Admin→管理界面,Staff/Student→租借界面),不是安全功能。被问"需求没要认证你为什么做"就这么答;明文密码是 academic scope 的已知取舍,别当安全卖。

---

### Member C — Billing & DB 地基

**代码(`merit.model` 的 Bill · `merit.service` 的 BillGenerator · `merit.dao` 的 Database/BillDao):**
- `Bill`(**组合** base/discount/penalty/net 各行值 → 实心菱形)。
- `BillGenerator.generate(rental)`:`base = equipment.calculateRentalCharge(days)`;`discount = base × user.getDiscountRate()`;`penalty = equipment.calculatePenalty(...)`;`net = base − discount + penalty`。
- 逐条账单字符串/表格给 UI。
- **`Database.java`**:连接 + 建表 + seed。
- `BillDao` + `BillDaoSqlite`。

**⚠️ Day-1 关键路径:** `Database.java`(建表 + seed)**必须第一天出**,因为 A 和 B 的 DAO 冒烟测试都要靠它先把表建好、把种子数据塞进去。C 第一天优先做这个,晚了会卡住 A 和 B。

**答辩关键(C 必背):** 账单四段怎么算;组合关系;为什么 DAO 层要隔离 SQL(域/服务与数据库解耦 → future-proof)。

---

### Member D — UI & 集成

**代码(`merit.ui` 全部 + `merit.Main`):**
- `LoginFrame` → 按 role 路由的 `MainFrame`(CardLayout 或 role 判断)。
- Admin:`EquipmentAdminPanel`(增 / 查 / 切换可用)。
- Staff/Student:`CatalogPanel`(浏览可租)→ `RentalPanel`(选天数、租)→ `ReturnPanel`(天数、损坏)→ `BillDialog`(逐条账单)。
- 接线 UI→service→DAO;输入校验(天数 > 0、必填项)。

**答辩关键(D 必背):** 分层架构(UI 不跑 SQL、域不 import Swing);事件处理 + 校验;**"四个人怎么集成的"**——"我们 Phase 0 先冻结了接口(PricingPolicy / PenaltyPolicy / 4 个 DAO / RentalManager / BillGenerator / AuthService),然后各自在接口背后独立开发。"

**D 不写 DAO 了,但 UI + 集成本身够重**,第 7 天要把所有面板接完 + 全组联调,是 long pole。若 7 号吃紧,A 的 buffer 日(见时间线)可来支援 UI。

---

## 交付物清单(PDF 要求 + 老师口头加的)

- [ ] **源码**(全部 `.java`)
- [ ] **Use Case Diagram** — D
- [ ] **Class Diagram**(Bridge 清楚标出) — A
- [ ] **Sequence Diagram(s)**(rent+billing、return+penalty) — B
- [ ] **Database Diagram / ER** — C ← **老师口头加,旧计划漏了,别再漏**
- [ ] **Report**(每人写自己章节 + 合并) ← **老师口头加,旧计划漏了,别再漏**
- [ ] `StudentNames.zip`(含源码 + 全部图;排除 `bin/`、`db/`)

> **报告合并**:每人交自己那节(见分工表"报告章节"),由 8 号当天最闲的人(通常是 A 或 C)合并统稿。

---

## 时间线(今天 = 7 月 4 日;freeze 7 月 7 日)

| 日期 | A(Equipment/Bridge) | B(Users/Rentals) | C(Billing/DB) | D(UI/集成) |
|------|----------------------|-------------------|----------------|-------------|
| **7/4** | Phase 0(全员冻接口)→ Equipment + policies | Phase 0 → User 体系 | Phase 0 → **`Database.java` schema+seed(优先!)** | Phase 0 → UI 骨架 + LoginFrame 壳 |
| **7/5** | policies 完成 + `EquipmentDao(Sqlite)` | `AuthService` + `Rental` + `UserDao` | `Bill` + `BillGenerator` + `BillDao` | `MainFrame` + 角色路由 |
| **7/6** | Class Diagram + buffer(可援 C/D) | `RentalManager` + `RentalDao` + Sequence Diagram | ER Diagram + seed 定稿 | Catalog / Rental / Return / Bill 面板 |
| **7/7 (FREEZE)** | 集成 + 援 UI | 集成 | 集成 | Admin 面板 + **全组接线联调** |
| **7/8** | 全员:端到端测试 → 图定稿 → 合并报告 → 打包 → **23:59 前提交** | | | |

**面试(7/10–26)在提交后**,所以答辩排练可以放到 8 号之后,不占开发时间。

---

## Phase 0 先冻结的集成契约(全员在场,~2h)

在任何人写实现之前,先把这些空接口 commit 掉,四个模块才能独立编译:
- `PricingPolicy` / `PenaltyPolicy`(A↔C 之间)
- 4 个 DAO 接口 `EquipmentDao` / `UserDao` / `RentalDao` / `BillDao`(A/B/C 各自 own 实现,但接口全组共用)
- `AuthService` / `RentalManager` / `BillGenerator` 方法签名(B/C↔D 之间)

骨架代码直接抄 `2026-07-04-merit-team-division.md` 的 Phase 0。**确认四个人 `lib/` 里都有 `sqlite-jdbc` 的 jar**(commit 进去或写清下载步骤),否则队友一 build 就挂。

---

## 全组要盯的几点

- **4 个 feature 对齐 20 分**:设备+租借管理(D 的 admin + B 的 rent/return)、fee/discount/penalty(A+B+C)、逐条账单(C)、可用 Swing UI(D)。四个都要真能跑。
- **账单公式**:`net = base − discount + penalty`,UI 上要分行显示 base / discount / penalty / net(rubric 明确要"detailed billing")。
- **seed 数据**:每类 2–3 个、共 6+ 个设备,**含至少 1 个 promotional 项**(好演示折扣);账户 = 1 Admin + 1 Staff + 1 final-year Student + 1 普通 Student。
- **`double` 存钱是常见面试 gotcha**:准备一句"本 scope 用 double + 显示时四舍五入可接受,生产会用 BigDecimal 避免浮点误差"。
- **demo happy-path**(联调和面试都走这条):学生登录 → 租一个 promotional Electronics(看到 8 折)→ 逾期 2 天且损坏归还(看到 Electronics 损坏加价)→ 逐条账单显示 base/discount/penalty/net;管理员登录 → 加设备 → 出现在目录。

---

## 一句话总结

**每人 = 一块能 defend 的代码 + 一张图 + 一节报告。** DAO 拆给 A/B/C 解放了 D;ER 图和报告补回了老师口头加的两样。谁 own 什么,谁就必须真看懂那段代码——因为评审是单独进行的,答题卡背不出来会当场露馅。
