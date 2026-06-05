package util;

import model.Admin;
import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Player;
import model.Team;
import service.GameDataManager;

import java.time.LocalDate;
import java.util.List;

public class DataInitializer {
    private DataInitializer() {
    }

    public static GameDataManager createDefaultData() {
        GameDataManager manager = new GameDataManager();
        manager.addAdmin(new Admin("A001", "admin", "admin123", "系统管理员"));
        manager.addAdmin(new Admin("A002", "coach", "coach123", "战术教练"));
        addEquipment(manager);
        addHeroes(manager);
        addTeams(manager);
        addPlayers(manager);
        addMatches(manager);
        return manager;
    }

    private static void addEquipment(GameDataManager manager) {
        Object[][] rows = {
                {"E001", "破军", EquipmentType.ATTACK, 9.8, "高额物理攻击"},
                {"E002", "无尽战刃", EquipmentType.ATTACK, 9.4, "暴击和攻击"},
                {"E003", "暗影战斧", EquipmentType.ATTACK, 8.9, "穿透和冷却"},
                {"E004", "泣血之刃", EquipmentType.ATTACK, 8.4, "吸血"},
                {"E005", "宗师之力", EquipmentType.ATTACK, 8.7, "强化普攻"},
                {"E006", "博学者之怒", EquipmentType.MAGIC, 9.7, "法术强度"},
                {"E007", "回响之杖", EquipmentType.MAGIC, 8.8, "爆发法术"},
                {"E008", "虚无法杖", EquipmentType.MAGIC, 8.9, "法术穿透"},
                {"E009", "辉月", EquipmentType.MAGIC, 8.3, "主动保护"},
                {"E010", "痛苦面具", EquipmentType.MAGIC, 8.6, "持续伤害"},
                {"E011", "不祥征兆", EquipmentType.DEFENSE, 9.1, "生命和护甲"},
                {"E012", "魔女斗篷", EquipmentType.DEFENSE, 9.0, "法术防御"},
                {"E013", "霸者重装", EquipmentType.DEFENSE, 8.5, "回复生命"},
                {"E014", "反伤刺甲", EquipmentType.DEFENSE, 8.7, "反弹伤害"},
                {"E015", "抵抗之靴", EquipmentType.MOVEMENT, 8.8, "韧性"},
                {"E016", "急速战靴", EquipmentType.MOVEMENT, 8.2, "攻速"},
                {"E017", "贪婪之噬", EquipmentType.JUNGLE, 8.6, "打野成长"},
                {"E018", "巨人之握", EquipmentType.JUNGLE, 8.3, "打野生命"},
                {"E019", "近卫荣耀", EquipmentType.SUPPORT, 8.9, "团队防御"},
                {"E020", "救赎之翼", EquipmentType.SUPPORT, 8.8, "团队护盾"}
        };
        for (Object[] row : rows) {
            manager.addEquipment(new Equipment((String) row[0], (String) row[1], (EquipmentType) row[2], (double) row[3], (String) row[4]));
        }
    }

    private static void addHeroes(GameDataManager manager) {
        addHero(manager, "H001", "亚瑟", HeroType.WARRIOR, 82, 74, 58, List.of("E001", "E003", "E011", "E015"));
        addHero(manager, "H002", "妲己", HeroType.MAGE, 42, 38, 92, List.of("E006", "E007", "E008", "E009"));
        addHero(manager, "H003", "后羿", HeroType.MARKSMAN, 90, 36, 50, List.of("E001", "E002", "E004", "E016"));
        addHero(manager, "H004", "赵云", HeroType.ASSASSIN, 88, 62, 64, List.of("E003", "E005", "E017", "E015"));
        addHero(manager, "H005", "庄周", HeroType.SUPPORT, 48, 78, 70, List.of("E019", "E020", "E012", "E015"));
        addHero(manager, "H006", "廉颇", HeroType.TANK, 58, 95, 44, List.of("E011", "E012", "E013", "E014"));
        addHero(manager, "H007", "孙悟空", HeroType.ASSASSIN, 94, 52, 66, List.of("E001", "E002", "E005", "E017"));
        addHero(manager, "H008", "王昭君", HeroType.MAGE, 38, 42, 95, List.of("E006", "E007", "E010", "E009"));
        addHero(manager, "H009", "鲁班七号", HeroType.MARKSMAN, 96, 30, 48, List.of("E001", "E002", "E004", "E016"));
        addHero(manager, "H010", "张飞", HeroType.TANK, 50, 98, 56, List.of("E011", "E012", "E019", "E020"));
        addHero(manager, "H011", "貂蝉", HeroType.MAGE, 46, 48, 98, List.of("E006", "E008", "E009", "E010"));
        addHero(manager, "H012", "吕布", HeroType.WARRIOR, 92, 80, 62, List.of("E001", "E003", "E011", "E014"));
        addHero(manager, "H013", "孙尚香", HeroType.MARKSMAN, 93, 40, 58, List.of("E001", "E002", "E005", "E016"));
        addHero(manager, "H014", "蔡文姬", HeroType.SUPPORT, 34, 52, 88, List.of("E019", "E020", "E009", "E015"));
        addHero(manager, "H015", "李白", HeroType.ASSASSIN, 95, 44, 78, List.of("E001", "E003", "E005", "E017"));
    }

