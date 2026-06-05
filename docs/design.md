# 设计文档

## 架构

项目分为四层：模型层、服务层、控制台菜单层和 Swing GUI 层。模型层保存数据和简单报表，服务层处理业务逻辑，`Main` 负责控制台菜单流程，`GuiMain` 和 `VisualizationFrame` 负责图形化查询与展示。

## 类关系说明

- `Person` 是抽象用户父类，`Admin` 和 `Player` 通过继承复用登录身份字段。
- `Team` 聚合多个玩家 ID，避免在对象之间形成复杂循环引用。
- `Player` 关联多个英雄 ID，`Hero` 关联多个装备 ID，便于 JSON 保存。
- `MatchRecord` 保存两支战队、胜者和玩家英雄选择，用于对战历史和英雄选用率。
- 服务层统一接收 `GameDataManager`，避免 `Main` 直接维护多个集合。
- `VisualizationFrame` 复用 `AuthenticationService`、`SearchService`、`RankingService` 和 `GameDataManager`，不重新计算业务规则，保证 Swing GUI 与控制台输出一致。

## 接口设计说明

- `Reportable` 让模型类可以生成统一报表。
- `Searchable<Player>` 用于玩家搜索，战队和英雄搜索作为扩展方法保存在 `SearchService`。
- `Authenticatable` 抽象登录、登出和当前用户。
- `Persistable` 抽象保存和加载，便于说明文件 I/O 职责。

## 排行榜规则

玩家胜率 = 胜场 / 总场次。单局比赛只有胜负，没有平局。排行榜支持按胜率、等级、对战次数和综合实力排序。普通排行榜出现同位时，除当前排序条件外，依次按等级、胜率、对战次数降序比较；如果三项仍完全相同，则按玩家 ID 从小到大排序，保证结果稳定。

完整公式另见 `docs/comprehensive-ranking-formulas.md`。

玩家综合实力使用归一化 + 贝叶斯胜率修正：

```text
PlayerScore =
100 * (
  0.40 * BayesianWinRate
+ 0.25 * LevelScore
+ 0.25 * MatchVolumeScore
+ 0.10 * HeroDiversityScore
)
```

其中：

```text
BayesianWinRate = (wins + 5 * globalWinRate) / (matches + 5)
LevelScore = level / maxLevel
MatchVolumeScore = log(1 + totalMatches) / log(1 + maxTotalMatches)
HeroDiversityScore = ownedHeroCount / maxOwnedHeroCount
```

综合实力同分时，依次按贝叶斯胜率、对战次数、等级和玩家 ID 排序。这样可以避免少量比赛 100% 胜率的玩家异常靠前。

## 公开玩家搜索规则

公开玩家搜索只匹配玩家 ID、用户名和昵称，不检索 `Admin` 集合。输入精确命中唯一玩家时直接进入对应操作；输入部分关键字时显示所有候选玩家，由用户选择后再查看详情或对战历史。`admin` 和 `coach` 等超级账户关键字会返回“未找到玩家”，避免外部用户枚举管理员或教练账号。

## Swing GUI 设计

`GuiMain` 是 Swing 可视化入口，启动时优先读取 `data/game-data.json`，失败时回退 `DataInitializer` 内置数据。`VisualizationFrame` 使用页签组织五项可视化功能：玩家查询、战队概览、英雄详情、排行榜和编辑信息。玩家、战队和英雄页签采用“搜索框 + 列表 + 详情面板”的结构；排行榜页签使用表格展示名次、玩家 ID、昵称、等级、胜率和对战次数。

Swing GUI 已接入 `AuthenticationService`。窗口顶部提供登录、登出和“我的信息”入口；未登录时仍可使用公开可视化，玩家登录后会自动定位到自己的玩家详情，管理员和教练登录后显示当前身份状态。切换登录时如果密码错误，GUI 会回到未登录状态，不复用旧用户状态。

