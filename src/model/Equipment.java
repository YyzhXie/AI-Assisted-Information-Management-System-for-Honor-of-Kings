package model;

public class Equipment implements Reportable {
    private final String id;
    private String name;
    private EquipmentType type;
    private double rating;
    private String attributeDescription;

    public Equipment(String id, String name, EquipmentType type, double rating, String attributeDescription) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.rating = rating;
        this.attributeDescription = attributeDescription;
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

    public EquipmentType getType() {
        return type;
    }

    public void setType(EquipmentType type) {
        this.type = type;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = Math.max(0.0, rating);
    }

    public String getAttributeDescription() {
        return attributeDescription;
    }

    public void setAttributeDescription(String attributeDescription) {
        this.attributeDescription = attributeDescription;
    }

    @Override
    public String generateReport() {
        return String.format("装备ID: %s%n名称: %s%n类型: %s%n评分: %.1f%n属性: %s",
                id, name, type, rating, attributeDescription);
    }
}
