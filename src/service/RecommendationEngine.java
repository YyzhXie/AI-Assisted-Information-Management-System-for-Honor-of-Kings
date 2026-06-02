package service;

import model.Equipment;
import model.EquipmentType;
import model.Hero;
import model.HeroType;

import java.util.Comparator;
import java.util.List;

public class RecommendationEngine {
    private final GameDataManager manager;

    public RecommendationEngine(GameDataManager manager) {
        this.manager = manager;
    }

    public List<Equipment> recommendForHero(Hero hero, int limit) {
        EquipmentType preferred = preferredType(hero.getType());
        return hero.getCompatibleEquipmentIds().stream()
                .map(id -> manager.findEquipmentById(id).orElse(null))
                .filter(item -> item != null)
                .sorted(Comparator
                        .comparing((Equipment item) -> item.getType() == preferred).reversed()
                        .thenComparing(Equipment::getRating, Comparator.reverseOrder())
                        .thenComparing(Equipment::getId))
                .limit(limit)
                .toList();
    }

    private EquipmentType preferredType(HeroType type) {
        return switch (type) {
            case TANK -> EquipmentType.DEFENSE;
            case MAGE -> EquipmentType.MAGIC;
            case SUPPORT -> EquipmentType.SUPPORT;
            case ASSASSIN, MARKSMAN, WARRIOR -> EquipmentType.ATTACK;
        };
    }
}
