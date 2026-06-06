package test;

import model.Player;
import service.CombatSimulationReport;
import service.CombatSimulator;
import service.FileStorageService;
import service.GameDataManager;
import service.RankingService;
import util.DataInitializer;

import java.io.IOException;
import java.util.Random;

public class CombatSimulatorTest {
    public static void main(String[] args) {
        GameDataManager manager = loadData();
        RankingService rankingService = new RankingService(manager);
        int matchCountBefore = manager.getMatches().size();
        Player playerBefore = manager.findPlayerById("P001").orElseThrow();
        int winsBefore = playerBefore.getWins();
        int lossesBefore = playerBefore.getLosses();
        String topBefore = rankingService.topByWinRate(1).get(0).getId();

        CombatSimulationReport first = new CombatSimulator(manager, new Random(20260606L)).simulate("T001", "T002");
        CombatSimulationReport second = new CombatSimulator(manager, new Random(20260606L)).simulate("T001", "T002");
        assertEquals(first.formatReport(), second.formatReport(), "固定随机种子生成稳定战报");
        assertEquals(5, first.rounds().size(), "模拟固定生成5回合");
        assertTrue(first.winnerTeamId().equals("T001") || first.winnerTeamId().equals("T002"), "胜者必须来自参赛双方");
        assertTextContains(first.formatReport(), "本次模拟不写入对战记录", "战报说明不写入记录");
        assertTextContains(first.formatReport(), "随机环境", "战报包含随机环境");
        assertTextContains(first.formatReport(), "装备触发率", "战报包含装备触发环境");
        assertTextContains(first.formatReport(), "暴击率", "战报包含暴击环境");
        assertTextContains(first.formatReport(), "闪避率", "战报包含闪避环境");

        Player playerAfter = manager.findPlayerById("P001").orElseThrow();
        assertEquals(matchCountBefore, manager.getMatches().size(), "模拟不新增对战记录");
        assertEquals(winsBefore, playerAfter.getWins(), "模拟不修改玩家胜场");
        assertEquals(lossesBefore, playerAfter.getLosses(), "模拟不修改玩家负场");
        assertEquals(topBefore, rankingService.topByWinRate(1).get(0).getId(), "模拟不改变排行榜");

        assertThrows(() -> new CombatSimulator(manager, new Random(1L)).simulate("T001", "T001"),
                "两支战队不能相同", "相同战队不能模拟");
        assertThrows(() -> new CombatSimulator(manager, new Random(1L)).simulate("T004", "T001"),
                "没有成员", "空战队不能模拟");

        System.out.println("Combat simulator test passed.");
    }

    private static GameDataManager loadData() {
        try {
            return new FileStorageService().load("data/game-data.json");
        } catch (IOException ex) {
            return DataInitializer.createDefaultData();
        }
    }

    private static void assertThrows(Runnable runnable, String expectedText, String message) {
        try {
            runnable.run();
            throw new AssertionError(message + "，期望抛出异常。");
        } catch (IllegalArgumentException ex) {
            assertTextContains(ex.getMessage(), expectedText, message);
        }
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

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
