import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Person;
import model.Player;
import model.Role;
import model.Team;
import service.AuthenticationService;
import service.FileStorageService;
import service.GameDataManager;
import service.RankingService;
import service.RecommendationEngine;
import service.SearchService;
import util.DataInitializer;
import util.InputHelper;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final String DATA_FILE = "data/game-data.json";
    private static final String LOGIN_FAILURE_MESSAGE = "登录失败，请检查用户名和密码。";

    private GameDataManager manager;
    private AuthenticationService authService;
    private SearchService searchService;
    private RankingService rankingService;
    private RecommendationEngine recommendationEngine;
    private final FileStorageService storageService = new FileStorageService();
    private final InputHelper input;

    public Main() {
        Scanner scanner = new Scanner(System.in);
        this.input = new InputHelper(scanner);
        this.manager = loadStartupData();
        rebuildServices();
    }

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        System.out.println("王者荣耀 AI 辅助信息管理系统");
        while (true) {
            System.out.println("""

                    === 主菜单 ===
                    1. 登录
                    2. 公开查询
                    3. 加载JSON数据
                    4. 保存JSON数据
                    0. 退出
                    """);
            int choice = input.readInt("请选择: ", 0, 4);
            switch (choice) {
                case 1 -> login();
                case 2 -> publicQueryMenu();
                case 3 -> loadData();
                case 4 -> saveData();
                case 0 -> {
                    System.out.println("已退出。");
                    return;
                }
                default -> System.out.println("未知选择。");
            }
        }
    }

    private void login() {
        String username = input.readRequired("用户名: ");
        String password = input.readRequired("密码: ");
        Person user = authService.login(username, password);
        if (user == null) {
            System.out.println(LOGIN_FAILURE_MESSAGE);
            return;
        }
        System.out.println("登录成功，欢迎 " + user.getDisplayName());
        if (user.getRole() == Role.ADMIN) {
            adminMenu();
        } else {
            playerMenu((Player) user);
        }
    }

    private void adminMenu() {
        while (authService.getCurrentUser() != null) {
            System.out.println("""

                    === 管理员菜单 ===
                    1. 公开查询
                    2. 排行榜
                    3. 数据管理
                    4. 保存JSON数据
                    5. 加载JSON数据
                    0. 登出
                    """);
            int choice = input.readInt("请选择: ", 0, 5);
            switch (choice) {
                case 1 -> publicQueryMenu();
                case 2 -> rankingMenu();
                case 3 -> dataManagementMenu();
                case 4 -> saveData();
                case 5 -> loadData();
                case 0 -> authService.logout();
                default -> System.out.println("未知选择。");
            }
        }
    }

    private void playerMenu(Player player) {
        while (authService.getCurrentUser() != null) {
            System.out.println("""

                    === 玩家菜单 ===
                    1. 查看我的信息
                    2. 修改我的昵称
                    3. 查看我的对战历史
                    4. 公开查询
                    5. 排行榜
                    0. 登出
                    """);
            int choice = input.readInt("请选择: ", 0, 5);
            switch (choice) {
                case 1 -> System.out.println(searchService.playerDetails(player));
                case 2 -> {
                    player.setDisplayName(input.readRequired("新的昵称: "));
                    System.out.println("昵称已更新。");
                }
                case 3 -> showPlayerHistory(player.getId());
                case 4 -> publicQueryMenu();
                case 5 -> rankingMenu();
                case 0 -> authService.logout();
                default -> System.out.println("未知选择。");
            }
        }
    }

    private void publicQueryMenu() {
        while (true) {
            System.out.println("""

                    === 公开查询 ===
                    1. 玩家查询
                    2. 战队概览
                    3. 英雄详情
                    4. 装备统计
                    5. 玩家对战历史
                    6. 战队对战历史
                    0. 返回
                    """);
            int choice = input.readInt("请选择: ", 0, 6);
            switch (choice) {
                case 1 -> queryPlayer();
                case 2 -> queryTeam();
                case 3 -> queryHero();
                case 4 -> System.out.println(rankingService.formatEquipmentRanking());
                case 5 -> queryPlayerHistory();
                case 6 -> showTeamHistory(input.readRequired("战队ID: "));
                case 0 -> {
                    return;
                }
                default -> System.out.println("未知选择。");
            }
        }
    }

    private void rankingMenu() {
        int limit = input.readInt("显示前几名: ", 1, 20);
        System.out.println("""
                1. 按胜率
                2. 按等级
                3. 按对战次数
                """);
        int choice = input.readInt("请选择: ", 1, 3);
        switch (choice) {
            case 1 -> System.out.println(rankingService.formatPlayers(rankingService.topByWinRate(limit)));
            case 2 -> System.out.println(rankingService.formatPlayers(rankingService.topByLevel(limit)));
            case 3 -> System.out.println(rankingService.formatPlayers(rankingService.topByMatchCount(limit)));
            default -> System.out.println("未知选择。");
        }
        System.out.println("同位排序：除当前排序条件外，依次按等级、胜率、对战次数降序，再按ID升序。单局比赛没有平局。");
    }

    private void queryPlayer() {
        Player player = selectPublicPlayer("输入玩家ID、用户名或昵称关键字: ");
        if (player != null) {
            System.out.println(searchService.playerDetails(player));
            System.out.println();
        }
    }

    private void queryPlayerHistory() {
        Player player = selectPublicPlayer("输入玩家ID、用户名或昵称关键字: ");
        if (player != null) {
            showPlayerHistory(player.getId());
        }
    }

    private Player selectPublicPlayer(String prompt) {
        String keyword = input.readRequired(prompt);
        List<Player> exactPlayers = searchService.searchExactPlayers(keyword);
        if (exactPlayers.size() == 1) {
            return exactPlayers.get(0);
        }

        List<Player> players = exactPlayers.isEmpty() ? searchService.search(keyword) : exactPlayers;
        if (players.isEmpty()) {
            System.out.println("未找到玩家。");
            return null;
        }

        System.out.println("找到以下候选玩家，请选择：");
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            System.out.printf("%d. %s%n", i + 1, playerCandidateLine(player));
        }
        System.out.println("0. 取消");

        int choice = input.readInt("请选择候选玩家: ", 0, players.size());
        if (choice == 0) {
            System.out.println("已取消选择。");
            return null;
        }
        return players.get(choice - 1);
    }

    private String playerCandidateLine(Player player) {
        String teamName = manager.findTeamById(player.getTeamId()).map(Team::getName).orElse("暂无战队");
        return String.format("%s(%s) 用户名:%s 战队:%s 等级:%d 胜率:%.2f%%",
                player.getDisplayName(), player.getId(), player.getUsername(), teamName,
                player.getLevel(), player.getWinRate());
    }

    private void queryTeam() {
        String keyword = input.readRequired("输入战队ID或名称关键字: ");
        List<Team> teams = searchService.searchTeams(keyword);
        if (teams.isEmpty()) {
            System.out.println("未找到战队。");
            return;
        }
        for (Team team : teams) {
            System.out.println(searchService.teamOverview(team));
        }
    }

    private void queryHero() {
        String keyword = input.readRequired("输入英雄ID或名称关键字: ");
        List<Hero> heroes = searchService.searchHeroes(keyword);
        if (heroes.isEmpty()) {
            System.out.println("未找到英雄。");
            return;
        }
        for (Hero hero : heroes) {
            System.out.println(searchService.heroDetails(hero));
            System.out.println();
        }
    }

    private void showPlayerHistory(String playerId) {
        int limit = input.readInt("最近N场: ", 1, 20);
        System.out.println(searchService.matchHistoryForPlayer(playerId, limit));
        System.out.println("英雄选用率: " + searchService.heroPickRateForPlayer(playerId));
    }

    private void showTeamHistory(String teamId) {
        int limit = input.readInt("最近N场: ", 1, 20);
        System.out.println(searchService.matchHistoryForTeam(teamId, limit));
    }

    private void dataManagementMenu() {
        while (true) {
            System.out.println("""

                    === 数据管理 ===
                    1. 玩家
                    2. 英雄
                    3. 装备
                    4. 战队
                    5. 对战记录
                    0. 返回
                    """);
            int choice = input.readInt("请选择: ", 0, 5);
            switch (choice) {
                case 1 -> managePlayers();
                case 2 -> manageHeroes();
                case 3 -> manageEquipment();
                case 4 -> manageTeams();
                case 5 -> manageMatches();
                case 0 -> {
                    return;
                }
                default -> System.out.println("未知选择。");
            }
        }
    }

    private void managePlayers() {
        int choice = crudChoice("玩家");
        try {
            switch (choice) {
                case 1 -> addPlayer();
                case 2 -> editPlayer();
                case 3 -> deletePlayer();
                case 0 -> {
                }
                default -> System.out.println("未知选择。");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("操作失败: " + ex.getMessage());
        }
    }

    private void addPlayer() {
        String id = input.readRequired("玩家ID: ");
        String name = input.readRequired("昵称: ");
        int level = input.readInt("等级: ", 1, 100);
        String teamId = input.readRequired("战队ID: ");
        Player player = new Player(id, id, "123456", name, level, 0, 0, teamId);
        player.replaceHeroIds(readIds("拥有英雄ID，用逗号分隔: "));
        manager.addPlayer(player);
        System.out.println("玩家已添加，默认密码为 123456。");
    }

    private void editPlayer() {
        Player player = manager.findPlayerById(input.readRequired("玩家ID: ")).orElse(null);
        if (player == null) {
            System.out.println("玩家不存在。");
            return;
        }
        player.setDisplayName(input.readOptional("昵称", player.getDisplayName()));
        player.setLevel(input.readInt("等级: ", 1, 100));
        player.setTeamId(input.readOptional("战队ID", player.getTeamId()));
        player.replaceHeroIds(readIds("拥有英雄ID，用逗号分隔: "));
        manager.replacePlayer(player);
        System.out.println("玩家已更新。");
    }

    private void deletePlayer() {
        String id = input.readRequired("玩家ID: ");
        System.out.println(manager.deletePlayer(id) ? "玩家已删除。" : "玩家不存在。");
    }

    private void manageHeroes() {
        int choice = crudChoice("英雄");
        try {
            switch (choice) {
                case 1 -> addHero();
                case 2 -> editHero();
                case 3 -> deleteHero();
                case 0 -> {
                }
                default -> System.out.println("未知选择。");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("操作失败: " + ex.getMessage());
        }
    }

    private void addHero() {
        String id = input.readRequired("英雄ID: ");
        Hero hero = new Hero(id, input.readRequired("名称: "), readHeroType(),
                input.readInt("攻击: ", 0, 200), input.readInt("防御: ", 0, 200),
                input.readInt("技能: ", 0, 200));
        hero.replaceCompatibleEquipmentIds(readIds("兼容装备ID，用逗号分隔: "));
        manager.addHero(hero);
        System.out.println("英雄已添加。");
    }

    private void editHero() {
        Hero hero = manager.findHeroById(input.readRequired("英雄ID: ")).orElse(null);
        if (hero == null) {
            System.out.println("英雄不存在。");
            return;
        }
        hero.setName(input.readOptional("名称", hero.getName()));
        hero.setType(readHeroType());
        hero.setAttack(input.readInt("攻击: ", 0, 200));
        hero.setDefense(input.readInt("防御: ", 0, 200));
        hero.setSkillPower(input.readInt("技能: ", 0, 200));
        hero.replaceCompatibleEquipmentIds(readIds("兼容装备ID，用逗号分隔: "));
        System.out.println("英雄已更新。");
    }

    private void deleteHero() {
        String id = input.readRequired("英雄ID: ");
        System.out.println(manager.deleteHero(id) ? "英雄已删除。" : "英雄不存在。");
    }

    private void manageEquipment() {
        int choice = crudChoice("装备");
        try {
            switch (choice) {
                case 1 -> addEquipment();
                case 2 -> editEquipment();
                case 3 -> deleteEquipment();
                case 0 -> {
                }
                default -> System.out.println("未知选择。");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("操作失败: " + ex.getMessage());
        }
    }

    private void addEquipment() {
        Equipment item = new Equipment(input.readRequired("装备ID: "), input.readRequired("名称: "),
                readEquipmentType(), input.readDouble("评分: ", 0, 10),
                input.readRequired("属性描述: "));
        manager.addEquipment(item);
        System.out.println("装备已添加。");
    }

    private void editEquipment() {
        Equipment item = manager.findEquipmentById(input.readRequired("装备ID: ")).orElse(null);
        if (item == null) {
            System.out.println("装备不存在。");
            return;
        }
        item.setName(input.readOptional("名称", item.getName()));
        item.setType(readEquipmentType());
        item.setRating(input.readDouble("评分: ", 0, 10));
        item.setAttributeDescription(input.readOptional("属性描述", item.getAttributeDescription()));
        System.out.println("装备已更新。");
    }

    private void deleteEquipment() {
        String id = input.readRequired("装备ID: ");
        System.out.println(manager.deleteEquipment(id) ? "装备已删除。" : "装备不存在。");
    }

    private void manageTeams() {
        int choice = crudChoice("战队");
        try {
            switch (choice) {
                case 1 -> {
                    Team team = new Team(input.readRequired("战队ID: "), input.readRequired("名称: "));
                    team.replaceMemberIds(readIds("成员玩家ID，用逗号分隔: "));
                    manager.addTeam(team);
                    System.out.println("战队已添加。");
                }
                case 2 -> {
                    Team team = manager.findTeamById(input.readRequired("战队ID: ")).orElse(null);
                    if (team == null) {
                        System.out.println("战队不存在。");
                        return;
                    }
                    team.setName(input.readOptional("名称", team.getName()));
                    team.replaceMemberIds(readIds("成员玩家ID，用逗号分隔: "));
                    System.out.println("战队已更新。");
                }
                case 3 -> {
                    String id = input.readRequired("战队ID: ");
                    System.out.println(manager.deleteTeam(id) ? "战队已删除。" : "战队不存在。");
                }
                case 0 -> {
                }
                default -> System.out.println("未知选择。");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("操作失败: " + ex.getMessage());
        }
    }

    private void manageMatches() {
        int choice = crudChoice("对战记录");
        try {
            switch (choice) {
                case 1 -> addMatch();
                case 2 -> editMatch();
                case 3 -> {
                    String id = input.readRequired("对战ID: ");
                    System.out.println(manager.deleteMatch(id) ? "对战记录已删除。" : "对战记录不存在。");
                }
                case 0 -> {
                }
                default -> System.out.println("未知选择。");
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("操作失败: " + ex.getMessage());
        }
    }

    private void addMatch() {
        MatchRecord match = new MatchRecord(input.readRequired("对战ID: "),
                input.readDate("日期(yyyy-MM-dd): "),
                input.readRequired("队伍A ID: "), input.readRequired("队伍B ID: "),
                input.readRequired("胜者战队ID: "));
        fillAutoHeroChoices(match);
        manager.addMatch(match);
        System.out.println("对战记录已添加。");
    }

    private void editMatch() {
        String id = input.readRequired("对战ID: ");
        MatchRecord match = manager.getMatches().stream().filter(item -> item.getId().equals(id)).findFirst().orElse(null);
        if (match == null) {
            System.out.println("对战记录不存在。");
            return;
        }
        match.setDate(input.readDate("日期(yyyy-MM-dd): "));
        match.setTeamAId(input.readRequired("队伍A ID: "));
        match.setTeamBId(input.readRequired("队伍B ID: "));
        match.setWinnerTeamId(input.readRequired("胜者战队ID: "));
        fillAutoHeroChoices(match);
        System.out.println("对战记录已更新。");
    }

    private void fillAutoHeroChoices(MatchRecord match) {
        for (Team team : manager.getTeams()) {
            if (!team.getId().equals(match.getTeamAId()) && !team.getId().equals(match.getTeamBId())) {
                continue;
            }
            for (String playerId : team.getMemberIds()) {
                Player player = manager.findPlayerById(playerId).orElse(null);
                if (player != null && !player.getHeroIds().isEmpty()) {
                    match.setHeroChoice(playerId, player.getHeroIds().get(0));
                }
            }
        }
    }

    private int crudChoice(String label) {
        System.out.println("1. 添加" + label);
        System.out.println("2. 编辑" + label);
        System.out.println("3. 删除" + label);
        System.out.println("0. 返回");
        return input.readInt("请选择: ", 0, 3);
    }

    private List<String> readIds(String prompt) {
        String raw = input.readRequired(prompt);
        return List.of(raw.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toList();
    }

    private HeroType readHeroType() {
        System.out.println("英雄类型: TANK, WARRIOR, ASSASSIN, MAGE, MARKSMAN, SUPPORT");
        while (true) {
            try {
                return HeroType.valueOf(input.readRequired("类型: ").toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("英雄类型无效。");
            }
        }
    }

    private EquipmentType readEquipmentType() {
        System.out.println("装备类型: ATTACK, MAGIC, DEFENSE, MOVEMENT, JUNGLE, SUPPORT");
        while (true) {
            try {
                return EquipmentType.valueOf(input.readRequired("类型: ").toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("装备类型无效。");
            }
        }
    }

    private void saveData() {
        try {
            storageService.save(manager, DATA_FILE);
            System.out.println("数据已保存到 " + DATA_FILE);
        } catch (IOException ex) {
            System.out.println("保存失败: " + ex.getMessage());
        }
    }

    private void loadData() {
        try {
            manager = storageService.load(DATA_FILE);
            rebuildServices();
            System.out.println("数据已从 " + DATA_FILE + " 加载。");
        } catch (IOException ex) {
            System.out.println("加载失败: " + ex.getMessage());
        }
    }

    private GameDataManager loadStartupData() {
        try {
            GameDataManager loaded = storageService.load(DATA_FILE);
            System.out.println("已自动加载数据文件 " + DATA_FILE);
            return loaded;
        } catch (IOException ex) {
            System.out.println("未能自动加载 " + DATA_FILE + "，使用内置初始数据。原因: " + ex.getMessage());
            return DataInitializer.createDefaultData();
        }
    }

    private void rebuildServices() {
        recommendationEngine = new RecommendationEngine(manager);
        searchService = new SearchService(manager, recommendationEngine);
        rankingService = new RankingService(manager);
        authService = new AuthenticationService(manager);
    }
}
