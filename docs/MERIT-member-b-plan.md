# 📋 Member B 执行计划 — Users / Auth / Rentals

> 配套 spec:`docs/MERIT-assumptions-and-data-dictionary.md`(§1.1 角色、§1.3 折扣、§1.4 租期、§2.1 users、§2.3 rentals);分工见 `docs/MERIT-team-execution-plan.md`。
> 前置已就位:**A 的 `EquipmentDao`(可查设备)+ C 的 `Database`(users/rentals 表已建好、已 seed 4 个账户)。B 现在可以全量开工。**

---

## 一、目标与依赖

B 负责 **User 体系 + 登录 + 租借流程 + 两个 DAO**,是连接 A(设备)、C(账单)、D(UI)的中枢。

| 依赖 | 状态 |
|------|------|
| A 的 `Equipment` / `EquipmentDao` | ✅ 已完成(rent/return 要查设备、切 availability) |
| C 的 `Database`(users/rentals 表 + seed) | ✅ 已完成(UserDao/RentalDao 直接用) |
| C 的 `BillGenerator`(消费 `Rental`+`User`) | ✅ 已完成,**反向约束 B**(见下) |

---

## 二、⚠️ 接手 C 写的 Phase-0 骨架(第一件事)

C 为了让 `BillGenerator` 能编译,先建了两个**最小骨架**,现在**归属移交给 B**:

- `src/merit/model/User.java`(抽象类,已有 `abstract getDiscountRate()` + 身份 getter)
- `src/merit/model/Rental.java`(最小聚合:id/user/equipment/rentalDays/daysLate/damaged)

**硬约束:`BillGenerator` 和 C 的冒烟测试绑死了这些签名,B 扩展时不能改、只能加:**

| 不能改(改了就断 C) | 可以加(B 的活) |
|---|---|
| `User.getDiscountRate()` | `Admin`/`Staff`/`Student` 三个子类 |
| `User` 的身份 getter | `role` 概念、`final_year` 字段 |
| `Rental` 的 `getId/getUser/getEquipment/getRentalDays/getDaysLate/isDamaged` | `rentDate`/`dueDate`/`returnDate`/`returned` 字段 + getter |
| `Rental(id,user,equipment,rentalDays)` 构造 + `setDaysLate/setDamaged` | **再重载一个**带日期的构造,别删旧的(C 的 test 在用) |

> 改完后务必重跑 `BillGeneratorSmokeTest` + `EquipmentSmokeTest`,确认没把别人搞断。

---

## 三、要创建 / 修改的文件

### 修改(接手扩展)
| 文件 | 动作 |
|------|------|
| `merit/model/User.java` | 保持抽象契约不变;可加 `role` 辅助(可选) |
| `merit/model/Rental.java` | 加 rentDate/dueDate/returnDate/returned + getter;**保留旧构造** |

### 新建
| 文件 | 说明 |
|------|------|
| `merit/model/Admin.java` | `getDiscountRate()=0`(不租借,主要用于角色路由) |
| `merit/model/Staff.java` | `getDiscountRate()=0.20` |
| `merit/model/Student.java` | `getDiscountRate()= finalYear ? 0.10 : 0`;带 `finalYear` 字段 |
| `merit/service/AuthService.java` | `login(username,password) → User`(经 `UserDao`) |
| `merit/service/RentalManager.java` | `rent(...)` / `returnEquipment(...)`,切 availability |
| `merit/dao/UserDao.java` + `UserDaoSqlite.java` | 含 **role→子类重建**(DAO 边界的唯一 switch) |
| `merit/dao/RentalDao.java` + `RentalDaoSqlite.java` | 靠 `UserDao`+`EquipmentDao` 重建聚合 |

---

## 四、各文件设计要点

### 4.1 User 子类(多态折扣 —— B 的答辩核心)

