package service;

import model.Admin;
import model.Equipment;
import model.Hero;
import model.MatchRecord;
import model.Player;
import model.Team;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class GameDataManager {
    private final Map<String, Player> players = new LinkedHashMap<>();
    private final Map<String, Admin> admins = new LinkedHashMap<>();
    private final Map<String, Hero> heroes = new LinkedHashMap<>();
    private final Map<String, Equipment> equipment = new LinkedHashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<String, MatchRecord> matches = new LinkedHashMap<>();

    public void addPlayer(Player player) {
        requireNewId(players, player.getId(), "玩家");
        players.put(player.getId(), player);
        Team team = teams.get(player.getTeamId());
        if (team != null) {
            team.addMember(player.getId());
        }
    }

    public void addAdmin(Admin admin) {
        requireNewId(admins, admin.getId(), "管理员");
        admins.put(admin.getId(), admin);
    }

    public void replaceAdmin(Admin admin) {
        if (!admins.containsKey(admin.getId())) {
            throw new IllegalArgumentException("管理员不存在: " + admin.getId());
        }
        admins.put(admin.getId(), admin);
    }

    public void addHero(Hero hero) {
        requireNewId(heroes, hero.getId(), "英雄");
        heroes.put(hero.getId(), hero);
    }

    public void addEquipment(Equipment item) {
        requireNewId(equipment, item.getId(), "装备");
        equipment.put(item.getId(), item);
    }

    public void addTeam(Team team) {
        requireNewId(teams, team.getId(), "战队");
        teams.put(team.getId(), team);
    }

    public void addMatch(MatchRecord match) {
        requireNewId(matches, match.getId(), "对战记录");
        matches.put(match.getId(), match);
    }

    public void replacePlayer(Player player) {
        if (!players.containsKey(player.getId())) {
            throw new IllegalArgumentException("玩家不存在: " + player.getId());
        }
        removePlayerFromTeams(player.getId());
        players.put(player.getId(), player);
        Team team = teams.get(player.getTeamId());
        if (team != null) {
            team.addMember(player.getId());
        }
    }

    public boolean deletePlayer(String playerId) {
        Player removed = players.remove(playerId);
        if (removed == null) {
            return false;
        }
        removePlayerFromTeams(playerId);
        return true;
    }

    public boolean deleteAdmin(String adminId) {
        return admins.remove(adminId) != null;
    }

    public boolean deleteHero(String heroId) {
        Hero removed = heroes.remove(heroId);
        if (removed == null) {
            return false;
        }
        for (Player player : players.values()) {
            player.removeHero(heroId);
        }
        return true;
    }

    public boolean deleteEquipment(String equipmentId) {
        Equipment removed = equipment.remove(equipmentId);
        if (removed == null) {
            return false;
        }
        for (Hero hero : heroes.values()) {
            List<String> kept = new ArrayList<>(hero.getCompatibleEquipmentIds());
            kept.remove(equipmentId);
            hero.replaceCompatibleEquipmentIds(kept);
        }
        return true;
    }

    public boolean deleteTeam(String teamId) {
        Team removed = teams.remove(teamId);
        if (removed == null) {
            return false;
        }
        for (Player player : players.values()) {
            if (teamId.equals(player.getTeamId())) {
                player.setTeamId("");
            }
        }
        return true;
    }

    public boolean deleteMatch(String matchId) {
        return matches.remove(matchId) != null;
    }

    public Optional<Player> findPlayerById(String id) {
        return Optional.ofNullable(players.get(id));
    }

    public Optional<Admin> findAdminById(String id) {
        return Optional.ofNullable(admins.get(id));
    }

    public Optional<Player> findPlayerByUsername(String username) {
        return players.values().stream()
                .filter(player -> player.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public Optional<Admin> findAdminByUsername(String username) {
        return admins.values().stream()
                .filter(admin -> admin.getUsername().equalsIgnoreCase(username))
                .findFirst();
    }

    public Optional<Hero> findHeroById(String id) {
        return Optional.ofNullable(heroes.get(id));
    }

    public Optional<Team> findTeamById(String id) {
        return Optional.ofNullable(teams.get(id));
    }

    public Optional<Equipment> findEquipmentById(String id) {
        return Optional.ofNullable(equipment.get(id));
    }

    public Collection<Player> getPlayers() {
        return players.values();
    }

    public Collection<Admin> getAdmins() {
        return admins.values();
    }

    public Collection<Hero> getHeroes() {
        return heroes.values();
    }

    public Collection<Equipment> getEquipment() {
        return equipment.values();
    }

    public Collection<Team> getTeams() {
        return teams.values();
    }

    public Collection<MatchRecord> getMatches() {
        return matches.values();
    }

    public List<MatchRecord> matchesForPlayer(String playerId) {
        return matches.values().stream()
                .filter(match -> match.involvesPlayer(playerId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    public List<MatchRecord> matchesForTeam(String teamId) {
        return matches.values().stream()
                .filter(match -> match.involvesTeam(teamId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    public int equipmentUsageCount(String equipmentId) {
        int count = 0;
        for (Hero hero : heroes.values()) {
            if (hero.getCompatibleEquipmentIds().contains(equipmentId)) {
                count++;
            }
        }
        return count;
    }

    private void removePlayerFromTeams(String playerId) {
        for (Team team : teams.values()) {
            team.removeMember(playerId);
        }
    }

    private <T> void requireNewId(Map<String, T> map, String id, String label) {
        if (map.containsKey(id)) {
            throw new IllegalArgumentException(label + "ID重复: " + id);
        }
    }
}
