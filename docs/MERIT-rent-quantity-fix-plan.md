# 🐛 修复计划 — 租借时的「数量 vs 天数」混淆

## 一、现象

学生界面里 L001 Microscope 的 **Available = 2**,但底部那个数字框可以选到 **14**,还能成功租。用户以为「available 只有 2,却能租 14 件」。

## 二、真正原因(先说清楚)

底部那个框 **`Days (max 30)` 是「租借天数(时长)」,不是「数量」。**
- 你实际做的是:**租 1 台 Microscope,租期 14 天**——因为有 2 台可租,所以允许。**这不是库存 bug。**
- 目前的设计是:**一次 rent 动作 = 租 1 件**。available 每租一次减 1,减到 0 就租不了(会弹 "out of stock")。所以库存约束其实是生效的,只是**没有让你选「一次租几件」的地方**,而 `Days` 又容易被误看成数量。

## 三、两个修法(二选一)

### 选项 A —— 维持「一次租 1 件」,只消除误解(改动小、风险低)

- 把 `Days (max 30)` 标签改清楚,例如 **`Rental days (duration):`**,让人不会当成数量。
- 选中某一行时,在按钮旁显示该设备的 **Available: N**(只读提示)。
- 库存约束已生效(available=0 就租不了),无需改模型。

**改动文件:** 只动 `RentalFrame`。
**不动:** `Rental` / `Bill` / DAO / schema / 计费。
**缺点:** 仍然不能「一次租多件」——如果你要的是这个,选 B。

---

### 选项 B —— 加「可租数量」选择器,上限 = available(改动大、符合你的直觉)⭐推荐(如果你要多件)

- 租借面板加一个 **`Quantity:` spinner**,它的**最大值动态等于当前选中设备的 `available_quantity`**(选中 L001 时最大只能选 2,想选 14 也选不了)。
- 一次可租 **K 件**:`available -= K`;归还时 `available += K`(不超过总库存)。
- **计费按数量放大:** `base = 单件base × K`;`penalty = 单件penalty × K`;`discount = base × 折扣率`(仍只作用于 base);`net = base − discount + penalty`。
- 账单多显示一行 **Quantity: K**(detailed billing 更完整)。

**改动文件:**
| 文件 | 改动 |
|------|------|
| `rentals` 表(`Database.java`) | 加 `quantity INTEGER NOT NULL DEFAULT 1` 列 |
| `merit.model.Rental` | 加 `quantity` 字段 + getter;构造多带一个参数(保留旧构造给测试) |
| `merit.service.RentalManager` | `rent(...)` 增加 `quantity` 参数:校验 `1 ≤ K ≤ available`,`available -= K`;`returnEquipment` 归还 K 件 |
| `merit.service.BillGenerator` | base/penalty 乘以 `rental.getQuantity()` |
| `merit.model.Bill` | 加 `quantity` 字段,`toDetailedString()` 多显示一行(可选) |
| `merit.dao.RentalDaoSqlite` | insert/reconstruct 读写 `quantity` 列 |
| `merit.ui.RentalFrame` | 加 Quantity spinner;`ListSelectionListener` 里把 spinner 最大值设成选中行的 available;传 K 给 `rent` |
| 测试 | `UserRentalSmokeTest`(按数量租/还)、`BillGeneratorSmokeTest`(K>1 的账单)、端到端 |
| 文档 | 数据字典 `rentals` 表 + 假设 |

**校验(选项 B 的验收):**
- 选中 available=2 的设备时,Quantity spinner 最多只能选 2(选不到 14)。
- 租 2 件 → available 变 0,该设备从目录消失。
- 账单:租 3 天、单价 10、促销 0.8、租 2 件 → base = `10×3×0.8×2 = 48.00`;逾期/损坏罚金也 ×2。
- 归还 → available 加回 2。

## 四、需要你拍板

**你要的是哪一种?**
- **A**:一次只租 1 件,我把界面说清楚就好(最省事);或
- **B**:要能「一次租多件,且不能超过 available」(功能更全,改动较大)。

从你的描述(「能选 14 却只有 2」)看,你大概率要 **B**。确认后我就照选定的方案实现,并回归所有 smoke test。
