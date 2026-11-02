// FILE: src/main/java/com/streakkeeper/Main.java
// PACKAGE: com.streakkeeper
package com.streakkeeper;

import com.streakkeeper.controller.AppController;
import com.streakkeeper.model.JsonPersistenceService;
import com.streakkeeper.model.PersistenceService;
import com.streakkeeper.model.StreakModel;
import com.streakkeeper.util.ThemeManager;
import com.streakkeeper.view.MainFrame;

import javax.swing.SwingUtilities;

/**
 * Application entry point.
 *
 * <p>Why we bootstrap this way: everything Swing-related must run on the EDT.
 * We install the Look &amp; Feel (FlatLaf) BEFORE constructing any Swing
 * component so that defaults are consistent, then load persisted state (so the
 * theme that was last chosen is respected on very first paint) and only then
 * create the frame.</p>
 */
public final class Main {

    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // 1) L&F first
            ThemeManager.init();

            // 2) Load persisted state (counters, theme, sidebar state)
            PersistenceService persistence = new JsonPersistenceService();
            StreakModel model = persistence.load();

            // 3) Apply the persisted theme before any window is created
            ThemeManager.applyTheme(model.isDarkTheme());

            // 4) Build view + controller
            MainFrame frame = new MainFrame();
            AppController controller = new AppController(model, frame, persistence);
            controller.init();

            frame.setVisible(true);
        });
    }
}