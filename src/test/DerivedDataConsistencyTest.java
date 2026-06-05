package test;

import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.Player;
import model.Team;
import service.FileStorageService;
import service.GameDataManager;
import service.RankingService;
import service.RecommendationEngine;
import service.SearchService;
import util.DataInitializer;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class DerivedDataConsistencyTest {
    public static void main(String[] args) {
        testAddAndDeleteTemporaryPlayer();
        testDeleteExistingPlayerFromTeamCopy();
        testAddAndDeleteHeroOwnership();
        testAddAndDeleteRecommendedEquipment();
        testEquipmentUsageAcrossMultipleHeroes();
        System.out.println("Derived data consistency test passed.");
    }

    private static void testAddAndDeleteTemporaryPlayer() {
        GameDataManager manager = loadData();
        SearchService searchService = searchService(manager);
        Team team = manager.findTeamById("t001").orElseThrow();
        TeamStats before = stats(manager, team);

        Player player = new Player("P880", "P880", "123456", "派生测试玩家", 99, 30, 0, team.getId());
        player.addHero("h003");
        manager.addPlayer(player);

        TeamStats afterAdd = stats(manager, team);
        assertEquals(before.memberCount + 1, afterAdd.memberCount, "新增玩家后战队成员数量");
        assertDouble((before.levelSum + 99.0) / (before.memberCount + 1), afterAdd.averageLevel, "新增玩家后平均等级");
        assertEquals(before.totalMatches, afterAdd.totalMatches, "新增玩家不改变战队历史总对战数");
        assertDouble(before.winRate, afterAdd.winRate, "新增玩家不改变战队历史胜率");
        assertEquals("派生测试玩家", afterAdd.topPlayerName, "新增高胜率玩家后顶尖玩家");

        String overview = searchService.teamOverview(team);
        assertTextContains(overview, "派生测试玩家(P880)", "战队概览显示新增成员");
        assertTextContains(overview, "平均等级: " + format(afterAdd.averageLevel), "战队概览平均等级同步");
        assertTextContains(overview, "总对战数: " + before.totalMatches, "战队概览总对战数保持");
        assertTextContains(overview, "胜率: " + format(before.winRate) + "%", "战队概览胜率保持");
        assertTextContains(overview, "顶尖玩家: 派生测试玩家", "战队概览顶尖玩家同步");

        assertTrue(manager.deletePlayer("P880"), "删除临时玩家成功");
        TeamStats afterDelete = stats(manager, team);
        assertEquals(before.memberCount, afterDelete.memberCount, "删除临时玩家后成员数量恢复");
        assertDouble(before.averageLevel, afterDelete.averageLevel, "删除临时玩家后平均等级恢复");
        assertEquals(before.totalMatches, afterDelete.totalMatches, "删除临时玩家后总对战数恢复");
        assertDouble(before.winRate, afterDelete.winRate, "删除临时玩家后胜率恢复");
        assertEquals(before.topPlayerName, afterDelete.topPlayerName, "删除临时玩家后顶尖玩家恢复");
        assertTextNotContains(searchService.teamOverview(team), "P880", "删除临时玩家后战队概览不再显示该成员");
    }

    private static void testDeleteExistingPlayerFromTeamCopy() {
        GameDataManager manager = loadData();
        SearchService searchService = searchService(manager);
        Team team = manager.findTeamById("t001").orElseThrow();
        TeamStats before = stats(manager, team);
        String targetId = team.getMemberIds().stream()
                .filter(id -> manager.findPlayerById(id).isPresent())
                .findFirst()
                .orElseThrow();
        Player target = manager.findPlayerById(targetId).orElseThrow();

        assertTrue(manager.deletePlayer(targetId), "删除现有玩家成功");
        TeamStats afterDelete = stats(manager, team);
        assertEquals(before.memberCount - 1, afterDelete.memberCount, "删除现有玩家后战队成员数量");
        assertDouble((before.levelSum - target.getLevel()) * 1.0 / (before.memberCount - 1), afterDelete.averageLevel, "删除现有玩家后平均等级");
        assertEquals(before.totalMatches, afterDelete.totalMatches, "删除玩家不改变战队历史总对战数");
        assertDouble(before.winRate, afterDelete.winRate, "删除玩家不改变战队历史胜率");
        assertTextNotContains(searchService.teamOverview(team), targetId, "删除现有玩家后战队概览不再显示该成员");
    }

    private static void testAddAndDeleteHeroOwnership() {
        GameDataManager manager = loadData();
        SearchService searchService = searchService(manager);
        Hero hero = new Hero("h900", "测试射手", HeroType.MARKSMAN, 90, 30, 60);
        hero.addCompatibleEquipment("e001");
        manager.addHero(hero);
        Player first = manager.findPlayerById("P001").orElseThrow();
        Player second = manager.findPlayerById("P002").orElseThrow();
        first.addHero(hero.getId());
        second.addHero(hero.getId());

        String details = searchService.heroDetails(hero);
        assertTextContains(details, "拥有该英雄的玩家: " + first.getDisplayName() + ", " + second.getDisplayName(), "新增英雄后拥有玩家列表");
        assertTextContains(searchService.playerDetails(first), "测试射手", "新增英雄后玩家详情显示英雄");

        assertTrue(manager.deleteHero(hero.getId()), "删除临时英雄成功");
        assertTrue(manager.findHeroById(hero.getId()).isEmpty(), "删除英雄后无法按ID找到");
        assertFalse(first.getHeroIds().contains(hero.getId()), "删除英雄后玩家一移除英雄引用");
        assertFalse(second.getHeroIds().contains(hero.getId()), "删除英雄后玩家二移除英雄引用");
        assertTextNotContains(searchService.playerDetails(first), "测试射手", "删除英雄后玩家详情不再显示该英雄");
    }

    private static void testAddAndDeleteRecommendedEquipment() {
        GameDataManager manager = loadData();
        SearchService searchService = searchService(manager);
        RecommendationEngine recommendationEngine = new RecommendationEngine(manager);
        RankingService rankingService = new RankingService(manager);
        Hero hero = manager.findHeroById("h003").orElseThrow();
        Equipment equipment = new Equipment("e900", "测试破晓", EquipmentType.ATTACK, 999.0, "测试用高评分攻击装");
        manager.addEquipment(equipment);
        hero.addCompatibleEquipment(equipment.getId());

        List<Equipment> recommended = recommendationEngine.recommendForHero(hero, 3);
        assertEquals("e900", recommended.get(0).getId(), "新增高评分同类型装备后推荐第一名");
        assertTextContains(searchService.heroDetails(hero), "推荐装备: 测试破晓", "英雄详情推荐装备同步");
        RankingService.EquipmentScore score = rankingService.equipmentRanking().stream()
                .filter(item -> item.equipment().getId().equals("e900"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, score.compatibleHeroCount(), "新增装备后适配英雄数量");
        assertTrue(score.score() > 0.0 && score.score() <= 100.0, "新增装备后综合实力分数在0到100之间");

        assertTrue(manager.deleteEquipment("e900"), "删除临时装备成功");
        assertTrue(manager.findEquipmentById("e900").isEmpty(), "删除装备后无法按ID找到");
        assertFalse(hero.getCompatibleEquipmentIds().contains("e900"), "删除装备后英雄兼容装备清理");
        assertFalse(recommendationEngine.recommendForHero(hero, 10).stream().anyMatch(item -> item.getId().equals("e900")), "删除装备后推荐列表清理");
        assertTextNotContains(searchService.heroDetails(hero), "测试破晓", "删除装备后英雄详情不再显示该装备");
    }

    private static void testEquipmentUsageAcrossMultipleHeroes() {
        GameDataManager manager = loadData();
        RankingService rankingService = new RankingService(manager);
        List<Hero> heroes = manager.getHeroes().stream().limit(2).toList();
        Equipment equipment = new Equipment("e901", "测试法杖", EquipmentType.MAGIC, 100.0, "测试用多英雄装备");
        manager.addEquipment(equipment);
        heroes.forEach(hero -> hero.addCompatibleEquipment(equipment.getId()));

        RankingService.EquipmentScore score = rankingService.equipmentRanking().stream()
                .filter(item -> item.equipment().getId().equals("e901"))
                .findFirst()
                .orElseThrow();
        assertEquals(2, score.compatibleHeroCount(), "多英雄兼容装备适配英雄数量");
        assertTrue(score.score() > 0.0 && score.score() <= 100.0, "多英雄兼容装备综合实力分数在0到100之间");

        assertTrue(manager.deleteEquipment("e901"), "删除多英雄兼容装备成功");
        for (Hero hero : heroes) {
            assertFalse(hero.getCompatibleEquipmentIds().contains("e901"), "删除装备后所有英雄兼容列表清理");
        }
    }

    private static GameDataManager loadData() {
        try {
            return new FileStorageService().load("data/game-data.json");
        } catch (IOException ex) {
            return DataInitializer.createDefaultData();
        }
    }

    private static SearchService searchService(GameDataManager manager) {
        return new SearchService(manager, new RecommendationEngine(manager));
    }

    private static TeamStats stats(GameDataManager manager, Team team) {
        List<Player> members = team.getMemberIds().stream()
                .map(id -> manager.findPlayerById(id).orElse(null))
                .filter(player -> player != null)
                .toList();
        int levelSum = members.stream().mapToInt(Player::getLevel).sum();
        int totalMatches = manager.matchesForTeam(team.getId()).size();
        long wins = manager.matchesForTeam(team.getId()).stream()
                .filter(match -> team.getId().equals(match.getWinnerTeamId()))
                .count();
        String topPlayer = members.stream()
                .max(Comparator.comparing(Player::getWinRate))
                .map(Player::getDisplayName)
                .orElse("暂无");
        return new TeamStats(
                members.size(),
                levelSum,
                members.isEmpty() ? 0.0 : levelSum * 1.0 / members.size(),
                totalMatches,
                totalMatches == 0 ? 0.0 : wins * 100.0 / totalMatches,
                topPlayer
        );
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，期望: " + expected + "，实际: " + actual);
        }
    }

    private static void assertDouble(double expected, double actual, String message) {
        if (Math.abs(expected - actual) > 0.000001) {
            throw new AssertionError(message + "，期望: " + expected + "，实际: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
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

    private record TeamStats(int memberCount, int levelSum, double averageLevel, int totalMatches, double winRate, String topPlayerName) {
    }
}
