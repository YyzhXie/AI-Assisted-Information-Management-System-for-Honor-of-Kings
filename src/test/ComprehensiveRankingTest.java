package test;

import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Player;
import model.Team;
import service.GameDataManager;
import service.RankingService;

import java.time.LocalDate;

public class ComprehensiveRankingTest {
    public static void main(String[] args) {
        GameDataManager manager = createData();
        RankingService rankingService = new RankingService(manager);

        Player p100 = manager.findPlayerById("P100").orElseThrow();
        Player p101 = manager.findPlayerById("P101").orElseThrow();
        assertDouble(84.1666666667, rankingService.playerComprehensiveScore(p100), "P100 综合实力公式");
        assertDouble(78.3333333333, rankingService.playerComprehensiveScore(p101), "P101 综合实力公式");
        assertEquals("P100", rankingService.topByComprehensiveScore(1).get(0).getId(), "玩家综合实力排名第一");

        RankingService.EquipmentScore e100 = rankingService.equipmentRanking().stream()
                .filter(score -> score.equipment().getId().equals("e100"))
                .findFirst()
                .orElseThrow();
        RankingService.EquipmentScore e101 = rankingService.equipmentRanking().stream()
                .filter(score -> score.equipment().getId().equals("e101"))
                .findFirst()
                .orElseThrow();
        assertEquals(4, e100.estimatedUsage(), "e100 估算使用量");
        assertEquals(2, e100.estimatedWins(), "e100 估算胜场");
        assertEquals(2, e100.compatibleHeroCount(), "e100 适配英雄数量");
        assertDouble(77.5, e100.score(), "e100 装备综合实力公式");
        assertDouble(63.6835806185, e101.score(), "e101 装备综合实力公式");
        assertEquals("e100", rankingService.equipmentRanking().get(0).equipment().getId(), "装备综合实力排名第一");

        System.out.println("Comprehensive ranking test passed.");
    }

    private static GameDataManager createData() {
        GameDataManager manager = new GameDataManager();
        manager.addTeam(new Team("t100", "测试红队"));
        manager.addTeam(new Team("t101", "测试蓝队"));

        Player p100 = new Player("P100", "P100", "123456", "综合一号", 50, 10, 0, "t100");
        p100.replaceHeroIds(java.util.List.of("h100", "h101"));
        Player p101 = new Player("P101", "P101", "123456", "综合二号", 100, 5, 5, "t101");
        p101.replaceHeroIds(java.util.List.of("h100"));
        manager.addPlayer(p100);
        manager.addPlayer(p101);

        Equipment e100 = new Equipment("e100", "综合装备A", EquipmentType.ATTACK, 8.0, "测试装备A");
        Equipment e101 = new Equipment("e101", "综合装备B", EquipmentType.ATTACK, 10.0, "测试装备B");
        manager.addEquipment(e100);
        manager.addEquipment(e101);

        Hero h100 = new Hero("h100", "综合英雄A", HeroType.MARKSMAN, 80, 30, 50);
        h100.replaceCompatibleEquipmentIds(java.util.List.of("e100"));
        Hero h101 = new Hero("h101", "综合英雄B", HeroType.MARKSMAN, 90, 20, 60);
        h101.replaceCompatibleEquipmentIds(java.util.List.of("e100", "e101"));
        manager.addHero(h100);
        manager.addHero(h101);

        MatchRecord first = new MatchRecord("m100", LocalDate.of(2026, 6, 1), "t100", "t101", "t100");
        first.setHeroChoice("P100", "h100");
        first.setHeroChoice("P101", "h100");
        manager.addMatch(first);

        MatchRecord second = new MatchRecord("m101", LocalDate.of(2026, 6, 2), "t100", "t101", "t100");
        second.setHeroChoice("P100", "h101");
        second.setHeroChoice("P101", "h100");
        manager.addMatch(second);
        return manager;
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
}
