package service;

import model.Equipment;
import model.Hero;
import model.MatchRecord;
import model.Player;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class RankingService {
    private static final double BAYESIAN_M = 5.0;
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

    public List<Player> topByComprehensiveScore(int limit) {
        return manager.getPlayers().stream()
                .sorted(Comparator.comparing(this::playerComprehensiveScore).reversed()
                        .thenComparing(this::playerBayesianWinRate, Comparator.reverseOrder())
                        .thenComparing(Player::getTotalMatches, Comparator.reverseOrder())
                        .thenComparing(Player::getLevel, Comparator.reverseOrder())
                        .thenComparing(Player::getId))
                .limit(limit)
                .toList();
    }

    public List<EquipmentScore> equipmentRanking() {
        int maxEstimatedUsage = manager.getEquipment().stream()
                .mapToInt(this::estimatedEquipmentUsage)
                .max()
                .orElse(0);
        int maxCompatibleHeroCount = manager.getEquipment().stream()
                .mapToInt(item -> manager.equipmentUsageCount(item.getId()))
                .max()
                .orElse(0);
        double globalWinRate = globalMatchChoiceWinRate();
        return manager.getEquipment().stream()
                .map(item -> equipmentScore(item, maxEstimatedUsage, maxCompatibleHeroCount, globalWinRate))
                .sorted(Comparator.comparing(EquipmentScore::score).reversed()
                        .thenComparing(EquipmentScore::bayesianWinRate, Comparator.reverseOrder())
                        .thenComparing(EquipmentScore::estimatedUsage, Comparator.reverseOrder())
                        .thenComparing(score -> score.equipment().getRating(), Comparator.reverseOrder())
                        .thenComparing(score -> score.equipment().getId()))
                .toList();
    }

    public double playerComprehensiveScore(Player player) {
        double bayesianWinRate = playerBayesianWinRate(player);
        double levelScore = maxPlayerLevel() == 0 ? 0.0 : player.getLevel() * 1.0 / maxPlayerLevel();
        double matchVolumeScore = normalizedLog(player.getTotalMatches(), maxPlayerMatches());
        double heroDiversityScore = maxPlayerHeroCount() == 0 ? 0.0 : player.getHeroIds().size() * 1.0 / maxPlayerHeroCount();
        return 100.0 * (0.40 * bayesianWinRate
                + 0.25 * clamp01(levelScore)
                + 0.25 * clamp01(matchVolumeScore)
                + 0.10 * clamp01(heroDiversityScore));
    }

    public double playerBayesianWinRate(Player player) {
        int matches = player.getTotalMatches();
        return (player.getWins() + BAYESIAN_M * globalPlayerWinRate()) / (matches + BAYESIAN_M);
    }

    public String formatPlayers(List<Player> players) {
        StringBuilder builder = new StringBuilder();
        int rank = 1;
        for (Player player : players) {
            builder.append(String.format("%d. %s(%s) 等级:%d 胜率:%.2f%% 对战:%d 综合实力:%.2f%n",
                    rank++, player.getDisplayName(), player.getId(), player.getLevel(),
                    player.getWinRate(), player.getTotalMatches(), playerComprehensiveScore(player)));
        }
        return builder.toString();
    }

    public String formatEquipmentRanking() {
        return equipmentRanking().stream()
                .map(score -> String.format("%s(%s) 类型:%s 综合实力:%.2f 估算使用:%d 适配英雄:%d 贝叶斯胜率:%.2f%%",
                        score.equipment().getName(), score.equipment().getId(),
                        score.equipment().getType(), score.score(), score.estimatedUsage(),
                        score.compatibleHeroCount(), score.bayesianWinRate() * 100.0))
                .collect(Collectors.joining("\n"));
    }

    private EquipmentScore equipmentScore(Equipment item, int maxEstimatedUsage, int maxCompatibleHeroCount, double globalWinRate) {
        int estimatedUsage = estimatedEquipmentUsage(item);
        int estimatedWins = estimatedEquipmentWins(item);
        int compatibleHeroCount = manager.equipmentUsageCount(item.getId());
        double bayesianWinRate = (estimatedWins + BAYESIAN_M * globalWinRate) / (estimatedUsage + BAYESIAN_M);
        double popularityScore = normalizedLog(estimatedUsage, maxEstimatedUsage);
        double ratingScore = clamp01(item.getRating() / 10.0);
        double heroCoverageScore = maxCompatibleHeroCount == 0 ? 0.0 : compatibleHeroCount * 1.0 / maxCompatibleHeroCount;
        double score = 100.0 * (0.35 * bayesianWinRate
                + 0.25 * popularityScore
                + 0.25 * ratingScore
                + 0.15 * clamp01(heroCoverageScore));
        return new EquipmentScore(item, score, estimatedUsage, compatibleHeroCount, estimatedWins, bayesianWinRate);
    }

    private int estimatedEquipmentUsage(Equipment item) {
        int usage = 0;
        for (MatchRecord match : manager.getMatches()) {
            for (String heroId : match.getPlayerHeroChoices().values()) {
                if (heroCompatibleWithEquipment(heroId, item.getId())) {
                    usage++;
                }
            }
        }
        return usage;
    }

    private int estimatedEquipmentWins(Equipment item) {
        int wins = 0;
        for (MatchRecord match : manager.getMatches()) {
            for (var entry : match.getPlayerHeroChoices().entrySet()) {
                Player player = manager.findPlayerById(entry.getKey()).orElse(null);
                if (player != null
                        && player.getTeamId().equals(match.getWinnerTeamId())
                        && heroCompatibleWithEquipment(entry.getValue(), item.getId())) {
                    wins++;
                }
            }
        }
        return wins;
    }

    private boolean heroCompatibleWithEquipment(String heroId, String equipmentId) {
        Hero hero = manager.findHeroById(heroId).orElse(null);
        return hero != null && hero.getCompatibleEquipmentIds().contains(equipmentId);
    }

    private double globalPlayerWinRate() {
        int matches = manager.getPlayers().stream().mapToInt(Player::getTotalMatches).sum();
        int wins = manager.getPlayers().stream().mapToInt(Player::getWins).sum();
        return matches == 0 ? 0.5 : wins * 1.0 / matches;
    }

    private double globalMatchChoiceWinRate() {
        int total = 0;
        int wins = 0;
        for (MatchRecord match : manager.getMatches()) {
            for (String playerId : match.getPlayerHeroChoices().keySet()) {
                Player player = manager.findPlayerById(playerId).orElse(null);
                if (player == null) {
                    continue;
                }
                total++;
                if (player.getTeamId().equals(match.getWinnerTeamId())) {
                    wins++;
                }
            }
        }
        return total == 0 ? 0.5 : wins * 1.0 / total;
    }

    private int maxPlayerLevel() {
        return manager.getPlayers().stream().mapToInt(Player::getLevel).max().orElse(0);
    }

    private int maxPlayerMatches() {
        return manager.getPlayers().stream().mapToInt(Player::getTotalMatches).max().orElse(0);
    }

    private int maxPlayerHeroCount() {
        return manager.getPlayers().stream().mapToInt(player -> player.getHeroIds().size()).max().orElse(0);
    }

    private double normalizedLog(int value, int maxValue) {
        if (value <= 0 || maxValue <= 0) {
            return 0.0;
        }
        return Math.log1p(value) / Math.log1p(maxValue);
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public record EquipmentScore(Equipment equipment, double score, int estimatedUsage, int compatibleHeroCount,
                                 int estimatedWins, double bayesianWinRate) {
    }
}