```java
public class Staff extends User {
    public Staff(String id, String username, String password, String name) { super(...); }
    @Override public double getDiscountRate() { return 0.20; }
}
public class Student extends User {
    private final boolean finalYear;
    @Override public double getDiscountRate() { return finalYear ? 0.10 : 0.0; }
}
public class Admin extends User {
    @Override public double getDiscountRate() { return 0.0; }   // 不租借
}
```
- ⚠️ 折扣**只由子类决定**,`BillGenerator` 里不写 `if(role)`。这是 "同一调用 `getDiscountRate()`、运行时多态返回不同值" 的活证据。

### 4.2 Rental 扩展(聚合 + 日期)

- 加字段:`rentDate`(LocalDate)、`dueDate`、`returnDate`(可空)、`returned`(boolean)。
- `dueDate = rentDate.plusDays(rentalDays)`(在 `RentalManager.rent` 算好传入,或 Rental 构造里算)。
- **聚合关系**:Rental 持有 User + Equipment,但它们比 rental 活得久 → 答辩画**空心菱形**(对比 C 的 Bill 组合=实心菱形)。

### 4.3 AuthService(角色路由,非安全)

```java
public User login(String username, String password) {
    User u = userDao.findByUsername(username);
    return (u != null && u.getPassword().equals(password)) ? u : null;   // null = 失败
}
```
- 明文比对(academic scope)。返回的 `User` 交给 D 做路由:`Admin → 管理界面`,`Staff/Student → 租借界面`。

### 4.4 RentalManager(租借流程 + availability)

```java
Rental rent(User user, Equipment equipment, int days):
    校验 days>0 且 days<=30(30 是校验上限,不实现续租)
    校验 equipment.isAvailable() 否则拒绝
    rentDate=today; dueDate=today.plusDays(days)
    组装 Rental → rentalDao.insert(rental)
    equipmentDao.updateAvailability(equipment.getId(), false)   // 切为已租出
    return rental

Bill/void returnEquipment(Rental rental, LocalDate returnDate, boolean damaged):
    daysLate = max(0, returnDate - rental.getDueDate())   // ChronoUnit.DAYS
    设置 rental 的 returnDate/daysLate/damaged/returned=true
    rentalDao.update(rental)
    equipmentDao.updateAvailability(equipment.getId(), true)    // 归还,重新可租
```
- ⚠️ **daysLate 只能 ≥0**(提前还不给负罚金)。damage 是布尔,归还时人工勾选。
- 归还后是否直接生成账单由 D/流程决定;`BillGenerator.generate(rental)` 已就绪,喂这个 rental 即可。

### 4.5 UserDao + UserDaoSqlite(role→子类重建)

```java
interface UserDao {
    User findByUsername(String username);   // 登录用,null=无
    User findById(String id);
    List<User> findAll();
}
```
- **重建工厂**(和 A 的 category switch 同性质,DAO 边界唯一允许的 switch):
```java
switch (role) {
    case "ADMIN"   -> new Admin(...);
    case "STAFF"   -> new Staff(...);
    case "STUDENT" -> new Student(..., final_year==1);
}
```
- 答辩标准词同 A:"按 role 分支只出现在 DAO 重建,是扁平行还原对象的必要边界,业务逻辑里没有。"

### 4.6 RentalDao + RentalDaoSqlite(靠别人的 DAO 重建)

```java
interface RentalDao {
    void insert(Rental rental);
    void update(Rental rental);          // 归还时更新 return_date/days_late/damaged/returned
    Rental findById(String id);
    List<Rental> findAll();
}
```
- 重建时:读到 `user_id`/`equipment_id` → 分别调 `userDao.findById(...)` + `equipmentDao.findById(...)` 组回聚合。**RentalDaoSqlite 构造注入 `UserDao` + `EquipmentDao`**(层间靠接口协作,不 new 具体类)。

---

## 五、冻结数(和数据字典对齐)

| 角色 | getDiscountRate() | 路由去向 | seed 账户 |
|------|-------------------|----------|-----------|
| Admin | 0.00 | 管理界面 | admin/admin |
| Staff | 0.20 | 租借界面 | staff/staff |
| Student(regular) | 0.00 | 租借界面 | student/student |
| Student(final-year) | 0.10 | 租借界面 | finalyear/finalyear |

