package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hero implements Reportable {
    private final String id;
    private String name;
    private HeroType type;
    private int attack;
    private int defense;
    private int skillPower;
    private final List<String> compatibleEquipmentIds;

    public Hero(String id, String name, HeroType type, int attack, int defense, int skillPower) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.attack = attack;
        this.defense = defense;
        this.skillPower = skillPower;
        this.compatibleEquipmentIds = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HeroType getType() {
        return type;
    }

    public void setType(HeroType type) {
        this.type = type;
    }

    public int getAttack() {
        return attack;
    }

    public void setAttack(int attack) {
        this.attack = Math.max(0, attack);
    }

    public int getDefense() {
        return defense;
    }

    public void setDefense(int defense) {
        this.defense = Math.max(0, defense);
    }

    public int getSkillPower() {
        return skillPower;
    }

    public void setSkillPower(int skillPower) {
        this.skillPower = Math.max(0, skillPower);
    }

    public void addCompatibleEquipment(String equipmentId) {
        if (!compatibleEquipmentIds.contains(equipmentId)) {
            compatibleEquipmentIds.add(equipmentId);
        }
    }

    public List<String> getCompatibleEquipmentIds() {
        return Collections.unmodifiableList(compatibleEquipmentIds);
    }

    public void replaceCompatibleEquipmentIds(List<String> equipmentIds) {
        compatibleEquipmentIds.clear();
        for (String equipmentId : equipmentIds) {
            addCompatibleEquipment(equipmentId);
        }
    }

    @Override
    public String generateReport() {
        return String.format("英雄ID: %s%n名称: %s%n类型: %s%n攻击: %d%n防御: %d%n技能: %d",
                id, name, type, attack, defense, skillPower);
    }
}
