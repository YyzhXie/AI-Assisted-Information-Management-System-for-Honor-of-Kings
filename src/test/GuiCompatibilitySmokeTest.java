package test;

import gui.VisualizationFrame;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Player;
import model.Team;
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
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

public class GuiCompatibilitySmokeTest {
    public static void main(String[] args) throws Exception {
        GameDataManager manager = loadData();
        SearchService searchService = new SearchService(manager, new RecommendationEngine(manager));
        RankingService rankingService = new RankingService(manager);

        assertEquals(15, manager.getPlayers().size(), "初始玩家数量");
        assertEquals("云梦试训队", manager.findTeamById("T004").orElseThrow().getName(), "外部新增战队兼容");
        assertEquals("日渊", manager.findEquipmentById("E021").orElseThrow().getName(), "外部新增装备兼容");
        assertEquals("P001", rankingService.topByWinRate(1).get(0).getId(), "胜率榜第一名");
        assertEquals(0, searchService.search("admin").size(), "管理员不进入公开玩家搜索");
        assertEquals(0, searchService.search("coach").size(), "教练不进入公开玩家搜索");
        assertBomJsonCanLoad();

        addAndDeletePlayers(manager, searchService);
        assertEquals(15, manager.getPlayers().size(), "连续增删后玩家数量恢复");
        assertEquals("P001", rankingService.topByWinRate(1).get(0).getId(), "连续增删后排行榜稳定");
        assertInvalidPlayerReferencesRejected(manager);
        assertInvalidAdminDataReferencesRejected(manager);
        assertChineseNameConflictSearch(manager, searchService);

        AtomicReference<VisualizationFrame> frameRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            VisualizationFrame frame = new VisualizationFrame(manager);
            frameRef.set(frame);
            assertEquals("王者荣耀信息管理系统", frame.getTitle(), "GUI 窗口标题");
            assertTextNotContains(frame.getTitle(), "AI", "GUI 窗口标题不显示AI相关词句");
            JTabbedPane tabs = findTabbedPane(frame.getContentPane());
            if (tabs == null) {
                throw new AssertionError("未找到 GUI 页签组件");
            }
            assertEquals(6, tabs.getTabCount(), "GUI 页签数量");
            assertEquals("玩家查询", tabs.getTitleAt(0), "玩家查询页签");
            assertEquals("战队概览", tabs.getTitleAt(1), "战队概览页签");
            assertEquals("英雄详情", tabs.getTitleAt(2), "英雄详情页签");
            assertEquals("排行榜", tabs.getTitleAt(3), "排行榜页签");
            assertEquals("对战模拟", tabs.getTitleAt(4), "对战模拟页签");
            assertEquals("编辑信息", tabs.getTitleAt(5), "编辑信息页签");
            assertTextContains(frame.getLoginStatusText(), "未登录", "GUI 初始登录状态");
            assertEquals("login", frame.getCurrentEditCardForTest(), "GUI 初始编辑页未登录状态");
            assertEquals("登录以查看信息。", frame.getEditPromptText(), "GUI 未登录编辑提示");
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
            Player player = new Player(id, id, "123456", "连续测试玩家" + i, 10 + i, i, 0, "T004");
            player.replaceHeroIds(java.util.List.of("H001", "H003"));
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

    private static void assertChineseNameConflictSearch(GameDataManager manager, SearchService searchService) {
        Player player = new Player("P881", "P881", "123456", "阿离新星", 20, 3, 1, "T004");
        player.addHero("H003");
        manager.addPlayer(player);
        java.util.List<Player> candidates = searchService.search("阿离");
        assertTrue(candidates.stream().anyMatch(item -> item.getId().equals("P001")), "中文重叠检索包含原有玩家 P001");
        assertTrue(candidates.stream().anyMatch(item -> item.getId().equals("P881")), "中文重叠检索包含新增玩家 P881");
        assertTrue(candidates.size() >= 2, "中文重叠检索返回多个候选");
        assertEquals(1, searchService.searchExactPlayers("阿离新星").size(), "新增中文昵称精确检索唯一命中");
        if (!manager.deletePlayer("P881")) {
            throw new AssertionError("删除中文重叠测试玩家失败");
        }
        assertEquals(0, searchService.searchExactPlayers("阿离新星").size(), "删除后中文测试玩家不可精确搜索");
    }

    private static void assertInvalidPlayerReferencesRejected(GameDataManager manager) {
        Player player = new Player("P889", "P889", "123456", "非法引用测试", 1, 0, 0, "T999");
        player.addHero("H999");
        try {
            manager.addPlayer(player);
            throw new AssertionError("非法战队和英雄引用不应保存成功");
        } catch (IllegalArgumentException ex) {
            assertTextContains(ex.getMessage(), "战队ID不存在: T999", "非法战队ID提示");
            assertTextContains(ex.getMessage(), "英雄ID不存在: H999", "非法英雄ID提示");
        }
        assertTrue(manager.findPlayerById("P889").isEmpty(), "非法引用玩家不会残留");
    }

    private static void assertInvalidAdminDataReferencesRejected(GameDataManager manager) {
        Hero hero = new Hero("H889", "非法装备英雄", HeroType.WARRIOR, 1, 1, 1);
        hero.addCompatibleEquipment("E999");
        try {
            manager.addHero(hero);
            throw new AssertionError("非法装备引用不应保存英雄成功");
        } catch (IllegalArgumentException ex) {
            assertTextContains(ex.getMessage(), "装备ID不存在: E999", "非法装备ID提示");
        }
        assertTrue(manager.findHeroById("H889").isEmpty(), "非法引用英雄不会残留");

        Team emptyTeam = new Team("T889", "空战队测试");
        manager.addTeam(emptyTeam);
        assertTrue(manager.findTeamById("T889").isPresent(), "空战队允许先创建");
        if (!manager.deleteTeam("T889")) {
            throw new AssertionError("删除空战队测试数据失败");
        }

        Team team = new Team("T890", "非法成员战队");
        team.addMember("P999");
        try {
            manager.validateTeamMemberReferences(team);
            throw new AssertionError("非法成员引用不应通过战队校验");
        } catch (IllegalArgumentException ex) {
            assertTextContains(ex.getMessage(), "成员ID不存在: P999", "非法成员ID提示");
        }
        assertTrue(manager.findTeamById("T890").isEmpty(), "非法成员战队不会残留");

        MatchRecord match = new MatchRecord("M889", LocalDate.of(2026, 6, 5), "T999", "T001", "T999");
        match.setHeroChoice("P999", "H999");
        try {
            manager.addMatch(match);
            throw new AssertionError("非法对战引用不应保存成功");
        } catch (IllegalArgumentException ex) {
            assertTextContains(ex.getMessage(), "队伍A不存在: T999", "非法队伍A提示");
            assertTextContains(ex.getMessage(), "胜者战队不存在: T999", "非法胜者战队提示");
            assertTextContains(ex.getMessage(), "玩家ID不存在: P999", "非法对战玩家提示");
            assertTextContains(ex.getMessage(), "英雄ID不存在: H999", "非法对战英雄提示");
        }
        assertTrue(manager.getMatches().stream().noneMatch(item -> item.getId().equals("M889")), "非法对战记录不会残留");
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
