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
            assertEquals(4, tabs.getTabCount(), "GUI 页签数量");
            assertEquals("玩家查询", tabs.getTitleAt(0), "玩家查询页签");
            assertEquals("战队概览", tabs.getTitleAt(1), "战队概览页签");
            assertEquals("英雄详情", tabs.getTitleAt(2), "英雄详情页签");
            assertEquals("排行榜", tabs.getTitleAt(3), "排行榜页签");
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
}
