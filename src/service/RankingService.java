package service;

import model.Equipment;
import model.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RankingService {
    private final GameDataManager manager;

    public RankingService(GameDataManager manager) {
        this.manager = manager;
    }

    public List<Player> topByWinRate(int limit) {
        return manager.getPlayers().stream()
                .sorted(Comparator.comparing(Player::getWinRate).reversed()
                        .thenComparing(Player::getLevel, Comparator.reverseOrder())
                        .thenComparing(Player::getTotalMatches, Comparator.reverseOrder())
                        .thenComparing(Player::getId))
                .limit(limit)
                .toList();
    }

    public List<Player> topByLevel(int limit) {
        return manager.getPlayers().stream()
                .sorted(Comparator.comparing(Player::getLevel).reversed()
                        .thenComparing(Player::getWinRate, Comparator.reverseOrder())
                        .thenComparing(Player::getTotalMatches, Comparator.reverseOrder())
                        .thenComparing(Player::getId))
                .limit(limit)
                .toList();
    }

    public List<Player> topByMatchCount(int limit) {
        return manager.getPlayers().stream()
                .sorted(Comparator.comparing(Player::getTotalMatches).reversed()
                        .thenComparing(Player::getLevel, Comparator.reverseOrder())
                        .thenComparing(Player::getWinRate, Comparator.reverseOrder())
                        .thenComparing(Player::getId))
                .limit(limit)
                .toList();
    }

    public List<EquipmentScore> equipmentRanking() {
        return manager.getEquipment().stream()
                .map(item -> new EquipmentScore(item, equipmentScore(item), manager.equipmentUsageCount(item.getId())))
                .sorted(Comparator.comparing(EquipmentScore::score).reversed()
                        .thenComparing(score -> score.equipment().getId()))
                .toList();
    }

    public String formatPlayers(List<Player> players) {
        StringBuilder builder = new StringBuilder();
        int rank = 1;
        for (Player player : players) {
            builder.append(String.format("%d. %s(%s) 等级:%d 胜率:%.2f%% 对战:%d%n",
                    rank++, player.getDisplayName(), player.getId(), player.getLevel(),
                    player.getWinRate(), player.getTotalMatches()));
        }
        return builder.toString();
    }

    public String formatEquipmentRanking() {
        return equipmentRanking().stream()
                .map(score -> String.format("%s(%s) 类型:%s 使用:%d 分数:%.2f",
                        score.equipment().getName(), score.equipment().getId(),
                        score.equipment().getType(), score.usageCount(), score.score()))
                .collect(Collectors.joining("\n"));
    }

    private double equipmentScore(Equipment item) {
        int usage = manager.equipmentUsageCount(item.getId());
        return item.getRating() + usage * 2.0 + usage * 1.5;
    }

    public record EquipmentScore(Equipment equipment, double score, int usageCount) {
    }
}
