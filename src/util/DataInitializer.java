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
        manager.addAdmin(new Admin("a001", "admin", "admin123", "系统管理员"));
        manager.addAdmin(new Admin("a002", "coach", "coach123", "战术教练"));
        addEquipment(manager);
        addHeroes(manager);
        addTeams(manager);
        addPlayers(manager);
        addMatches(manager);
        return manager;
    }

    private static void addEquipment(GameDataManager manager) {
        Object[][] rows = {
                {"e001", "破军", EquipmentType.ATTACK, 9.8, "高额物理攻击"},
                {"e002", "无尽战刃", EquipmentType.ATTACK, 9.4, "暴击和攻击"},
                {"e003", "暗影战斧", EquipmentType.ATTACK, 8.9, "穿透和冷却"},
                {"e004", "泣血之刃", EquipmentType.ATTACK, 8.4, "吸血"},
                {"e005", "宗师之力", EquipmentType.ATTACK, 8.7, "强化普攻"},
                {"e006", "博学者之怒", EquipmentType.MAGIC, 9.7, "法术强度"},
                {"e007", "回响之杖", EquipmentType.MAGIC, 8.8, "爆发法术"},
                {"e008", "虚无法杖", EquipmentType.MAGIC, 8.9, "法术穿透"},
                {"e009", "辉月", EquipmentType.MAGIC, 8.3, "主动保护"},
                {"e010", "痛苦面具", EquipmentType.MAGIC, 8.6, "持续伤害"},
                {"e011", "不祥征兆", EquipmentType.DEFENSE, 9.1, "生命和护甲"},
                {"e012", "魔女斗篷", EquipmentType.DEFENSE, 9.0, "法术防御"},
                {"e013", "霸者重装", EquipmentType.DEFENSE, 8.5, "回复生命"},
                {"e014", "反伤刺甲", EquipmentType.DEFENSE, 8.7, "反弹伤害"},
                {"e015", "抵抗之靴", EquipmentType.MOVEMENT, 8.8, "韧性"},
                {"e016", "急速战靴", EquipmentType.MOVEMENT, 8.2, "攻速"},
                {"e017", "贪婪之噬", EquipmentType.JUNGLE, 8.6, "打野成长"},
                {"e018", "巨人之握", EquipmentType.JUNGLE, 8.3, "打野生命"},
                {"e019", "近卫荣耀", EquipmentType.SUPPORT, 8.9, "团队防御"},
                {"e020", "救赎之翼", EquipmentType.SUPPORT, 8.8, "团队护盾"}
        };
        for (Object[] row : rows) {
            manager.addEquipment(new Equipment((String) row[0], (String) row[1], (EquipmentType) row[2], (double) row[3], (String) row[4]));
        }
    }

    private static void addHeroes(GameDataManager manager) {
        addHero(manager, "h001", "亚瑟", HeroType.WARRIOR, 82, 74, 58, List.of("e001", "e003", "e011", "e015"));
        addHero(manager, "h002", "妲己", HeroType.MAGE, 42, 38, 92, List.of("e006", "e007", "e008", "e009"));
        addHero(manager, "h003", "后羿", HeroType.MARKSMAN, 90, 36, 50, List.of("e001", "e002", "e004", "e016"));
        addHero(manager, "h004", "赵云", HeroType.ASSASSIN, 88, 62, 64, List.of("e003", "e005", "e017", "e015"));
        addHero(manager, "h005", "庄周", HeroType.SUPPORT, 48, 78, 70, List.of("e019", "e020", "e012", "e015"));
        addHero(manager, "h006", "廉颇", HeroType.TANK, 58, 95, 44, List.of("e011", "e012", "e013", "e014"));
        addHero(manager, "h007", "孙悟空", HeroType.ASSASSIN, 94, 52, 66, List.of("e001", "e002", "e005", "e017"));
        addHero(manager, "h008", "王昭君", HeroType.MAGE, 38, 42, 95, List.of("e006", "e007", "e010", "e009"));
        addHero(manager, "h009", "鲁班七号", HeroType.MARKSMAN, 96, 30, 48, List.of("e001", "e002", "e004", "e016"));
        addHero(manager, "h010", "张飞", HeroType.TANK, 50, 98, 56, List.of("e011", "e012", "e019", "e020"));
        addHero(manager, "h011", "貂蝉", HeroType.MAGE, 46, 48, 98, List.of("e006", "e008", "e009", "e010"));
        addHero(manager, "h012", "吕布", HeroType.WARRIOR, 92, 80, 62, List.of("e001", "e003", "e011", "e014"));
        addHero(manager, "h013", "孙尚香", HeroType.MARKSMAN, 93, 40, 58, List.of("e001", "e002", "e005", "e016"));
        addHero(manager, "h014", "蔡文姬", HeroType.SUPPORT, 34, 52, 88, List.of("e019", "e020", "e009", "e015"));
        addHero(manager, "h015", "李白", HeroType.ASSASSIN, 95, 44, 78, List.of("e001", "e003", "e005", "e017"));
    }

    private static void addHero(GameDataManager manager, String id, String name, HeroType type, int attack, int defense, int skill, List<String> equipmentIds) {
        Hero hero = new Hero(id, name, type, attack, defense, skill);
        for (String equipmentId : equipmentIds) {
            hero.addCompatibleEquipment(equipmentId);
        }
        manager.addHero(hero);
    }

    private static void addTeams(GameDataManager manager) {
        manager.addTeam(new Team("t001", "长安星火"));
        manager.addTeam(new Team("t002", "稷下风暴"));
        manager.addTeam(new Team("t003", "峡谷晨光"));
    }

    private static void addPlayers(GameDataManager manager) {
        addPlayer(manager, "P001", "阿离同学", 31, 28, 12, "t001", List.of("h001", "h003", "h015"));
        addPlayer(manager, "P002", "峡谷小周", 27, 22, 13, "t001", List.of("h002", "h008", "h011"));
        addPlayer(manager, "P003", "青铜到王者", 35, 32, 18, "t001", List.of("h004", "h007", "h012"));
        addPlayer(manager, "P004", "河道巡逻员", 24, 18, 16, "t001", List.of("h005", "h006", "h010"));
        addPlayer(manager, "P005", "红蓝都要", 29, 26, 15, "t001", List.of("h003", "h009", "h013"));
        addPlayer(manager, "P006", "法术核心", 33, 30, 17, "t002", List.of("h002", "h008", "h011"));
        addPlayer(manager, "P007", "野区节奏", 34, 31, 14, "t002", List.of("h004", "h007", "h015"));
        addPlayer(manager, "P008", "边路很稳", 28, 21, 19, "t002", List.of("h001", "h012", "h006"));
        addPlayer(manager, "P009", "辅助之光", 25, 20, 18, "t002", List.of("h005", "h010", "h014"));
        addPlayer(manager, "P010", "输出机器", 32, 29, 16, "t002", List.of("h003", "h009", "h013"));
        addPlayer(manager, "P011", "沉着指挥", 30, 25, 15, "t003", List.of("h001", "h010", "h014"));
        addPlayer(manager, "P012", "闪现向前", 36, 35, 16, "t003", List.of("h004", "h007", "h015"));
        addPlayer(manager, "P013", "草丛观察者", 22, 15, 17, "t003", List.of("h002", "h005", "h008"));
        addPlayer(manager, "P014", "稳定发育", 26, 19, 14, "t003", List.of("h003", "h009", "h013"));
        addPlayer(manager, "P015", "团战开关", 31, 27, 13, "t003", List.of("h006", "h010", "h012"));
    }

    private static void addPlayer(GameDataManager manager, String id, String name, int level, int wins, int losses, String teamId, List<String> heroes) {
        Player player = new Player(id, id, "123456", name, level, wins, losses, teamId);
        for (String heroId : heroes) {
            player.addHero(heroId);
        }
        manager.addPlayer(player);
    }

    private static void addMatches(GameDataManager manager) {
        addMatch(manager, "m001", "2026-05-01", "t001", "t002", "t001");
        addMatch(manager, "m002", "2026-05-03", "t002", "t003", "t003");
        addMatch(manager, "m003", "2026-05-05", "t001", "t003", "t001");
        addMatch(manager, "m004", "2026-05-07", "t003", "t001", "t003");
        addMatch(manager, "m005", "2026-05-09", "t002", "t001", "t002");
        addMatch(manager, "m006", "2026-05-11", "t003", "t002", "t002");
        addMatch(manager, "m007", "2026-05-13", "t001", "t002", "t001");
        addMatch(manager, "m008", "2026-05-15", "t002", "t003", "t002");
        addMatch(manager, "m009", "2026-05-17", "t003", "t001", "t001");
        addMatch(manager, "m010", "2026-05-19", "t001", "t003", "t003");
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
