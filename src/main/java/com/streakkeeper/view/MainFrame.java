// FILE: src/main/java/com/streakkeeper/view/MainFrame.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

import com.streakkeeper.model.StreakModel;
import com.streakkeeper.util.ThemeManager;

import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Path;

public class MainFrame extends JFrame {

    private final TopBar       topBar       = new TopBar();
    private final Sidebar      sidebar      = new Sidebar();
    private final CounterPanel counterPanel = new CounterPanel();
    private final CelebrationOverlay celebrationOverlay = new CelebrationOverlay();

    private Runnable onNewCounter;
    private Runnable onResetSelected;
    private Runnable onRenameSelected;
    private Runnable onDeleteSelected;
    private Runnable onToggleTheme;
    private Runnable onToggleSidebar;
    private Runnable onExport;
    private Runnable onImport;
    private Runnable onExit;

    private boolean currentDark;

    public MainFrame() {
        super("StreakKeeper");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (onExit != null) onExit.run();
            }
        });

        setMinimumSize(new Dimension(820, 620));
        setSize(1080, 800);
        setLocationRelativeTo(null);

        // Install the lightweight particle overlay
        setGlassPane(celebrationOverlay);

        setJMenuBar(buildMenuBar());

        JPanel body = new JPanel(new BorderLayout());
        body.add(sidebar, BorderLayout.WEST);
        body.add(counterPanel, BorderLayout.CENTER);

        JPanel root = new JPanel(new BorderLayout());
        root.add(topBar, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        setContentPane(root);

        installGlobalShortcuts();

        topBar.setOnNewCounter(() -> { if (onNewCounter != null) onNewCounter.run(); });
        topBar.setOnThemeToggle(() -> { if (onToggleTheme != null) onToggleTheme.run(); });
        topBar.setOnSidebarToggle(() -> { if (onToggleSidebar != null) onToggleSidebar.run(); });
    }

    public TopBar       getTopBar()       { return topBar; }
    public Sidebar      getSidebar()      { return sidebar; }
    public CounterPanel getCounterPanel() { return counterPanel; }
    public HoldButton   getHoldButton()   { return counterPanel.getHoldButton(); }

    public void setOnNewCounter(Runnable r)      { this.onNewCounter = r; }
    public void setOnResetSelected(Runnable r)   { this.onResetSelected = r; }
    public void setOnRenameSelected(Runnable r)  { this.onRenameSelected = r; }
    public void setOnDeleteSelected(Runnable r)  { this.onDeleteSelected = r; }
    public void setOnToggleTheme(Runnable r)     { this.onToggleTheme = r; }
    public void setOnToggleSidebar(Runnable r)   { this.onToggleSidebar = r; }
    public void setOnExport(Runnable r)          { this.onExport = r; }
    public void setOnImport(Runnable r)          { this.onImport = r; }
    public void setOnExit(Runnable r)            { this.onExit = r; }

    public void updateFromModel(StreakModel model) {
        if (model.isDarkTheme() != currentDark) {
            currentDark = model.isDarkTheme();
            ThemeManager.applyTheme(currentDark);
        }
        topBar.setDarkTheme(model.isDarkTheme());
        sidebar.updateFromModel(model);
        sidebar.setExpanded(model.isSidebarExpanded());
        counterPanel.updateFromModel(model);
    }

    public void primeSidebarState(boolean expanded) {
        sidebar.setExpandedInstant(expanded);
    }

    // ------------------------------------------------------ animations

    /** Triggers the particle and ring celebration overlay. */
    public void triggerCelebration() {
        HoldButton btn = getCounterPanel().getHoldButton();
        Point p = btn.getLocationOnScreen();
        Point frameP = getLocationOnScreen();

        double centerX = p.x - frameP.x + btn.getWidth() / 2.0;
        double centerY = p.y - frameP.y + btn.getHeight() / 2.0;

        Color waveColor = btn.getCurrentColor();

        celebrationOverlay.playCelebration(new Point2D.Double(centerX, centerY), waveColor);
    }

    // --------------------------------------------------------- dialogs

    public String promptForName(String initial) {
        return JOptionPane.showInputDialog(this, "Name:", initial);
    }

    public String promptForStartValue(String initial) {
        return JOptionPane.showInputDialog(this, "Start value:", initial);
    }

    public boolean confirmReset(String counterName) {
        int r = JOptionPane.showConfirmDialog(this,
                "Reset \"" + counterName + "\" back to its start value?\n"
                        + "This will clear its current streak.",
                "Confirm reset", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    public boolean confirmDelete(String counterName) {
        int r = JOptionPane.showConfirmDialog(this,
                "Delete \"" + counterName + "\"?\nThis cannot be undone.",
                "Confirm delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return r == JOptionPane.YES_OPTION;
    }

    public Path chooseExportFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export counters");
        fc.setSelectedFile(new File("streakkeeper-export.json"));
        int r = fc.showSaveDialog(this);
        return r == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile().toPath() : null;
    }

    public Path chooseImportFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Import counters");
        int r = fc.showOpenDialog(this);
        return r == JFileChooser.APPROVE_OPTION ? fc.getSelectedFile().toPath() : null;
    }

    public void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // -------------------------------------------------------- menu bar

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");

        JMenuItem newItem = new JMenuItem("New Counter");
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        newItem.addActionListener(e -> { if (onNewCounter != null) onNewCounter.run(); });
        file.add(newItem);

        JMenuItem renameItem = new JMenuItem("Rename Counter…");
        renameItem.addActionListener(e -> { if (onRenameSelected != null) onRenameSelected.run(); });
        file.add(renameItem);

        JMenuItem deleteItem = new JMenuItem("Delete Counter…");
        deleteItem.addActionListener(e -> { if (onDeleteSelected != null) onDeleteSelected.run(); });
        file.add(deleteItem);

        JMenuItem resetItem = new JMenuItem("Reset Counter");
        resetItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK));
        resetItem.addActionListener(e -> { if (onResetSelected != null) onResetSelected.run(); });
        file.add(resetItem);

        file.addSeparator();

        JMenuItem exportItem = new JMenuItem("Export…");
        exportItem.addActionListener(e -> { if (onExport != null) onExport.run(); });
        file.add(exportItem);

        JMenuItem importItem = new JMenuItem("Import…");
        importItem.addActionListener(e -> { if (onImport != null) onImport.run(); });
        file.add(importItem);

        file.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> { if (onExit != null) onExit.run(); });
        file.add(exitItem);

        JMenu view = new JMenu("View");

        JMenuItem themeItem = new JMenuItem("Toggle Theme");
        themeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK));
        themeItem.addActionListener(e -> { if (onToggleTheme != null) onToggleTheme.run(); });
        view.add(themeItem);

        JMenuItem sidebarItem = new JMenuItem("Toggle Sidebar");
        sidebarItem.addActionListener(e -> { if (onToggleSidebar != null) onToggleSidebar.run(); });
        view.add(sidebarItem);

        bar.add(file);
        bar.add(view);
        return bar;
    }

    private void installGlobalShortcuts() {
        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "newCounter");
        getRootPane().getActionMap().put("newCounter", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onNewCounter != null) onNewCounter.run();
            }
        });

        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK), "reset");
        getRootPane().getActionMap().put("reset", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onResetSelected != null) onResetSelected.run();
            }
        });

        getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_T, InputEvent.CTRL_DOWN_MASK), "toggleTheme");
        getRootPane().getActionMap().put("toggleTheme", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                if (onToggleTheme != null) onToggleTheme.run();
            }
        });
    }
}