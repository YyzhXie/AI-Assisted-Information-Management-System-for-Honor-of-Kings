package service;

import model.Equipment;
import model.Hero;
import model.MatchRecord;
import model.Player;
import model.Team;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class SearchService implements Searchable<Player> {
    private final GameDataManager manager;
    private final RecommendationEngine recommendationEngine;

    public SearchService(GameDataManager manager, RecommendationEngine recommendationEngine) {
        this.manager = manager;
        this.recommendationEngine = recommendationEngine;
    }

    @Override
    public List<Player> search(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        if (isSuperAccountKeyword(lower)) {
            return List.of();
        }
        return manager.getPlayers().stream()
                .filter(player -> matchesPlayerKeyword(player, lower))
                .toList();
    }

    public List<Player> searchExactPlayers(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        if (isSuperAccountKeyword(lower)) {
            return List.of();
        }
        return manager.getPlayers().stream()
                .filter(player -> equalsIgnoreCase(player.getId(), lower)
                        || equalsIgnoreCase(player.getUsername(), lower)
                        || equalsIgnoreCase(player.getDisplayName(), lower))
                .toList();
    }

    public List<Team> searchTeams(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return manager.getTeams().stream()
                .filter(team -> team.getId().equalsIgnoreCase(keyword)
                        || team.getName().toLowerCase(Locale.ROOT).contains(lower))
                .toList();
    }

    public List<Hero> searchHeroes(String keyword) {
        String lower = keyword.toLowerCase(Locale.ROOT);
        return manager.getHeroes().stream()
                .filter(hero -> hero.getId().equalsIgnoreCase(keyword)
                        || hero.getName().toLowerCase(Locale.ROOT).contains(lower))
                .toList();
    }

    public String playerDetails(Player player) {
        StringBuilder builder = new StringBuilder(player.generateReport());
        manager.findTeamById(player.getTeamId())
                .ifPresent(team -> builder.append("\n战队: ").append(team.getName()));
        builder.append("\n拥有英雄:");
        for (String heroId : player.getHeroIds()) {
            Hero hero = manager.findHeroById(heroId).orElse(null);
            if (hero == null) {
                continue;
            }
            builder.append("\n- ").append(hero.getName()).append(" (").append(hero.getType()).append(")");
            builder.append("\n  可用装备: ");
            builder.append(hero.getCompatibleEquipmentIds().stream()
                    .map(id -> manager.findEquipmentById(id).map(Equipment::getName).orElse(id))
                    .collect(Collectors.joining(", ")));
        }
        return builder.toString();
    }

    public String teamOverview(Team team) {
        List<Player> members = team.getMemberIds().stream()
                .map(id -> manager.findPlayerById(id).orElse(null))
                .filter(player -> player != null)
                .toList();
        double averageLevel = members.stream().mapToInt(Player::getLevel).average().orElse(0.0);
        int wins = 0;
        int total = 0;
        for (MatchRecord match : manager.matchesForTeam(team.getId())) {
            total++;
            if (team.getId().equals(match.getWinnerTeamId())) {
                wins++;
            }
        }
        Player topPlayer = members.stream()
                .max((a, b) -> Double.compare(a.getWinRate(), b.getWinRate()))
                .orElse(null);
        String memberNames = members.stream()
                .map(player -> player.getDisplayName() + "(" + player.getId() + ")")
                .collect(Collectors.joining(", "));
        return String.format("""
                战队ID: %s
                战队名称: %s
                成员: %s
                平均等级: %.2f
                总对战数: %d
                胜率: %.2f%%
                顶尖玩家: %s
                """,
                team.getId(), team.getName(), memberNames, averageLevel, total,
                total == 0 ? 0.0 : wins * 100.0 / total,
                topPlayer == null ? "暂无" : topPlayer.getDisplayName());
    }

    public String heroDetails(Hero hero) {
        String owners = manager.getPlayers().stream()
                .filter(player -> player.getHeroIds().contains(hero.getId()))
                .map(Player::getDisplayName)
                .collect(Collectors.joining(", "));
        if (owners.isBlank()) {
            owners = "暂无";
        }

        String compatible = hero.getCompatibleEquipmentIds().stream()
                .map(id -> manager.findEquipmentById(id).map(Equipment::getName).orElse(id))
                .collect(Collectors.joining(", "));
        String recommended = recommendationEngine.recommendForHero(hero, 3).stream()
                .map(Equipment::getName)
                .collect(Collectors.joining(", "));

        return hero.generateReport()
                + "\n兼容装备: " + compatible
                + "\n拥有该英雄的玩家: " + owners
                + "\n推荐装备: " + (recommended.isBlank() ? "暂无" : recommended);
    }

    public String matchHistoryForPlayer(String playerId, int limit) {
        Player player = manager.findPlayerById(playerId).orElse(null);
        if (player == null) {
            return "未找到玩家。";
        }
        return manager.matchesForPlayer(playerId).stream()
                .limit(limit)
                .map(match -> formatMatchForPlayer(match, player))
                .collect(Collectors.joining("\n\n"));
    }

    public String matchHistoryForTeam(String teamId, int limit) {
        Team team = manager.findTeamById(teamId).orElse(null);
        if (team == null) {
            return "未找到战队。";
        }
        return manager.matchesForTeam(teamId).stream()
                .limit(limit)
                .map(match -> String.format("日期: %s | 对手: %s | 结果: %s | 记录ID: %s",
                        match.getDate(), teamName(match.opponentOf(teamId)),
                        match.getWinnerTeamId().equals(teamId) ? "胜利" : "失败", match.getId()))
                .collect(Collectors.joining("\n"));
    }

    private String formatMatchForPlayer(MatchRecord match, Player player) {
        String heroId = match.getPlayerHeroChoices().get(player.getId());
        String heroName = manager.findHeroById(heroId).map(Hero::getName).orElse(heroId);
        String result = match.getWinnerTeamId().equals(player.getTeamId()) ? "胜利" : "失败";
        return String.format("日期: %s | 对手: %s | 结果: %s | 英雄: %s | 记录ID: %s",
                match.getDate(), teamName(match.opponentOf(player.getTeamId())), result, heroName, match.getId());
    }

    public String heroPickRateForPlayer(String playerId) {
        Map<String, Long> counts = manager.matchesForPlayer(playerId).stream()
                .map(match -> match.getPlayerHeroChoices().get(playerId))
                .filter(id -> id != null)
                .collect(Collectors.groupingBy(id -> id, Collectors.counting()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return "暂无英雄选用记录。";
        }
        return counts.entrySet().stream()
                .map(entry -> {
                    String name = manager.findHeroById(entry.getKey()).map(Hero::getName).orElse(entry.getKey());
                    return String.format("%s: %.2f%%", name, entry.getValue() * 100.0 / total);
                })
                .collect(Collectors.joining(", "));
    }

    private String teamName(String teamId) {
        return manager.findTeamById(teamId).map(Team::getName).orElse(teamId);
    }

    private boolean matchesPlayerKeyword(Player player, String lower) {
        return containsIgnoreCase(player.getId(), lower)
                || containsIgnoreCase(player.getUsername(), lower)
                || containsIgnoreCase(player.getDisplayName(), lower);
    }

    private boolean isSuperAccountKeyword(String lower) {
        return manager.getAdmins().stream()
                .anyMatch(admin -> equalsIgnoreCase(admin.getId(), lower)
                        || equalsIgnoreCase(admin.getUsername(), lower)
                        || equalsIgnoreCase(admin.getDisplayName(), lower));
    }

    private boolean containsIgnoreCase(String value, String lower) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lower);
    }

    private boolean equalsIgnoreCase(String value, String lower) {
        return value != null && value.toLowerCase(Locale.ROOT).equals(lower);
    }
}
