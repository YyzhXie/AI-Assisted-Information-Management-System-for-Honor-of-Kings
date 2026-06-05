# 重要说明：作业可视化与 UML 文件

## 1. 正式作业可视化方案

根据重新确认的作业要求，可视化应使用 Java 技术实现。本项目在 `main` 分支中新增 Swing GUI 版本，属于正式作业内容。

运行命令：

```bash
javac -encoding UTF-8 -d out src/Main.java src/GuiMain.java src/model/*.java src/service/*.java src/util/*.java src/gui/*.java
java -cp out GuiMain
```

GUI 至少覆盖以下作业要求功能：

- 玩家查询
- 战队概览
- 英雄详情
- 排行榜

控制台版本仍然保留：

```bash
java -cp out Main
```

## 2. Node.js 分支不作为作业内容

`Node.js-visualization` 分支只是额外实验和个人演示，不作为本 Java 课程作业提交内容。正式提交、说明和演示应以 `main` 分支中的纯 Java 控制台版本和 Swing GUI 版本为准。

## 3. UML 文件位置

本项目已创建电子 UML 文件：

- `docs/class-diagram.puml`：PlantUML 类图源文件，适合正式 UML 工具渲染。
- `docs/class-diagram.md`：Mermaid 预览版，适合在支持 Mermaid 的 Markdown 查看器中直接查看。

UML 覆盖应用入口、Swing GUI、模型类、服务类、工具类、接口、枚举和主要依赖关系。
