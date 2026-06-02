package service;

import model.Person;

public interface Authenticatable {
    Person login(String username, String password);
    void logout();
    Person getCurrentUser();
}
