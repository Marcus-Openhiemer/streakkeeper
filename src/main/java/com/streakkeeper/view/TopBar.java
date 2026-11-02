// FILE: src/main/java/com/streakkeeper/view/TopBar.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

/**
 * Thin top strip: sidebar toggle, "new counter", title, theme toggle.
 * Pure view — it only surfaces callbacks for the controller to wire.
 */
public class TopBar extends JPanel {

    private final JButton sidebarToggle = new JButton("☰");
    private final JButton newCounterBtn = new JButton("+ New Counter");
    private final JLabel  title         = new JLabel("StreakKeeper", SwingConstants.CENTER);
    private final JButton themeToggle   = new JButton("🌙");

    private Runnable onSidebarToggle;
    private Runnable onNewCounter;
    private Runnable onThemeToggle;

    public TopBar() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        sidebarToggle.setToolTipText("Toggle sidebar");
        newCounterBtn.setToolTipText("Create a new counter  (Ctrl+N)");
        themeToggle.setToolTipText("Toggle light / dark theme  (Ctrl+T)");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        left.add(sidebarToggle);
        left.add(newCounterBtn);

        add(left, BorderLayout.WEST);
        add(title, BorderLayout.CENTER);
        add(themeToggle, BorderLayout.EAST);

        sidebarToggle.addActionListener(e -> { if (onSidebarToggle != null) onSidebarToggle.run(); });
        newCounterBtn.addActionListener(e -> { if (onNewCounter != null) onNewCounter.run(); });
        themeToggle.addActionListener(e -> { if (onThemeToggle != null) onThemeToggle.run(); });
    }

    public void setOnSidebarToggle(Runnable r) { this.onSidebarToggle = r; }
    public void setOnNewCounter(Runnable r)    { this.onNewCounter = r; }
    public void setOnThemeToggle(Runnable r)   { this.onThemeToggle = r; }

    /** Flips the icon to match the active theme. */
    public void setDarkTheme(boolean dark) {
        themeToggle.setText(dark ? "☀" : "🌙");
    }
}