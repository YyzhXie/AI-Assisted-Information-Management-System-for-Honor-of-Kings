# AI辅助王者荣耀信息管理系统

## 1. 项目概述

这是一个 Java 控制台课程作业项目，用于管理王者荣耀风格的数据：玩家、英雄、装备、战队和对战记录。系统支持登录、查询、排行榜、管理员数据管理、JSON 保存和加载，以及基于英雄类型的装备推荐。

## 2. 如何运行

### Java 控制台版本

在项目根目录执行：

```bash
javac -encoding UTF-8 -d out src/Main.java src/model/*.java src/service/*.java src/util/*.java
java -cp out Main
```

项目不依赖 Maven、Gradle 或第三方库，所有功能均由 Java 实现。

程序启动时会优先自动加载 `data/game-data.json`。如果该文件不存在或格式错误，系统会回退到内置初始数据。

### Node.js 可视化版本

本分支新增 `visualization/` 子项目，用于把同一份 `data/game-data.json` 展示为网页仪表盘：

```bash
cd visualization
npm test
npm start
```

启动后访问 `http://localhost:3000`。该版本只暴露玩家、战队、英雄、装备和对战等公开数据，不向浏览器返回管理员、教练或密码字段。详细说明见 `docs/node-visualization.md`。

## 3. 默认登录账户

- 管理员：`admin` / `admin123`
- 管理员：`coach` / `coach123`
- 玩家示例：`P001` / `123456`
- 玩家示例：`P002` / `123456`
- 玩家示例：`P006` / `123456`

所有玩家初始密码均为 `123456`。

## 4. 已实现的功能

- 管理员和玩家登录、登出
- 按玩家 ID、用户名或昵称关键字查询玩家；精确命中时直接显示详情，模糊命中时先显示候选列表
- 按 ID 或名称查询战队概览
- 按名称查询英雄详情
- 装备统计和推荐装备
- 玩家或战队最近 N 场对战历史
- 按胜率、等级、对战次数显示排行榜
- 排行榜同位时按等级、胜率、对战次数和玩家 ID 顺序稳定排序
- 管理员添加、编辑、删除玩家、英雄、装备、战队和对战记录
- 玩家编辑自己的昵称
- JSON 保存和加载数据
- 启动时自动加载外部 JSON 数据，支持长期运营式追加战队和装备
- 管理员和教练超级账户不进入公开玩家检索
- Node.js 可视化仪表盘：总览图表、玩家筛选、战队详情、英雄推荐、装备评分和对战时间线

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

测试文档位于 `docs/test-cases.md`，包含登录、查询、排行榜、数据管理、JSON 持久化、非法输入和连续登录稳定性等场景。连续登录专项报告位于 `docs/login-stability-report.md`，交互鲁棒性报告位于 `docs/robustness-report.md`，长期运营外部更新说明位于 `docs/operations-update-guide.md`，输出准确性评测位于 `docs/output-accuracy-report.md`。

## 8. 已知限制

- JSON 解析器只支持本项目保存的固定结构，不作为通用 JSON 库使用。
- 控制台界面以课程功能展示为主，没有实现 GUI。
