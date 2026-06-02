package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Player extends Person {
    private int level;
    private int wins;
    private int losses;
    private String teamId;
    private final List<String> heroIds;

    public Player(String id, String username, String password, String displayName, int level, int wins, int losses, String teamId) {
        super(id, username, password, displayName, Role.PLAYER);
        this.level = level;
        this.wins = wins;
        this.losses = losses;
        this.teamId = teamId;
        this.heroIds = new ArrayList<>();
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, level);
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = Math.max(0, wins);
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = Math.max(0, losses);
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public int getTotalMatches() {
        return wins + losses;
    }

    public double getWinRate() {
        int total = getTotalMatches();
        return total == 0 ? 0.0 : (wins * 100.0 / total);
    }

    public void addHero(String heroId) {
        if (!heroIds.contains(heroId)) {
            heroIds.add(heroId);
        }
    }

    public void removeHero(String heroId) {
        heroIds.remove(heroId);
    }

    public List<String> getHeroIds() {
        return Collections.unmodifiableList(heroIds);
    }

    public void replaceHeroIds(List<String> ids) {
        heroIds.clear();
        for (String id : ids) {
            addHero(id);
        }
    }

    @Override
    public String generateReport() {
        return String.format("玩家ID: %s%n昵称: %s%n等级: %d%n胜率: %.2f%%%n战队ID: %s",
                getId(), getDisplayName(), level, getWinRate(), teamId);
    }
}
