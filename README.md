# AI辅助王者荣耀信息管理系统

## 1. 项目概述

这是一个 Java 控制台课程作业项目，用于管理王者荣耀风格的数据：玩家、英雄、装备、战队和对战记录。系统支持登录、查询、排行榜、管理员数据管理、JSON 保存和加载，以及基于英雄类型的装备推荐。

## 2. 如何运行

在项目根目录执行：

```bash
javac -encoding UTF-8 -d out src/Main.java src/model/*.java src/service/*.java src/util/*.java
java -cp out Main
```

项目不依赖 Maven、Gradle 或第三方库，所有功能均由 Java 实现。

## 3. 默认登录账户

- 管理员：`admin` / `admin123`
- 玩家示例：`p001` / `123456`

所有玩家初始密码均为 `123456`。

## 4. 已实现的功能

- 管理员和玩家登录、登出
- 按 ID 或姓名查询玩家
- 按 ID 或名称查询战队概览
- 按名称查询英雄详情
- 装备统计和推荐装备
- 玩家或战队最近 N 场对战历史
- 按胜率、等级、对战次数显示排行榜
- 管理员添加、编辑、删除玩家、英雄、装备、战队和对战记录
- 玩家编辑自己的昵称
- JSON 保存和加载数据

## 5. 使用的 Java 概念

- 继承：`Player` 和 `Admin` 继承 `Person`
- 接口：`Searchable`、`Reportable`、`Persistable`、`Authenticatable`
- 多态：登录用户以 `Person` 引用保存
- 集合：使用 `Map`、`List`、`Set`
- 枚举：角色、英雄类型、装备类型、对战结果
- 异常处理：处理输入错误、重复 ID、文件加载错误
- 文件 I/O：使用 Java 标准库读写 JSON 文本

## 6. AI使用摘要

本项目使用 Codex 作为 AI 辅助工具，分别以架构师、实现者、测试评审者角色辅助规划、编码和检查。详细记录见 `ai/prompts.md`、`ai/agent-log.md` 和 `ai/reflection.md`。

## 7. 测试摘要

测试文档位于 `docs/test-cases.md`，包含登录、查询、排行榜、数据管理、JSON 持久化和非法输入等场景。

## 8. 已知限制

- JSON 解析器只支持本项目保存的固定结构，不作为通用 JSON 库使用。
- 控制台界面以课程功能展示为主，没有实现 GUI。
