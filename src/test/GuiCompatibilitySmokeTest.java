package test;

import gui.VisualizationFrame;
import model.Player;
import service.FileStorageService;
import service.GameDataManager;
import service.RankingService;
import service.RecommendationEngine;
import service.SearchService;
import util.DataInitializer;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public class GuiCompatibilitySmokeTest {
    public static void main(String[] args) throws Exception {
        GameDataManager manager = loadData();
        SearchService searchService = new SearchService(manager, new RecommendationEngine(manager));
        RankingService rankingService = new RankingService(manager);

        assertEquals(15, manager.getPlayers().size(), "初始玩家数量");
        assertEquals("云梦试训队", manager.findTeamById("t004").orElseThrow().getName(), "外部新增战队兼容");
        assertEquals("日渊", manager.findEquipmentById("e021").orElseThrow().getName(), "外部新增装备兼容");
        assertEquals("P001", rankingService.topByWinRate(1).get(0).getId(), "胜率榜第一名");
        assertEquals(0, searchService.search("admin").size(), "管理员不进入公开玩家搜索");
        assertEquals(0, searchService.search("coach").size(), "教练不进入公开玩家搜索");
        assertBomJsonCanLoad();

        addAndDeletePlayers(manager, searchService);
        assertEquals(15, manager.getPlayers().size(), "连续增删后玩家数量恢复");
        assertEquals("P001", rankingService.topByWinRate(1).get(0).getId(), "连续增删后排行榜稳定");

        AtomicReference<VisualizationFrame> frameRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            VisualizationFrame frame = new VisualizationFrame(manager);
            frameRef.set(frame);
            JTabbedPane tabs = findTabbedPane(frame.getContentPane());
            if (tabs == null) {
                throw new AssertionError("未找到 GUI 页签组件");
            }
            assertEquals(5, tabs.getTabCount(), "GUI 页签数量");
            assertEquals("玩家查询", tabs.getTitleAt(0), "玩家查询页签");
            assertEquals("战队概览", tabs.getTitleAt(1), "战队概览页签");
            assertEquals("英雄详情", tabs.getTitleAt(2), "英雄详情页签");
            assertEquals("排行榜", tabs.getTitleAt(3), "排行榜页签");
            assertEquals("编辑信息", tabs.getTitleAt(4), "编辑信息页签");
            assertTextContains(frame.getLoginStatusText(), "未登录", "GUI 初始登录状态");
            assertEquals("login", frame.getCurrentEditCardForTest(), "GUI 初始编辑页未登录状态");
            assertEquals("登录已查看信息。", frame.getEditPromptText(), "GUI 未登录编辑提示");
            if (!frame.loginUser("P001", "123456")) {
                throw new AssertionError("GUI 玩家登录失败");
            }
            assertTextContains(frame.getLoginStatusText(), "阿离同学", "GUI 玩家登录状态");
            assertEquals("player", frame.getCurrentEditCardForTest(), "GUI 玩家登录后编辑页状态");
            frame.logoutCurrentUser();
            assertTextContains(frame.getLoginStatusText(), "未登录", "GUI 登出状态");
            assertEquals("login", frame.getCurrentEditCardForTest(), "GUI 登出后编辑页回到未登录状态");
            if (!frame.loginUser("admin", "admin123")) {
                throw new AssertionError("GUI 管理员登录失败");
            }
            assertTextContains(frame.getLoginStatusText(), "系统管理员", "GUI 管理员登录状态");
            assertEquals("admin", frame.getCurrentEditCardForTest(), "GUI 管理员登录后编辑页状态");
            if (frame.loginUser("admin", "wrong")) {
                throw new AssertionError("GUI 错误密码不应登录成功");
            }
            assertTextContains(frame.getLoginStatusText(), "未登录", "GUI 错误密码后回到未登录状态");
            assertTextNotContains(frame.getLoginStatusText(), "管理员", "GUI 管理员失败登录不应暴露管理员状态");
            assertTextNotContains(frame.getLoginStatusText(), "教练", "GUI 管理员失败登录不应暴露教练状态");
            assertEquals("login", frame.getCurrentEditCardForTest(), "GUI 错误密码后编辑页回到未登录状态");
            frame.dispose();
        });

        if (frameRef.get() == null) {
            throw new AssertionError("GUI 窗口未能创建");
        }
        System.out.println("GUI compatibility smoke test passed.");
    }

    private static GameDataManager loadData() {
        try {
            return new FileStorageService().load("data/game-data.json");
        } catch (IOException ex) {
            return DataInitializer.createDefaultData();
        }
    }

    private static void assertBomJsonCanLoad() throws IOException {
        Path tempFile = Files.createTempFile("game-data-bom-", ".json");
        try {
            String json = Files.readString(Path.of("data/game-data.json"), StandardCharsets.UTF_8);
            Files.writeString(tempFile, "\uFEFF" + json, StandardCharsets.UTF_8);
            GameDataManager loaded = new FileStorageService().load(tempFile.toString());
            assertEquals(15, loaded.getPlayers().size(), "带BOM的JSON可加载");
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static void addAndDeletePlayers(GameDataManager manager, SearchService searchService) {
        for (int i = 1; i <= 5; i++) {
            String id = "P9" + String.format("%02d", i);
            Player player = new Player(id, id, "123456", "连续测试玩家" + i, 10 + i, i, 0, "t004");
            player.replaceHeroIds(java.util.List.of("h001", "h003"));
            manager.addPlayer(player);
            assertEquals(1, searchService.searchExactPlayers(id).size(), "新增玩家可搜索: " + id);
        }
        for (int i = 1; i <= 5; i++) {
            String id = "P9" + String.format("%02d", i);
            if (!manager.deletePlayer(id)) {
                throw new AssertionError("删除玩家失败: " + id);
            }
            assertEquals(0, searchService.searchExactPlayers(id).size(), "删除后玩家不可搜索: " + id);
        }
    }

    private static JTabbedPane findTabbedPane(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof JTabbedPane tabs) {
                return tabs;
            }
            if (component instanceof Container child) {
                JTabbedPane nested = findTabbedPane(child);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，期望: " + expected + "，实际: " + actual);
        }
    }

    private static void assertTextContains(String text, String expected, String message) {
        if (!text.contains(expected)) {
            throw new AssertionError(message + "，期望包含: " + expected + "，实际: " + text);
        }
    }

    private static void assertTextNotContains(String text, String unexpected, String message) {
        if (text.contains(unexpected)) {
            throw new AssertionError(message + "，不应包含: " + unexpected + "，实际: " + text);
        }
    }
}
