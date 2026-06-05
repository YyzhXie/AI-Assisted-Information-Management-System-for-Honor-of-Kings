# 长线运营外部数据更新指南

## 目标

验证纯 Java 项目在不引入 Node.js、npm、Maven、Gradle 或数据库的情况下，是否能通过外部维护 JSON 文件来长期追加战队和装备数据。

## 结论

可行。当前程序启动时会自动加载 `data/game-data.json`。只要运营人员按固定 JSON 结构添加数据，再运行 Java 程序，新战队和新装备即可在查询、统计和推荐功能中生效。

## 本次验证数据

### 新增战队

```json
{"id": "T004", "name": "云梦试训队", "memberIds": []}
```

验证结果：启动程序后进入公开查询，搜索 `T004`，系统显示战队名称、成员数、平均等级、胜率和顶尖玩家。空成员战队不会导致崩溃。

### 新增装备

```json
{"id": "E021", "name": "日渊", "type": "ATTACK", "rating": 9.2, "attributeDescription": "运营更新装备：远程输出强化"}
```

为了让装备参与英雄详情和推荐，还在后羿 `H003` 的 `compatibleEquipmentIds` 中加入了 `E021`。

验证结果：启动程序后查询 `H003`，系统显示兼容装备包含“日渊”，推荐装备也出现“日渊”。

## 推荐运营流程

1. 备份 `data/game-data.json`。
2. 添加战队时，在 `teams` 数组中追加对象。
3. 添加装备时，在 `equipment` 数组中追加对象。
4. 如果装备需要被英雄使用，把装备 ID 加入对应英雄的 `compatibleEquipmentIds`。
5. 如果战队需要正式包含玩家，同时更新战队 `memberIds` 和玩家 `teamId`。
6. 运行：

```bash
javac -encoding UTF-8 -d out src/Main.java src/model/*.java src/service/*.java src/util/*.java
java -cp out Main
```

7. 在公开查询中搜索新增战队或英雄，确认数据已生效。

## 格式注意事项

- ID 必须唯一，不能和已有玩家、英雄、装备、战队或对战记录重复。
- 枚举字段必须使用代码中的英文枚举值：
  - 英雄类型：`TANK`、`WARRIOR`、`ASSASSIN`、`MAGE`、`MARKSMAN`、`SUPPORT`
  - 装备类型：`ATTACK`、`MAGIC`、`DEFENSE`、`MOVEMENT`、`JUNGLE`、`SUPPORT`
- 日期必须使用 `yyyy-MM-dd`。
- JSON 文件必须保持合法逗号和括号结构。

## 适用范围

这种方式适合课程作业和小型长期维护场景。如果未来数据规模变大、多人同时维护或需要审计历史，建议升级为数据库或提供专门的数据导入工具。
