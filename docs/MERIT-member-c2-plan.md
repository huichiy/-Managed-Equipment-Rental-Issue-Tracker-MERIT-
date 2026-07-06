# 📋 Member C-2 执行计划 — Bill + BillGenerator + BillDao

> 承接 `docs/MERIT-member-c-plan.md` 的 C-2 半区。C-1(`Database.java`)已完成并验证。
> 数值权威来源:`docs/MERIT-assumptions-and-data-dictionary.md`(§1.7 公式、§2.4 bills 表)。

---

## 一、目标与依赖分层

C-2 要交付 4 个自己的文件,但它们对 B 的依赖**不一样**,必须拆开做:

| 文件 | 归属 | 依赖 B? | 现在能做? |
|------|------|---------|-----------|
| `merit.model.Bill` | C | ❌ 纯值对象 | ✅ 立刻 |
| `merit.dao.BillDao`(接口) | C | ❌ | ✅ 立刻 |
| `merit.dao.BillDaoSqlite` | C | ❌ 只碰 `bills` 表 | ✅ 立刻 |
| `merit.service.BillGenerator` | C | ✅ 读 `Rental` + `User` | ⚠️ 需要 B 的类型 |

所以除了 `BillGenerator`,其余三个和 B 完全无关,现在就能写完 + 测。

---

## 二、关键决策:B 的 `User` / `Rental` 还没出,怎么办

`BillGenerator.generate(rental)` 要读 B 的 `Rental`(聚合 `User` + `Equipment`)。但 B 一行还没写,连类型都不存在 → `BillGenerator` 无法编译。

**决策(已采纳,需知会 B):建两个「Phase-0 最小骨架」**,只放 `BillGenerator` 真正要读的签名,并在文件头注明 **"Phase-0 contract — Member B owns the full implementation"**:

- `merit.model.User`(**抽象类**,只声明 `abstract double getDiscountRate()` + 基础字段/getter)。
  - ⚠️ **不写** `Admin` / `Staff` / `Student` 三个子类——那是 B 的多态答辩核心,C 不碰。C 的测试用**本地测试替身**(test 里的匿名/私有子类)提供折扣率。
- `merit.model.Rental`(最小聚合:id、user、equipment、rentalDays、daysLate、damaged 的 getter)。
  - B 后续会补 rentDate / dueDate / returnDate / returned 等字段和 DAO 映射;只要不改这几个 getter 签名,`BillGenerator` 不受影响。

**为什么这样合规:** Phase-0 "先冻结共用接口再各自实现" 本就是全组动作;C 先把 B↔C 之间的读契约冻下来,让 C-2 能独立编译/测试,B 落地时接手扩展即可。答辩标准词:"我们 Phase-0 先冻结了 `Rental`/`User` 被 `BillGenerator` 消费的那几个签名,双方在契约背后独立开发。"

> 备选(更保守):完全等 B 出 `User`/`Rental` 再写 `BillGenerator`。缺点是 C 现在只能做 3/4,且把 B↔C 的集成风险后移。**不采用。**

---

## 三、各文件设计要点

### 3.1 `merit.model.Bill`(组合值对象)

- 字段:`billId`、`rentalId`、`baseFee`、`discount`、`penalty`、`netPayable`、`createdAt`(ISO 字符串)。
- **组合关系**:base/discount/penalty/net 四段是 Bill 的内部构成,不脱离 Bill 独立存在(答辩画实心菱形)。
- 提供 getter + 一个 `toDetailedString()`,**逐行输出 base / discount / penalty / net**(rubric 要 "detailed billing")。金额显示时用 `String.format("%.2f", ...)` 四舍五入到 2 位;入库存原值。
- 不含任何业务逻辑(不自己算钱)——算钱是 `BillGenerator` 的事。

### 3.2 `merit.service.BillGenerator`(计费收口 + 多态折扣)

```java
public Bill generate(Rental rental) {
    Equipment eq = rental.getEquipment();
    User user    = rental.getUser();
    double base     = eq.calculateRentalCharge(rental.getRentalDays());   // A 的 PricingPolicy
    double discount = base * user.getDiscountRate();                      // ⚠️ 只打 base
    double penalty  = eq.calculatePenalty(rental.getDaysLate(), rental.isDamaged()); // A 的 PenaltyPolicy
    double net      = base - discount + penalty;
    String billId   = "B-" + rental.getId();                              // 1-1,天然唯一
    return new Bill(billId, rental.getId(), base, discount, penalty, net,
                    LocalDateTime.now().toString());
}
```

