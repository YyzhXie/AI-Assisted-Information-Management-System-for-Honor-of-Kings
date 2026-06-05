import gui.VisualizationFrame;
import service.FileStorageService;
import service.GameDataManager;
import util.DataInitializer;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.IOException;

public class GuiMain {
    private static final String DATA_FILE = "data/game-data.json";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setLookAndFeel();
            VisualizationFrame frame = new VisualizationFrame(loadStartupData());
            frame.setVisible(true);
        });
    }

    private static GameDataManager loadStartupData() {
        try {
            return new FileStorageService().load(DATA_FILE);
        } catch (IOException ex) {
            return DataInitializer.createDefaultData();
        }
    }

    private static void setLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Swing can continue with the default cross-platform look and feel.
        }
    }
}
