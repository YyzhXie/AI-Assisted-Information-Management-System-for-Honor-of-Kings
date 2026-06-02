package model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MatchRecord implements Reportable {
    private final String id;
    private LocalDate date;
    private String teamAId;
    private String teamBId;
    private String winnerTeamId;
    private final Map<String, String> playerHeroChoices;

    public MatchRecord(String id, LocalDate date, String teamAId, String teamBId, String winnerTeamId) {
        this.id = id;
        this.date = date;
        this.teamAId = teamAId;
        this.teamBId = teamBId;
        this.winnerTeamId = winnerTeamId;
        this.playerHeroChoices = new LinkedHashMap<>();
    }

    public String getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getTeamAId() {
        return teamAId;
    }

    public void setTeamAId(String teamAId) {
        this.teamAId = teamAId;
    }

    public String getTeamBId() {
        return teamBId;
    }

    public void setTeamBId(String teamBId) {
        this.teamBId = teamBId;
    }

    public String getWinnerTeamId() {
        return winnerTeamId;
    }

    public void setWinnerTeamId(String winnerTeamId) {
        this.winnerTeamId = winnerTeamId;
    }

    public MatchResult resultForTeam(String teamId) {
        return winnerTeamId.equals(teamId) ? MatchResult.WIN : MatchResult.LOSE;
    }

    public void setHeroChoice(String playerId, String heroId) {
        playerHeroChoices.put(playerId, heroId);
    }

    public Map<String, String> getPlayerHeroChoices() {
        return Collections.unmodifiableMap(playerHeroChoices);
    }

    public void replacePlayerHeroChoices(Map<String, String> choices) {
        playerHeroChoices.clear();
        playerHeroChoices.putAll(choices);
    }

    public boolean involvesTeam(String teamId) {
        return teamAId.equals(teamId) || teamBId.equals(teamId);
    }

    public boolean involvesPlayer(String playerId) {
        return playerHeroChoices.containsKey(playerId);
    }

    public String opponentOf(String teamId) {
        if (teamAId.equals(teamId)) {
            return teamBId;
        }
        if (teamBId.equals(teamId)) {
            return teamAId;
        }
        return "未知";
    }

    @Override
    public String generateReport() {
        return String.format("对战ID: %s%n日期: %s%n队伍: %s vs %s%n胜者: %s",
                id, date, teamAId, teamBId, winnerTeamId);
    }
}