- ⚠️ **折扣只作用于 base**,别写成 `(base + penalty) × rate`。
- ⚠️ **禁止 `instanceof` / `if(role)`**——折扣靠 `user.getDiscountRate()` 多态拿。这是 C 的多态答辩点。
- `billId = "B-" + rentalId`:每条 rental 对应一张 bill(1-1),用 rentalId 派生保证 PK 唯一且可读。

### 3.3 `merit.dao.BillDao`(Phase-0 冻结接口)

```java
void insert(Bill bill);
Bill findByRentalId(String rentalId);   // 可空
List<Bill> findAll();
```

### 3.4 `merit.dao.BillDaoSqlite`

- 构造接 `Connection`(和 A 同款,不 import `Database`)。
- `insert` 写全 7 列到 `bills`;查询走 `reconstruct(rs)`。
- SQL 异常包成 `RuntimeException`,不外泄 `SQLException`。
- ⚠️ `bills.rental_id` 有 **FK → rentals**,且 C-1 开了 `PRAGMA foreign_keys=ON` → 插 bill 前对应的 rental 必须已存在(测试时要先造 rental 行)。

---

## 四、计费公式 & 冻结对照数

公式(数据字典 §1.7):
```
base=112.00  discount=base×rate  penalty=610.00  net=base−discount+penalty
```

E002 Tablet(PROMOTIONAL,rate=10,replacement=1500),租 14 天、逾期 2 天、损坏:

| 用户 | discountRate | base | discount | penalty | **net** |
|------|-------------|------|----------|---------|--------|
| 普通学生 | 0% | 112.00 | 0.00 | 610.00 | **722.00** |
| Staff | 20% | 112.00 | 22.40 | 610.00 | **699.60** |
| 毕业年学生 | 10% | 112.00 | 11.20 | 610.00 | **710.80** |

---

## 五、验证方式

### 5.1 主验证:`BillGeneratorSmokeTest`(纯逻辑,**不需要 jar / DB**)

- 用真实 `ElectronicsEquipment`(E002 参数)+ 真实 `Rental` + **本地假 `User` 子类**(返回 0% / 20% / 10%)。
- 断言上表三行 net = 722.00 / 699.60 / 710.80,证明「多态折扣 + 只打 base」走通。

```bash
javac -d bin $(find src test -name "*.java")
java -cp bin BillGeneratorSmokeTest
```

### 5.2 可选:`BillDaoSqlite` 持久化(需要 jar + 先造 rental)

因 FK 约束,插 bill 前要先有一条 rental(用 seed 里的 U003 + E002 手插一条 rental),再 `insert(bill)` → `findByRentalId` 读回校验。B 的 `RentalDao` 出来后并入端到端 demo 更自然,此步可暂缓。

---

## 六、交接给 B 的注意事项(重要)

1. `merit.model.User` / `merit.model.Rental` 是 **C 临时建的 Phase-0 骨架**,**归属仍是 B**。B 接手时:
   - 给 `User` 加 `Admin`/`Staff`/`Student` 子类(`getDiscountRate()` 返回 `0 / 0.20 / finalYear?0.10:0`)。
   - 给 `Rental` 补 rentDate/dueDate/returnDate/returned + `UserDao`/`RentalDao` 映射。
   - **别改** `BillGenerator` 依赖的那几个 getter 签名(`getEquipment/getUser/getRentalDays/getDaysLate/isDamaged/getId`),否则要联动改 C。
2. `BillGenerator.generate(Rental)` 是 B↔C↔D 的冻结签名,Sequence Diagram(B 画)按此展开。

---

## 七、Checklist

- [ ] `Bill.java`(四段分行 `toDetailedString()`,组合关系)
- [ ] `BillGenerator.java`(公式对、无 `instanceof`、折扣只打 base、billId 派生自 rentalId)
- [ ] `BillDao.java`(接口冻结)
- [ ] `BillDaoSqlite.java`(接 Connection,写/读 bills,FK 注意)
- [ ] `User.java` / `Rental.java` Phase-0 骨架(标注 B 归属)
- [ ] `BillGeneratorSmokeTest`:722.00 / 699.60 / 710.80 全绿
- [ ] 知会 B 接手骨架 + 冻结签名
