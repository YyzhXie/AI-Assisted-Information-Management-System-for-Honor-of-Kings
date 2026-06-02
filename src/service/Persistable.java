package service;

import java.io.IOException;

public interface Persistable {
    void save(GameDataManager manager, String filePath) throws IOException;
    GameDataManager load(String filePath) throws IOException;
}
