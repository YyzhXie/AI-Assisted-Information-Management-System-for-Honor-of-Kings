package service;

import model.Admin;
import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;
import model.MatchRecord;
import model.Player;
import model.Team;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class FileStorageService implements Persistable {
    @Override
    public void save(GameDataManager manager, String filePath) throws IOException {
        Path path = Path.of(filePath);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, toJson(manager), StandardCharsets.UTF_8);
    }

    @Override
    public GameDataManager load(String filePath) throws IOException {
        try {
            String json = Files.readString(Path.of(filePath), StandardCharsets.UTF_8);
            Object parsed = new JsonParser(json).parse();
            if (!(parsed instanceof Map<?, ?> root)) {
                throw new IOException("数据文件格式错误。");
            }
            return fromJson(root);
        } catch (RuntimeException ex) {
            throw new IOException("数据文件内容无法解析: " + ex.getMessage(), ex);
        }
    }

    private String toJson(GameDataManager manager) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\n");
        appendArray(builder, "admins", manager.getAdmins().stream().map(this::adminJson).toList(), true);
        appendArray(builder, "teams", manager.getTeams().stream().map(this::teamJson).toList(), true);
        appendArray(builder, "equipment", manager.getEquipment().stream().map(this::equipmentJson).toList(), true);
        appendArray(builder, "heroes", manager.getHeroes().stream().map(this::heroJson).toList(), true);
        appendArray(builder, "players", manager.getPlayers().stream().map(this::playerJson).toList(), true);
        appendArray(builder, "matches", manager.getMatches().stream().map(this::matchJson).toList(), false);
        builder.append("\n}\n");
        return builder.toString();
    }

    private GameDataManager fromJson(Map<?, ?> root) throws IOException {
        GameDataManager manager = new GameDataManager();
        for (Map<String, Object> item : objectList(root, "admins")) {
            manager.addAdmin(new Admin(text(item, "id"), text(item, "username"), text(item, "password"), text(item, "displayName")));
        }
        for (Map<String, Object> item : objectList(root, "teams")) {
            Team team = new Team(text(item, "id"), text(item, "name"));
            team.replaceMemberIds(stringList(item, "memberIds"));
            manager.addTeam(team);
        }
        for (Map<String, Object> item : objectList(root, "equipment")) {
            manager.addEquipment(new Equipment(text(item, "id"), text(item, "name"),
                    EquipmentType.valueOf(text(item, "type")), number(item, "rating"), text(item, "attributeDescription")));
        }
        for (Map<String, Object> item : objectList(root, "heroes")) {
            Hero hero = new Hero(text(item, "id"), text(item, "name"), HeroType.valueOf(text(item, "type")),
                    integer(item, "attack"), integer(item, "defense"), integer(item, "skillPower"));
            hero.replaceCompatibleEquipmentIds(stringList(item, "compatibleEquipmentIds"));
            manager.addHero(hero);
        }
        for (Map<String, Object> item : objectList(root, "players")) {
            Player player = new Player(text(item, "id"), text(item, "username"), text(item, "password"),
                    text(item, "displayName"), integer(item, "level"), integer(item, "wins"),
                    integer(item, "losses"), text(item, "teamId"));
            player.replaceHeroIds(stringList(item, "heroIds"));
            manager.addPlayer(player);
        }
        for (Map<String, Object> item : objectList(root, "matches")) {
            MatchRecord match = new MatchRecord(text(item, "id"), LocalDate.parse(text(item, "date")),
                    text(item, "teamAId"), text(item, "teamBId"), text(item, "winnerTeamId"));
            Object choices = item.get("playerHeroChoices");
            if (choices instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    match.setHeroChoice(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
            }
            manager.addMatch(match);
        }
        return manager;
    }

    private String adminJson(Admin admin) {
        return object(
                field("id", admin.getId()),
                field("username", admin.getUsername()),
                field("password", admin.getPassword()),
                field("displayName", admin.getDisplayName())
        );
    }

    private String playerJson(Player player) {
        return object(
                field("id", player.getId()),
                field("username", player.getUsername()),
                field("password", player.getPassword()),
                field("displayName", player.getDisplayName()),
                field("level", player.getLevel()),
                field("wins", player.getWins()),
                field("losses", player.getLosses()),
                field("teamId", player.getTeamId()),
                fieldArray("heroIds", player.getHeroIds())
        );
    }

    private String heroJson(Hero hero) {
        return object(
                field("id", hero.getId()),
                field("name", hero.getName()),
                field("type", hero.getType().name()),
                field("attack", hero.getAttack()),
                field("defense", hero.getDefense()),
                field("skillPower", hero.getSkillPower()),
                fieldArray("compatibleEquipmentIds", hero.getCompatibleEquipmentIds())
        );
    }

    private String equipmentJson(Equipment item) {
        return object(
                field("id", item.getId()),
                field("name", item.getName()),
                field("type", item.getType().name()),
                field("rating", item.getRating()),
                field("attributeDescription", item.getAttributeDescription())
        );
    }

    private String teamJson(Team team) {
        return object(
                field("id", team.getId()),
                field("name", team.getName()),
                fieldArray("memberIds", team.getMemberIds())
        );
    }

    private String matchJson(MatchRecord match) {
        return object(
                field("id", match.getId()),
                field("date", match.getDate().toString()),
                field("teamAId", match.getTeamAId()),
                field("teamBId", match.getTeamBId()),
                field("winnerTeamId", match.getWinnerTeamId()),
                fieldMap("playerHeroChoices", match.getPlayerHeroChoices())
        );
    }

    private void appendArray(StringBuilder builder, String name, List<String> values, boolean comma) {
        builder.append("  \"").append(name).append("\": [\n");
        for (int i = 0; i < values.size(); i++) {
            builder.append("    ").append(values.get(i));
            if (i < values.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("  ]");
        if (comma) {
            builder.append(",");
        }
        builder.append("\n");
    }

    private String object(String... fields) {
        return "{" + String.join(", ", fields) + "}";
    }

    private String field(String name, String value) {
        return quote(name) + ": " + quote(value);
    }

    private String field(String name, int value) {
        return quote(name) + ": " + value;
    }

    private String field(String name, double value) {
        return quote(name) + ": " + value;
    }

    private String fieldArray(String name, List<String> values) {
        return quote(name) + ": [" + values.stream().map(this::quote).reduce((a, b) -> a + ", " + b).orElse("") + "]";
    }

    private String fieldMap(String name, Map<String, String> values) {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (Map.Entry<String, String> entry : values.entrySet()) {
            joiner.add(quote(entry.getKey()) + ": " + quote(entry.getValue()));
        }
        return quote(name) + ": " + joiner;
    }

    private String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> objectList(Map<?, ?> root, String key) throws IOException {
        Object value = root.get(key);
        if (!(value instanceof List<?> list)) {
            throw new IOException("缺少数组: " + key);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                throw new IOException("数组元素格式错误: " + key);
            }
            result.add((Map<String, Object>) map);
        }
        return result;
    }

    private String text(Map<String, Object> map, String key) {
        return String.valueOf(map.getOrDefault(key, ""));
    }

    private int integer(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(String.valueOf(value));
    }

    private double number(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number number ? number.doubleValue() : Double.parseDouble(String.valueOf(value));
    }

    private List<String> stringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private static class JsonParser {
        private final String text;
        private int index;

        JsonParser(String text) {
            this.text = text;
        }

        Object parse() throws IOException {
            Object value = parseValue();
            skipWhitespace();
            if (index != text.length()) {
                throw error("JSON 末尾存在多余内容");
            }
            return value;
        }

        private Object parseValue() throws IOException {
            skipWhitespace();
            if (index >= text.length()) {
                throw error("意外结束");
            }
            char c = text.charAt(index);
            if (c == '{') {
                return parseObject();
            }
            if (c == '[') {
                return parseArray();
            }
            if (c == '"') {
                return parseString();
            }
            return parseNumber();
        }

        private Map<String, Object> parseObject() throws IOException {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (peek('}')) {
                expect('}');
                return map;
            }
            while (true) {
                String key = parseString();
                expect(':');
                map.put(key, parseValue());
                skipWhitespace();
                if (peek('}')) {
                    expect('}');
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() throws IOException {
            expect('[');
            List<Object> values = new ArrayList<>();
            skipWhitespace();
            if (peek(']')) {
                expect(']');
                return values;
            }
            while (true) {
                values.add(parseValue());
                skipWhitespace();
                if (peek(']')) {
                    expect(']');
                    return values;
                }
                expect(',');
            }
        }

        private String parseString() throws IOException {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (index < text.length()) {
                char c = text.charAt(index++);
                if (c == '"') {
                    return builder.toString();
                }
                if (c == '\\') {
                    if (index >= text.length()) {
                        throw error("字符串转义不完整");
                    }
                    char escaped = text.charAt(index++);
                    switch (escaped) {
                        case '"' -> builder.append('"');
                        case '\\' -> builder.append('\\');
                        case 'n' -> builder.append('\n');
                        default -> builder.append(escaped);
                    }
                } else {
                    builder.append(c);
                }
            }
            throw error("字符串未闭合");
        }

        private Number parseNumber() throws IOException {
            int start = index;
            while (index < text.length()) {
                char c = text.charAt(index);
                if (!Character.isDigit(c) && c != '-' && c != '.') {
                    break;
                }
                index++;
            }
            if (start == index) {
                throw error("需要数值");
            }
            String raw = text.substring(start, index);
            return raw.contains(".") ? Double.parseDouble(raw) : Integer.parseInt(raw);
        }

        private void expect(char expected) throws IOException {
            skipWhitespace();
            if (index >= text.length() || text.charAt(index) != expected) {
                throw error("需要字符: " + expected);
            }
            index++;
        }

        private boolean peek(char expected) {
            skipWhitespace();
            return index < text.length() && text.charAt(index) == expected;
        }

        private void skipWhitespace() {
            while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
                index++;
            }
        }

        private IOException error(String message) {
            return new IOException(message + "，位置 " + index);
        }
    }
}
