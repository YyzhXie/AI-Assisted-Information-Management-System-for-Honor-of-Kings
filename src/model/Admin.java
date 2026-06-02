package model;

public class Admin extends Person {
    public Admin(String id, String username, String password, String displayName) {
        super(id, username, password, displayName, Role.ADMIN);
    }

    @Override
    public String generateReport() {
        return "管理员ID: " + getId() + "\n用户名: " + getUsername() + "\n显示名: " + getDisplayName();
    }
}
