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
        validatePlayerReferences(player);
        players.put(player.getId(), player);
        Team team = findTeamById(player.getTeamId()).orElse(null);
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
        validateHeroReferences(hero);
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
        validateMatchReferences(match);
        matches.put(match.getId(), match);
    }

    public void replacePlayer(Player player) {
        String key = keyOf(players, player.getId()).orElse(player.getId());
        if (!players.containsKey(key)) {
            throw new IllegalArgumentException("玩家不存在: " + player.getId());
        }
        validatePlayerReferences(player);
        removePlayerFromTeams(player.getId());
        players.put(key, player);
        Team team = findTeamById(player.getTeamId()).orElse(null);
        if (team != null) {
            team.addMember(player.getId());
        }
    }

    public boolean deletePlayer(String playerId) {
        String key = keyOf(players, playerId).orElse(playerId);
        Player removed = players.remove(key);
        if (removed == null) {
            return false;
        }
        removePlayerFromTeams(removed.getId());
        return true;
    }

    public boolean deleteAdmin(String adminId) {
        return keyOf(admins, adminId).map(admins::remove).isPresent();
    }

    public boolean deleteHero(String heroId) {
        String key = keyOf(heroes, heroId).orElse(heroId);
        Hero removed = heroes.remove(key);
        if (removed == null) {
            return false;
        }
        for (Player player : players.values()) {
            player.removeHero(removed.getId());
        }
        return true;
    }

    public boolean deleteEquipment(String equipmentId) {
        String key = keyOf(equipment, equipmentId).orElse(equipmentId);
        Equipment removed = equipment.remove(key);
        if (removed == null) {
            return false;
        }
        for (Hero hero : heroes.values()) {
            List<String> kept = new ArrayList<>(hero.getCompatibleEquipmentIds());
            kept.remove(removed.getId());
            hero.replaceCompatibleEquipmentIds(kept);
        }
        return true;
    }

    public boolean deleteTeam(String teamId) {
        String key = keyOf(teams, teamId).orElse(teamId);
        Team removed = teams.remove(key);
        if (removed == null) {
            return false;
        }
        for (Player player : players.values()) {
            if (removed.getId().equalsIgnoreCase(player.getTeamId())) {
                player.setTeamId("");
            }
        }
        return true;
    }

    public boolean deleteMatch(String matchId) {
        return keyOf(matches, matchId).map(matches::remove).isPresent();
    }

    public Optional<Player> findPlayerById(String id) {
        return findById(players, id);
    }

    public Optional<Admin> findAdminById(String id) {
        return findById(admins, id);
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
        return findById(heroes, id);
    }

    public Optional<Team> findTeamById(String id) {
        return findById(teams, id);
    }

    public Optional<Equipment> findEquipmentById(String id) {
        return findById(equipment, id);
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
        String resolvedPlayerId = findPlayerById(playerId).map(Player::getId).orElse(playerId);
        return matches.values().stream()
                .filter(match -> match.involvesPlayer(resolvedPlayerId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    public List<MatchRecord> matchesForTeam(String teamId) {
        String resolvedTeamId = findTeamById(teamId).map(Team::getId).orElse(teamId);
        return matches.values().stream()
                .filter(match -> match.involvesTeam(resolvedTeamId))
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();
    }

    public int equipmentUsageCount(String equipmentId) {
        String resolvedEquipmentId = findEquipmentById(equipmentId).map(Equipment::getId).orElse(equipmentId);
        int count = 0;
        for (Hero hero : heroes.values()) {
            if (hero.getCompatibleEquipmentIds().contains(resolvedEquipmentId)) {
                count++;
            }
        }
        return count;
    }

    public void validateHeroReferences(Hero hero) {
        List<String> missingEquipmentIds = hero.getCompatibleEquipmentIds().stream()
                .filter(equipmentId -> findEquipmentById(equipmentId).isEmpty())
                .toList();
        if (!missingEquipmentIds.isEmpty()) {
            throw new IllegalArgumentException("装备ID不存在: " + String.join(", ", missingEquipmentIds));
        }
    }

    public void validateTeamMemberReferences(Team team) {
        List<String> missingPlayerIds = team.getMemberIds().stream()
                .filter(playerId -> findPlayerById(playerId).isEmpty())
                .toList();
        if (!missingPlayerIds.isEmpty()) {
            throw new IllegalArgumentException("成员ID不存在: " + String.join(", ", missingPlayerIds));
        }
    }

    public void validateMatchReferences(MatchRecord match) {
        List<String> errors = new ArrayList<>();
        if (findTeamById(match.getTeamAId()).isEmpty()) {
            errors.add("队伍A不存在: " + match.getTeamAId());
        }
        if (findTeamById(match.getTeamBId()).isEmpty()) {
            errors.add("队伍B不存在: " + match.getTeamBId());
        }
        if (findTeamById(match.getWinnerTeamId()).isEmpty()) {
            errors.add("胜者战队不存在: " + match.getWinnerTeamId());
        } else if (!match.getWinnerTeamId().equals(match.getTeamAId()) && !match.getWinnerTeamId().equals(match.getTeamBId())) {
            errors.add("胜者战队必须是队伍A或队伍B");
        }
        List<String> missingPlayerIds = match.getPlayerHeroChoices().keySet().stream()
                .filter(playerId -> findPlayerById(playerId).isEmpty())
                .toList();
        if (!missingPlayerIds.isEmpty()) {
            errors.add("玩家ID不存在: " + String.join(", ", missingPlayerIds));
        }
        List<String> missingHeroIds = match.getPlayerHeroChoices().values().stream()
                .filter(heroId -> findHeroById(heroId).isEmpty())
                .distinct()
                .toList();
        if (!missingHeroIds.isEmpty()) {
            errors.add("英雄ID不存在: " + String.join(", ", missingHeroIds));
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    private void removePlayerFromTeams(String playerId) {
        for (Team team : teams.values()) {
            team.removeMember(playerId);
        }
    }

    private void validatePlayerReferences(Player player) {
        List<String> errors = new ArrayList<>();
        String teamId = player.getTeamId();
        if (teamId != null && !teamId.isBlank() && findTeamById(teamId).isEmpty()) {
            errors.add("战队ID不存在: " + teamId);
        }
        List<String> missingHeroIds = player.getHeroIds().stream()
                .filter(heroId -> findHeroById(heroId).isEmpty())
                .toList();
        if (!missingHeroIds.isEmpty()) {
            errors.add("英雄ID不存在: " + String.join(", ", missingHeroIds));
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("；", errors));
        }
    }

    private <T> void requireNewId(Map<String, T> map, String id, String label) {
        if (keyOf(map, id).isPresent()) {
            throw new IllegalArgumentException(label + "ID重复: " + id);
        }
    }

    private <T> Optional<T> findById(Map<String, T> map, String id) {
        return keyOf(map, id).map(map::get);
    }

    private <T> Optional<String> keyOf(Map<String, T> map, String id) {
        if (id == null) {
            return Optional.empty();
        }
        T exact = map.get(id);
        if (exact != null) {
            return Optional.of(id);
        }
        return map.keySet().stream()
                .filter(key -> key.equalsIgnoreCase(id))
                .findFirst();
    }
}
