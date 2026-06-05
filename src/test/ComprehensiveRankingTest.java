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

        RankingService.EquipmentScore E100 = rankingService.equipmentRanking().stream()
                .filter(score -> score.equipment().getId().equals("E100"))
                .findFirst()
                .orElseThrow();
        RankingService.EquipmentScore E101 = rankingService.equipmentRanking().stream()
                .filter(score -> score.equipment().getId().equals("E101"))
                .findFirst()
                .orElseThrow();
        assertEquals(4, E100.estimatedUsage(), "E100 估算使用量");
        assertEquals(2, E100.estimatedWins(), "E100 估算胜场");
        assertEquals(2, E100.compatibleHeroCount(), "E100 适配英雄数量");
        assertDouble(77.5, E100.score(), "E100 装备综合实力公式");
        assertDouble(63.6835806185, E101.score(), "E101 装备综合实力公式");
        assertEquals("E100", rankingService.equipmentRanking().get(0).equipment().getId(), "装备综合实力排名第一");

        System.out.println("Comprehensive ranking test passed.");
    }

    private static GameDataManager createData() {
        GameDataManager manager = new GameDataManager();
        manager.addTeam(new Team("T100", "测试红队"));
        manager.addTeam(new Team("T101", "测试蓝队"));

        Equipment E100 = new Equipment("E100", "综合装备A", EquipmentType.ATTACK, 8.0, "测试装备A");
        Equipment E101 = new Equipment("E101", "综合装备B", EquipmentType.ATTACK, 10.0, "测试装备B");
        manager.addEquipment(E100);
        manager.addEquipment(E101);

        Hero H100 = new Hero("H100", "综合英雄A", HeroType.MARKSMAN, 80, 30, 50);
        H100.replaceCompatibleEquipmentIds(java.util.List.of("E100"));
        Hero H101 = new Hero("H101", "综合英雄B", HeroType.MARKSMAN, 90, 20, 60);
        H101.replaceCompatibleEquipmentIds(java.util.List.of("E100", "E101"));
        manager.addHero(H100);
        manager.addHero(H101);

        Player p100 = new Player("P100", "P100", "123456", "综合一号", 50, 10, 0, "T100");
        p100.replaceHeroIds(java.util.List.of("H100", "H101"));
        Player p101 = new Player("P101", "P101", "123456", "综合二号", 100, 5, 5, "T101");
        p101.replaceHeroIds(java.util.List.of("H100"));
        manager.addPlayer(p100);
        manager.addPlayer(p101);

        MatchRecord first = new MatchRecord("M100", LocalDate.of(2026, 6, 1), "T100", "T101", "T100");
        first.setHeroChoice("P100", "H100");
        first.setHeroChoice("P101", "H100");
        manager.addMatch(first);

        MatchRecord second = new MatchRecord("M101", LocalDate.of(2026, 6, 2), "T100", "T101", "T100");
        second.setHeroChoice("P100", "H101");
        second.setHeroChoice("P101", "H100");
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