租期:默认 14 天,上限 30 天(仅校验,不续租),`days>0`。

---

## 六、关键不变量(别搞错)

- **折扣多态**:折扣值只在 User 子类里,`getDiscountRate()` 之外无第二处定义。
- **availability 一致性**:rent → `available=0`,return → `available=1`;目录只列 available=1。
- **daysLate ≥ 0**:`max(0, returnDate−dueDate)`,用 `ChronoUnit.DAYS`。
- **日期存 ISO 字符串**(`LocalDate.toString()`),未还的 `return_date` 存 NULL。
- **登录是角色路由不是安全**;明文密码是 academic scope 已知取舍。

---

## 七、验证方式

### 7.1 纯逻辑(不需要 jar)
`UserRentalSmokeTest`:
- 断言 `new Staff(...).getDiscountRate()==0.20`、`new Student(...,true)==0.10`、`==false→0.0`、`Admin==0.0`。
- 断言 `dueDate = rentDate.plusDays(14)`;`daysLate = max(0, return−due)`(逾期 2 天→2,提前还→0)。

### 7.2 DB 往返(需要 jar,依赖 C 的 Database)
- `UserDaoSqlite(Database.getConnection()).findByUsername("staff")` → 返回一个 `Staff` 实例,`getDiscountRate()==0.20`(证明 role→子类重建走通)。
- `RentalManager.rent(student, E002, 14)` → 插入 rental + E002 `available` 变 0;`returnEquipment(...)` 后变回 1。
- 端到端:rent → returnEquipment → `BillGenerator.generate(rental)` → 逐条账单(串起 A+B+C)。

```bash
javac -cp "lib/sqlite-jdbc.jar" -d bin $(find src test -name "*.java")
java -cp bin UserRentalSmokeTest                       # 纯逻辑
java -cp "bin;lib/sqlite-jdbc.jar" merit.dao.Database  # 确认 DB 在
```

---

## 八、答辩要点(B 必背)

1. **多态**:`user.getDiscountRate()` 同一调用按运行时子类返回不同折扣,billing 无 `instanceof`。
2. **聚合 vs 组合**:Rental 聚合 User+Equipment(空心菱形,它们活得更久);对比 Bill 组合金额(实心菱形)。
3. **login 的正当理由**:角色路由手段,非安全功能;明文密码是 academic scope 取舍。
4. **DAO 边界**:role switch 只在 UserDao 重建;RentalDao 靠 UserDao+EquipmentDao 组装,层间靠接口。

---

## 九、集成 / 交接注意

- **对 C**:改完 `User`/`Rental` 立即回归 `BillGeneratorSmokeTest`,别断 C。
- **对 D**:冻结签名给 UI —— `AuthService.login`、`RentalManager.rent/returnEquipment`、`BillGenerator.generate`。D 的面板照这些调。
- **对 A**:`RentalManager` 通过 `EquipmentDao.updateAvailability` 切状态,不直接改 A 的对象持久层。

---

## 十、Checklist

- [ ] 接手 `User`/`Rental` 骨架,**冻结签名不改**,回归 A、C 两个 test
- [ ] `Admin`/`Staff`/`Student`(多态折扣 0 / 0.20 / finalYear?0.10:0)
- [ ] `Rental` 加 rentDate/dueDate/returnDate/returned(保留旧构造)
- [ ] `AuthService.login`(明文比对,返回 User 或 null)
- [ ] `RentalManager.rent/returnEquipment`(校验 days、切 availability、算 daysLate)
- [ ] `UserDao(+Sqlite)`:role→子类重建
- [ ] `RentalDao(+Sqlite)`:注入 UserDao+EquipmentDao 重建聚合
- [ ] `UserRentalSmokeTest`:折扣 + dueDate + daysLate 全绿
- [ ] DB 往返:findByUsername("staff")→Staff;rent/return 切 availability
- [ ] 冻结签名交给 D
