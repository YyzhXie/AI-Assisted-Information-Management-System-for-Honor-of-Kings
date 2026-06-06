package service;

import model.Equipment;
import model.Hero;
import model.Player;
import model.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CombatSimulator {
    private static final int INITIAL_HEALTH = 1000;
    private static final int ROUND_COUNT = 5;
    private static final int LINEUP_SIZE = 5;

    private final GameDataManager manager;
    private final Random random;

    public CombatSimulator(GameDataManager manager) {
        this(manager, new Random());
    }

    public CombatSimulator(GameDataManager manager, Random random) {
        this.manager = manager;
        this.random = random;
    }

    public CombatSimulationReport simulate(String rawTeamAId, String rawTeamBId) {
        Team teamA = findTeam(rawTeamAId, "队伍A");
        Team teamB = findTeam(rawTeamBId, "队伍B");
        if (teamA.getId().equalsIgnoreCase(teamB.getId())) {
            throw new IllegalArgumentException("两支战队不能相同。");
        }

        TeamPlan planA = buildTeamPlan(teamA);
        TeamPlan planB = buildTeamPlan(teamB);
        Environment environment = nextEnvironment();
        int teamAHealth = INITIAL_HEALTH;
        int teamBHealth = INITIAL_HEALTH;
        List<CombatRoundReport> rounds = new ArrayList<>();
        for (int round = 1; round <= ROUND_COUNT; round++) {
            RoundDamage damage = nextRoundDamage(planA, planB, environment);
            teamBHealth = Math.max(0, teamBHealth - damage.teamADamage());
            teamAHealth = Math.max(0, teamAHealth - damage.teamBDamage());
            rounds.add(new CombatRoundReport(round, damage.teamADamage(), damage.teamBDamage(),
                    teamAHealth, teamBHealth, damage.eventSummary()));
        }

        Team winner = winner(teamA, planA, teamAHealth, teamB, planB, teamBHealth);
        return new CombatSimulationReport(teamA.getId(), teamA.getName(), teamB.getId(), teamB.getName(),
                winner.getId(), winner.getName(), teamAHealth, teamBHealth,
                planA.basePower(), planB.basePower(), planA.lineupText(), planB.lineupText(),
                environment.summary(), List.copyOf(rounds));
    }

    private Team findTeam(String rawTeamId, String label) {
        String teamId = normalizeTeamId(rawTeamId);
        return manager.findTeamById(teamId)
                .orElseThrow(() -> new IllegalArgumentException(label + "不存在: " + teamId));
    }

    private String normalizeTeamId(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.matches("\\d+")) {
            return "T" + String.format("%03d", Integer.parseInt(value));
        }
        if (value.matches("T\\d+")) {
            return "T" + String.format("%03d", Integer.parseInt(value.substring(1)));
        }
        return value;
    }

    private TeamPlan buildTeamPlan(Team team) {
        List<Player> availablePlayers = new ArrayList<>();
        for (String playerId : team.getMemberIds()) {
            Player player = manager.findPlayerById(playerId)
                    .orElseThrow(() -> new IllegalArgumentException("成员玩家不存在: " + playerId));
            if (!player.getHeroIds().isEmpty()) {
                availablePlayers.add(player);
            }
        }
        if (availablePlayers.size() < LINEUP_SIZE) {
            throw new IllegalArgumentException("战队 " + team.getId() + " 可参赛成员不足5人，不能模拟。");
        }
        Collections.shuffle(availablePlayers, random);
        List<FighterPlan> fighters = new ArrayList<>();
        for (Player player : availablePlayers.stream().limit(LINEUP_SIZE).toList()) {
            fighters.add(bestFighterPlan(player));
        }
        double basePower = fighters.stream().mapToDouble(FighterPlan::basePower).sum();
        double attackPower = fighters.stream().mapToDouble(FighterPlan::attackPower).sum();
        double defensePower = fighters.stream().mapToDouble(FighterPlan::defensePower).sum();
        StringBuilder lineup = new StringBuilder();
        for (FighterPlan fighter : fighters) {
            lineup.append(String.format("- %s(%s) 使用 %s(%s)，装备: %s，战力 %.2f%n",
                    fighter.player().getDisplayName(), fighter.player().getId(),
                    fighter.hero().getName(), fighter.hero().getId(),
                    fighter.equipmentNames(), fighter.basePower()));
        }
        return new TeamPlan(team, basePower, attackPower, defensePower, lineup.toString().stripTrailing());
    }

    private FighterPlan bestFighterPlan(Player player) {
        return player.getHeroIds().stream()
                .map(heroId -> fighterPlan(player, heroId))
                .max(Comparator.comparing(FighterPlan::basePower)
                        .thenComparing(plan -> plan.hero().getId()))
                .orElseThrow(() -> new IllegalArgumentException("玩家没有可用英雄: " + player.getId()));
    }

    private FighterPlan fighterPlan(Player player, String heroId) {
        Hero hero = manager.findHeroById(heroId)
                .orElseThrow(() -> new IllegalArgumentException("英雄ID不存在: " + heroId));
        List<Equipment> topEquipment = hero.getCompatibleEquipmentIds().stream()
                .map(equipmentId -> manager.findEquipmentById(equipmentId)
                        .orElseThrow(() -> new IllegalArgumentException("装备ID不存在: " + equipmentId)))
                .sorted(Comparator.comparing(Equipment::getRating).reversed()
                        .thenComparing(Equipment::getId))
                .limit(3)
                .toList();
        double equipmentRating = topEquipment.stream().mapToDouble(Equipment::getRating).sum();
        double basePower = hero.getAttack() * 0.42
                + hero.getSkillPower() * 0.34
                + hero.getDefense() * 0.24
                + player.getLevel() * 1.8
                + equipmentRating * 4.0;
        double attackPower = hero.getAttack() * 0.55
                + hero.getSkillPower() * 0.35
                + player.getLevel() * 1.2
                + equipmentRating * 4.5;
        double defensePower = hero.getDefense() * 0.65
                + player.getLevel() * 0.8
                + equipmentRating * 2.5;
        String equipmentNames = topEquipment.stream()
                .map(Equipment::getName)
                .reduce((a, b) -> a + "、" + b)
                .orElse("无");
        return new FighterPlan(player, hero, equipmentNames, basePower, attackPower, defensePower);
    }

    private Environment nextEnvironment() {
        double mapTempo = 0.92 + random.nextDouble() * 0.16;
        double moraleA = 0.93 + random.nextDouble() * 0.14;
        double moraleB = 0.93 + random.nextDouble() * 0.14;
        double equipmentRate = 0.22 + random.nextDouble() * 0.18;
        double criticalRate = 0.15 + random.nextDouble() * 0.12;
        double dodgeRate = 0.10 + random.nextDouble() * 0.10;
        return new Environment(mapTempo, moraleA, moraleB, equipmentRate, criticalRate, dodgeRate);
    }

    private RoundDamage nextRoundDamage(TeamPlan planA, TeamPlan planB, Environment environment) {
        boolean equipmentA = random.nextDouble() < environment.equipmentRate();
        boolean equipmentB = random.nextDouble() < environment.equipmentRate();
        boolean criticalA = random.nextDouble() < environment.criticalRate();
        boolean criticalB = random.nextDouble() < environment.criticalRate();
        boolean dodgeA = random.nextDouble() < environment.dodgeRate();
        boolean dodgeB = random.nextDouble() < environment.dodgeRate();
        int damageA = calculateDamage(planA.attackPower(), planB.defensePower(), environment.mapTempo(),
                environment.moraleA(), equipmentA, criticalA, dodgeB);
        int damageB = calculateDamage(planB.attackPower(), planA.defensePower(), environment.mapTempo(),
                environment.moraleB(), equipmentB, criticalB, dodgeA);
        String event = eventText(equipmentA, criticalA, dodgeB, planA.team().getName())
                + "；" + eventText(equipmentB, criticalB, dodgeA, planB.team().getName());
        return new RoundDamage(damageA, damageB, event);
    }

    private int calculateDamage(double attackPower, double defensePower, double mapTempo, double morale,
                                boolean equipmentTriggered, boolean critical, boolean dodged) {
        double jitter = 0.88 + random.nextDouble() * 0.24;
        double damage = (52.0 + attackPower * 0.09 - defensePower * 0.035) * mapTempo * morale * jitter;
        if (equipmentTriggered) {
            damage += 22.0;
        }
        if (critical) {
            damage *= 1.45;
        }
        if (dodged) {
            damage *= 0.55;
        }
        return Math.max(20, Math.min(260, (int) Math.round(damage)));
    }

    private String eventText(boolean equipmentTriggered, boolean critical, boolean dodged, String teamName) {
        List<String> events = new ArrayList<>();
        if (equipmentTriggered) {
            events.add("装备触发");
        }
        if (critical) {
            events.add("暴击");
        }
        if (dodged) {
            events.add("被闪避削减");
        }
        if (events.isEmpty()) {
            events.add("常规输出");
        }
        return teamName + String.join("+", events);
    }

    private Team winner(Team teamA, TeamPlan planA, int teamAHealth, Team teamB, TeamPlan planB, int teamBHealth) {
        if (teamAHealth != teamBHealth) {
            return teamAHealth > teamBHealth ? teamA : teamB;
        }
        if (Double.compare(planA.basePower(), planB.basePower()) != 0) {
            return planA.basePower() > planB.basePower() ? teamA : teamB;
        }
        double winRateA = historicalWinRate(teamA.getId());
        double winRateB = historicalWinRate(teamB.getId());
        if (Double.compare(winRateA, winRateB) != 0) {
            return winRateA > winRateB ? teamA : teamB;
        }
        return teamA.getId().compareTo(teamB.getId()) <= 0 ? teamA : teamB;
    }

    private double historicalWinRate(String teamId) {
        int total = manager.matchesForTeam(teamId).size();
        if (total == 0) {
            return 0.0;
        }
        long wins = manager.matchesForTeam(teamId).stream()
                .filter(match -> teamId.equals(match.getWinnerTeamId()))
                .count();
        return wins * 1.0 / total;
    }

    private record FighterPlan(Player player, Hero hero, String equipmentNames,
                               double basePower, double attackPower, double defensePower) {
    }

    private record TeamPlan(Team team, double basePower, double attackPower,
                            double defensePower, String lineupText) {
    }

    private record RoundDamage(int teamADamage, int teamBDamage, String eventSummary) {
    }

    private record Environment(double mapTempo, double moraleA, double moraleB,
                               double equipmentRate, double criticalRate, double dodgeRate) {
        String summary() {
            return String.format(Locale.ROOT,
                    "地图节奏 %.2f，队伍A士气 %.2f，队伍B士气 %.2f，装备触发率 %.0f%%，暴击率 %.0f%%，闪避率 %.0f%%",
                    mapTempo, moraleA, moraleB, equipmentRate * 100.0, criticalRate * 100.0, dodgeRate * 100.0);
        }
    }
}
