# 王者荣耀 AI 辅助信息管理系统计划

## 1. 项目目标

实现一个纯 Java 控制台系统，管理玩家、英雄、装备、战队和对战记录。用户包括管理员和玩家。管理员可以管理全部数据，玩家可以查看公开信息并修改自己的基础信息。

## 2. 需求分析

系统实现玩家查询、战队概览、英雄详情、装备统计、对战历史、排行榜、身份验证、数据管理和 JSON 保存加载。控制台菜单负责收集用户输入，服务类负责业务逻辑，模型类只保存状态和生成报表。

## 3. 使用的 Java 概念

- 继承：`Player`、`Admin` 继承 `Person`。
- 接口：`Searchable` 用于搜索服务，`Reportable` 用于模型报表，`Persistable` 用于存储服务，`Authenticatable` 用于登录服务。
- 多态：当前登录用户保存为 `Person`。
- 集合：`GameDataManager` 使用 `LinkedHashMap` 保存实体，模型类使用 `ArrayList` 保存关联 ID。
- 异常处理：输入、重复 ID、缺失对象、文件加载失败都通过异常或提示处理。
- 文件 I/O：`FileStorageService` 使用 JSON 文件保存和加载。
- 枚举：角色、英雄类型、装备类型、对战结果均使用枚举。

## 4. 类设计

`Person` 是抽象父类。`Player` 保存等级、战绩、战队和英雄。`Admin` 表示管理员。`Hero` 保存英雄类型、属性和装备兼容性。`Equipment` 保存装备类型、属性和评分。`Team` 保存成员。`MatchRecord` 保存比赛日期、双方战队、结果和英雄选择。

服务类包括 `GameDataManager`、`AuthenticationService`、`SearchService`、`RankingService`、`FileStorageService`、`RecommendationEngine`、`DataInitializer` 和 `InputHelper`。

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
FileStorageService --> GameDataManager
```

## 6. 数据设计

初始数据包含 10 名以上玩家、15 名英雄、20 件装备、3 支战队和 10 条对战记录。首次运行由 `DataInitializer` 创建。用户保存时写入 `data/game-data.json`。

## 7. AI 使用计划

架构师智能体用于规划类结构和接口。实现智能体用于实现搜索、排行榜、存储和菜单。测试/评审智能体用于检查功能覆盖、异常处理和测试用例。

## 8. 提示词策略

每次提示都限定角色、目标和边界，避免要求 AI 一次性生成整个项目。AI 输出需要通过编译、运行、人工阅读和测试用例验证。

## 9. 开发时间线

1. 创建项目结构和文档。
2. 实现模型类和接口。
3. 实现数据管理和初始数据。
4. 实现搜索、排行、推荐和认证服务。
5. 实现 JSON 保存加载。
6. 实现控制台菜单。
7. 编译运行并修复问题。
8. 完成测试文档、AI 文档和 Git 历史导出。

## 10. 测试计划

测试登录、玩家查询、战队查询、英雄查询、装备统计、对战历史、排行榜、管理员增删改、玩家编辑昵称、保存加载和非法输入。

## 11. 风险分析

最大风险是功能较多导致类职责混乱。缓解方式是让模型、服务和菜单分层。JSON 不使用外部库，风险是解析能力有限，因此只支持本项目固定格式。

## 12. 最终反思占位符

最终反思见 `ai/reflection.md`。