“编辑信息”页签使用 `CardLayout` 根据当前登录身份切换界面。未登录时显示居中提示“登录已查看信息。”；玩家登录后只显示自己的昵称和密码编辑表单；管理员或教练登录后显示通用数据维护界面，可按数据类型连续创建、修改或删除管理员、玩家、英雄、装备、战队和对战记录。登出会清空当前用户并强制切回未登录卡片，避免旧账号的编辑界面残留。

Swing GUI 属于正式 Java 作业内容。`Node.js-visualization` 分支仅作为额外实验，不作为作业提交内容。

## UML 文件

电子 UML 类图文件位于 `docs/class-diagram.puml` 和 `docs/class-diagram.md`。类图覆盖应用入口、Swing GUI、模型类、服务类、工具类、接口、枚举和主要依赖关系。

## 装备统计公式

装备综合实力使用归一化 + 贝叶斯胜率修正：

```text
EquipmentScore =
100 * (
  0.35 * BayesianWinRate
+ 0.25 * PopularityScore
+ 0.25 * RatingScore
+ 0.15 * HeroCoverageScore
)
```

其中：

```text
RatingScore = rating / 10.0
HeroCoverageScore = compatibleHeroCount / maxCompatibleHeroCount
PopularityScore = log(1 + estimatedUsage) / log(1 + maxEstimatedUsage)
BayesianWinRate = (wins + 5 * globalWinRate) / (matches + 5)
```

本项目没有真实记录“某场比赛玩家实际购买了什么装备”。因此装备胜率贡献是估算值：若某玩家在比赛中选择的英雄兼容某装备，则认为该装备可能参与该英雄的推荐出装统计。系统不会声称这是游戏内真实购买数据，而是作为课程项目中的推荐与排名参考指标。

## 推荐公式

推荐引擎先按英雄类型匹配装备类型，再按装备评分排序。坦克优先防御，法师优先法术，射手/刺客/战士优先攻击，辅助优先辅助装备。

## 派生数据一致性

战队成员和平均等级来自当前 `Team.memberIds` 与 `Player.level`；顶尖玩家来自当前战队成员的胜率比较。因此添加或删除玩家会影响这些展示项。战队总对战数和战队胜率来自 `MatchRecord`，只添加或删除玩家不会改变历史对战统计。

英雄拥有玩家情况由 `Player.heroIds` 反查得到，推荐装备由英雄兼容装备列表和当前装备集合实时计算。删除英雄时会从所有玩家英雄列表中移除该英雄 ID；删除装备时会从所有英雄兼容装备列表中移除该装备 ID，避免详情页和推荐列表出现已删除数据。

## JSON 持久化

`FileStorageService` 保存固定结构 JSON：players、admins、heroes、equipment、teams、matches。加载时只解析本项目生成的格式，不作为通用 JSON 解析器。

程序启动时优先加载 `data/game-data.json`。这使外部追加战队、装备等运营数据成为可行方案；如果加载失败，程序回退到 `DataInitializer` 的内置数据，避免启动阶段崩溃。

## 登录状态设计

`AuthenticationService` 使用单个 `Person currentUser` 保存当前登录用户。每次成功登录都会覆盖该字段，登出时调用 `logout()` 将其置空。连续登录稳定性测试覆盖了玩家到玩家、管理员到管理员、管理员到玩家、玩家到管理员和失败登录场景，确认不会复用上一个用户的菜单权限。

登录失败提示必须保持身份中性。即使输入的是管理员或教练用户名，系统也只提示“登录失败，请检查用户名和密码。”，不提示“管理员账号登录失败”或其他可用于枚举超级账户的信息。只有管理员或教练账号成功登录之后，控制台菜单和 GUI 顶部状态才显示管理员身份。

## 输入容错设计

`InputHelper` 统一处理控制台输入。整数、小数和日期都有循环校验；必填文本为空时会提示重新输入；英雄类型和装备类型通过枚举解析并在错误时重试。管理员新增对战记录时，错误日期不会再导致程序退出。