    private static void addHero(GameDataManager manager, String id, String name, HeroType type, int attack, int defense, int skill, List<String> equipmentIds) {
        Hero hero = new Hero(id, name, type, attack, defense, skill);
        for (String equipmentId : equipmentIds) {
            hero.addCompatibleEquipment(equipmentId);
        }
        manager.addHero(hero);
    }

    private static void addTeams(GameDataManager manager) {
        manager.addTeam(new Team("T001", "长安星火"));
        manager.addTeam(new Team("T002", "稷下风暴"));
        manager.addTeam(new Team("T003", "峡谷晨光"));
    }

    private static void addPlayers(GameDataManager manager) {
        addPlayer(manager, "P001", "阿离同学", 31, 28, 12, "T001", List.of("H001", "H003", "H015"));
        addPlayer(manager, "P002", "峡谷小周", 27, 22, 13, "T001", List.of("H002", "H008", "H011"));
        addPlayer(manager, "P003", "青铜到王者", 35, 32, 18, "T001", List.of("H004", "H007", "H012"));
        addPlayer(manager, "P004", "河道巡逻员", 24, 18, 16, "T001", List.of("H005", "H006", "H010"));
        addPlayer(manager, "P005", "红蓝都要", 29, 26, 15, "T001", List.of("H003", "H009", "H013"));
        addPlayer(manager, "P006", "法术核心", 33, 30, 17, "T002", List.of("H002", "H008", "H011"));
        addPlayer(manager, "P007", "野区节奏", 34, 31, 14, "T002", List.of("H004", "H007", "H015"));
        addPlayer(manager, "P008", "边路很稳", 28, 21, 19, "T002", List.of("H001", "H012", "H006"));
        addPlayer(manager, "P009", "辅助之光", 25, 20, 18, "T002", List.of("H005", "H010", "H014"));
        addPlayer(manager, "P010", "输出机器", 32, 29, 16, "T002", List.of("H003", "H009", "H013"));
        addPlayer(manager, "P011", "沉着指挥", 30, 25, 15, "T003", List.of("H001", "H010", "H014"));
        addPlayer(manager, "P012", "闪现向前", 36, 35, 16, "T003", List.of("H004", "H007", "H015"));
        addPlayer(manager, "P013", "草丛观察者", 22, 15, 17, "T003", List.of("H002", "H005", "H008"));
        addPlayer(manager, "P014", "稳定发育", 26, 19, 14, "T003", List.of("H003", "H009", "H013"));
        addPlayer(manager, "P015", "团战开关", 31, 27, 13, "T003", List.of("H006", "H010", "H012"));
    }

    private static void addPlayer(GameDataManager manager, String id, String name, int level, int wins, int losses, String teamId, List<String> heroes) {
        Player player = new Player(id, id, "123456", name, level, wins, losses, teamId);
        for (String heroId : heroes) {
            player.addHero(heroId);
        }
        manager.addPlayer(player);
    }

    private static void addMatches(GameDataManager manager) {
        addMatch(manager, "M001", "2026-05-01", "T001", "T002", "T001");
        addMatch(manager, "M002", "2026-05-03", "T002", "T003", "T003");
        addMatch(manager, "M003", "2026-05-05", "T001", "T003", "T001");
        addMatch(manager, "M004", "2026-05-07", "T003", "T001", "T003");
        addMatch(manager, "M005", "2026-05-09", "T002", "T001", "T002");
        addMatch(manager, "M006", "2026-05-11", "T003", "T002", "T002");
        addMatch(manager, "M007", "2026-05-13", "T001", "T002", "T001");
        addMatch(manager, "M008", "2026-05-15", "T002", "T003", "T002");
        addMatch(manager, "M009", "2026-05-17", "T003", "T001", "T001");
        addMatch(manager, "M010", "2026-05-19", "T001", "T003", "T003");
    }

    private static void addMatch(GameDataManager manager, String id, String date, String teamA, String teamB, String winner) {
        MatchRecord match = new MatchRecord(id, LocalDate.parse(date), teamA, teamB, winner);
        for (Team team : manager.getTeams()) {
            if (team.getId().equals(teamA) || team.getId().equals(teamB)) {
                for (String playerId : team.getMemberIds()) {
                    Player player = manager.findPlayerById(playerId).orElse(null);
                    if (player != null && !player.getHeroIds().isEmpty()) {
                        int index = Math.abs((id + playerId).hashCode()) % player.getHeroIds().size();
                        match.setHeroChoice(playerId, player.getHeroIds().get(index));
                    }
                }
            }
        }
        manager.addMatch(match);
    }
}
