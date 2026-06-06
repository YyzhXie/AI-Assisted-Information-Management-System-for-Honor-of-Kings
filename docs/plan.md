# 项目计划

## 1. 项目目标

本项目实现一个纯 Java 系统，用于管理王者荣耀风格的玩家、英雄、装备、战队和对战记录。系统用户包括管理员和玩家，管理员可以维护所有数据，玩家可以查看公开信息并修改自己的昵称。项目同时提供控制台入口和 Swing GUI，并加入只预览不落库的对战模拟加分功能。

## 2. 需求分析

系统需要实现玩家查询、战队概览、英雄详情、装备统计、对战历史、对战模拟、排行榜、数据管理、身份验证和文件持久化。控制台菜单负责交互，服务类负责业务逻辑，模型类负责保存数据。

## 3. 使用的 Java 概念

`Player` 和 `Admin` 继承 `Person`。`Searchable`、`Reportable`、`Persistable`、`Authenticatable` 展示接口使用。当前用户以 `Person` 引用保存，体现多态。数据集合使用 `LinkedHashMap`、`List` 和 `Map`。输入和文件加载通过异常处理。数据保存加载使用 JSON 文本文件。角色、英雄类型、装备类型和结果使用枚举。

## 4. 类设计

核心模型类包括 `Person`、`Player`、`Admin`、`Hero`、`Equipment`、`Team`、`MatchRecord`。服务类包括 `GameDataManager`、`AuthenticationService`、`SearchService`、`RankingService`、`FileStorageService`、`RecommendationEngine` 和 `CombatSimulator`。工具类包括 `DataInitializer` 和 `InputHelper`。

## 5. UML 草稿

```text
Person <|-- Player
Person <|-- Admin
Team o-- Player
Player --> Hero
Hero --> Equipment
MatchRecord --> Team
MatchRecord --> Player
GameDataManager --> Player/Hero/Equipment/Team/MatchRecord
AuthenticationService --> GameDataManager
SearchService --> GameDataManager
RankingService --> GameDataManager
CombatSimulator --> GameDataManager
FileStorageService --> GameDataManager
```

## 6. 数据设计

初始数据由 `DataInitializer` 创建，包含 15 名玩家、15 名英雄、20 件装备、3 支战队和 10 条对战记录。保存后的文件为 `data/game-data.json`。

## 7. AI 使用计划

架构师智能体用于类结构设计，实现智能体用于服务和菜单代码，测试/评审智能体用于检查编译、运行和测试用例。

## 8. 提示词策略

提示词明确角色、范围和限制。AI 输出必须经过人工阅读、编译、运行和测试文档验证。

## 9. 开发时间线

项目按文档、模型、服务、初始数据、持久化、菜单、测试、文档收尾八个阶段推进，并通过 Git 记录每个阶段。

## 10. 测试计划

测试覆盖登录、连续登录稳定性、查询、战队概览、英雄详情、装备统计、对战历史、对战模拟、排行榜、综合实力公式、管理员数据管理、玩家编辑、保存加载、非法输入和外部 JSON 长期运营更新。连续登录稳定性单独验证玩家到玩家、管理员到管理员、跨角色切换和失败登录。

## 11. 风险分析

功能较多时容易把逻辑塞进 `Main`，因此使用服务层分离业务逻辑。JSON 解析不使用外部库，因此只支持本项目生成的固定结构。

## 12. 最终反思占位符

最终反思见 `ai/reflection.md`。
