package service;

import model.Admin;
import model.Person;
import model.Player;

public class AuthenticationService implements Authenticatable {
    private final GameDataManager manager;
    private Person currentUser;

    public AuthenticationService(GameDataManager manager) {
        this.manager = manager;
    }

    @Override
    public Person login(String username, String password) {
        Admin admin = manager.findAdminByUsername(username).orElse(null);
        if (admin != null && admin.passwordMatches(password)) {
            currentUser = admin;
            return currentUser;
        }

        Player player = manager.findPlayerByUsername(username).orElse(null);
        if (player != null && player.passwordMatches(password)) {
            currentUser = player;
            return currentUser;
        }

        return null;
    }

    @Override
    public void logout() {
        currentUser = null;
    }

    @Override
    public Person getCurrentUser() {
        return currentUser;
    }
}
