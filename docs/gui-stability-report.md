# Swing GUI 加入后的稳定性回归报告

评测日期：2026-06-05
评测目标：验证加入 Swing GUI 后，控制台源程序、服务层、JSON 数据和可视化交互是否保持兼容与稳定。

## 结论

整体稳定。加入 Swing GUI 后，控制台连续登录/退出、玩家连续添加/删除、公开查询安全边界、排行榜排序、综合实力指标、编辑信息页签状态切换和 GUI 读取同一份 JSON 数据均保持正常。评测中发现一个外部 JSON 兼容性边界：带 UTF-8 BOM 的 JSON 文件会导致自写解析器启动加载失败。该问题已在 `FileStorageService.load()` 中修复，并加入 `GuiCompatibilitySmokeTest` 自动烟测。

## 评测范围

- 控制台连续登录和退出：管理员、教练、多个玩家、错误密码。
- 控制台连续添加和删除玩家信息。
- 超级账户安全边界：`admin` 和 `coach` 不进入公开玩家搜索。
- Swing GUI 与源程序兼容性：GUI 复用 `GameDataManager`、`AuthenticationService`、`SearchService`、`RankingService` 和 JSON 数据文件。
- GUI 编辑信息页签：未登录提示、玩家自我编辑、管理员数据维护、登出后回到未登录状态。
- 控制台保存后的 JSON 能被 GUI 路径读取。
- 外部工具保存带 BOM JSON 时，程序仍能加载。

## 测试结果

|编号|场景|操作|结果|
|---|---|---|---|
|GS-01|完整编译|编译 `Main`、`GuiMain`、模型、服务、工具、GUI 和测试类|通过|
|GS-02|连续登录退出|`admin` -> `coach` -> `P001` -> `P012` -> 错误密码|菜单权限切换正常，失败登录不复用旧用户|
|GS-03|连续添加/删除玩家|管理员连续添加 `P901`、`P902`，再连续删除|添加和删除均成功，删除后公开查询显示未找到|
|GS-04|GUI 兼容烟测|运行 `java -cp out test.GuiCompatibilitySmokeTest`|通过，GUI 五个页签存在，排行榜、综合实力列和公开搜索稳定|
|GS-05|控制台保存到 GUI 加载|临时新增 `P930` 并保存 JSON，再用 GUI 路径加载|加载成功，GUI 窗口对象可创建；测试后已还原正式数据|
|GS-06|超级账户公开搜索|搜索 `admin`、`coach`|公开玩家搜索返回 0 条，不暴露超级账户|
|GS-07|带 BOM JSON 加载|构造临时带 UTF-8 BOM 的 JSON 文件|修复后可正常加载|
|GS-08|GUI 登录状态|GUI 内执行玩家登录、登出、管理员登录和错误密码登录|登录状态正确更新，切换登录失败后回到未登录状态|
|GS-09|GUI 编辑页登出回退|玩家登录进入“编辑信息”，再执行登出；管理员登录后错误密码切换|编辑页均回到未登录卡片，不残留旧账号编辑界面|

## 修复的问题

问题：外部工具可能把 `data/game-data.json` 保存为带 UTF-8 BOM 的 UTF-8 文件，原自写 JSON 解析器会把 BOM 当成普通字符，导致加载失败并回退内置数据。

修复：`FileStorageService.load()` 在解析前剥离文件开头的 `\uFEFF`。该修复同时保护控制台版本和 Swing GUI 版本。

## 未实现项说明

程序没有实现超级账户批量删除或批量添加功能，本轮也不新增该功能。按当前已有能力，只测试连续添加和删除玩家信息；超级账户只测试公开检索安全边界和登录稳定性。

## 复测命令

```bash
javac -encoding UTF-8 -d out src/Main.java src/GuiMain.java src/model/*.java src/service/*.java src/util/*.java src/gui/*.java src/test/*.java
java -cp out test.GuiCompatibilitySmokeTest
```
