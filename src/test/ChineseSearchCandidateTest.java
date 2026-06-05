package test;

import model.Player;
import service.FileStorageService;
import service.GameDataManager;
import service.RecommendationEngine;
import service.SearchService;
import util.DataInitializer;

import java.io.IOException;
import java.util.List;

public class ChineseSearchCandidateTest {
    public static void main(String[] args) {
        GameDataManager manager = loadData();
        SearchService searchService = new SearchService(manager, new RecommendationEngine(manager));

        Player newPlayer = new Player("P881", "P881", "123456", "阿离新星", 20, 3, 1, "t004");
        newPlayer.addHero("h003");
        manager.addPlayer(newPlayer);

        List<Player> candidates = searchService.search("阿离");
        assertTrue(candidates.stream().anyMatch(player -> player.getId().equals("P001")), "候选应包含原有中文玩家 P001");
        assertTrue(candidates.stream().anyMatch(player -> player.getId().equals("P881")), "候选应包含新增中文玩家 P881");
        assertTrue(candidates.size() >= 2, "中文重叠检索应返回多个候选");
        assertEquals(1, searchService.searchExactPlayers("阿离新星").size(), "新增中文姓名精确检索应唯一命中");

        System.out.println("中文检索词: 阿离");
        System.out.println("候选结果:");
        candidates.forEach(player -> System.out.println("- " + player.getDisplayName() + "(" + player.getId() + ")"));
        System.out.println("Chinese search candidate test passed.");
    }

    private static GameDataManager loadData() {
        try {
            return new FileStorageService().load("data/game-data.json");
        } catch (IOException ex) {
            return DataInitializer.createDefaultData();
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "，期望: " + expected + "，实际: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
