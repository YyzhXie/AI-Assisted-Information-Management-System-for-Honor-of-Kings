package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Team implements Reportable {
    private final String id;
    private String name;
    private final List<String> memberIds;

    public Team(String id, String name) {
        this.id = id;
        this.name = name;
        this.memberIds = new ArrayList<>();
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

    public void addMember(String playerId) {
        if (!memberIds.contains(playerId)) {
            memberIds.add(playerId);
        }
    }

    public void removeMember(String playerId) {
        memberIds.remove(playerId);
    }

    public List<String> getMemberIds() {
        return Collections.unmodifiableList(memberIds);
    }

    public void replaceMemberIds(List<String> ids) {
        memberIds.clear();
        for (String id : ids) {
            addMember(id);
        }
    }

    @Override
    public String generateReport() {
        return "战队ID: " + id + "\n名称: " + name + "\n成员数量: " + memberIds.size();
    }
}
