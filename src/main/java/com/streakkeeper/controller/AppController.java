// FILE: src/main/java/com/streakkeeper/controller/AppController.java
// PACKAGE: com.streakkeeper.controller
package com.streakkeeper.controller;

import com.streakkeeper.model.Counter;
import com.streakkeeper.model.PersistenceService;
import com.streakkeeper.model.StreakModel;
import com.streakkeeper.view.HoldListener;
import com.streakkeeper.view.MainFrame;

import java.nio.file.Path;

/**
 * The controller glue. It:
 *   • registers itself as a {@link com.streakkeeper.model.ModelListener},
 *   • wires view callbacks to model mutations,
 *   • mediates the {@link HoldListener} so hold-complete leads to
 *     "model decides → view animates accordingly".
 *
 * <p>This class deliberately does NOT import Swing: all modals and dialogs are
 * encapsulated as {@code MainFrame} methods. Only domain/view-specific types
 * are imported.</p>
 */
public class AppController {

    private final StreakModel model;
    private final MainFrame view;
    private final PersistenceService persistence;

    public AppController(StreakModel model, MainFrame view, PersistenceService persistence) {
        this.model = model;
        this.view = view;
        this.persistence = persistence;
    }

    // -------------------------------------------------------------- init

    public void init() {
        model.ensureValidSelection();

        // Prime sidebar with persisted state (no animation on first paint).
        view.primeSidebarState(model.isSidebarExpanded());

        // Model → View
        model.addListener(this::onModelChanged);

        // View → Controller
        wireTopBar();
        wireSidebar();
        wireCounterPanel();
        wireMenuActions();
        wireHoldButton();

        // Initial paint
        onModelChanged();

        // Give the HoldButton focus so SPACE hold-to-increment works right away.
        view.getHoldButton().requestFocusInWindow();
    }

    private void wireTopBar() {
        view.getTopBar().setOnNewCounter(this::handleNewCounter);
        view.getTopBar().setOnThemeToggle(this::handleToggleTheme);
        view.getTopBar().setOnSidebarToggle(this::handleToggleSidebar);
    }

    private void wireSidebar() {
        view.getSidebar().setOnCounterSelected(id -> {
            model.selectCounter(id);
            saveState();
        });
        view.getSidebar().setOnNewCounter(this::handleNewCounter);
        view.getSidebar().setOnRename(this::handleRenameById);
        view.getSidebar().setOnDelete(this::handleDeleteById);
    }

    private void wireCounterPanel() {
        view.getCounterPanel().setOnReset(this::handleResetSelected);
        view.getCounterPanel().setOnRename(newName -> {
            Counter c = model.getSelectedCounter();
            if (c == null) return;
            model.renameCounter(c.getId(), newName);
            saveState();
        });
    }

    private void wireMenuActions() {
        view.setOnNewCounter(this::handleNewCounter);
        view.setOnResetSelected(this::handleResetSelected);
        view.setOnRenameSelected(this::handleRenameSelected);
        view.setOnDeleteSelected(this::handleDeleteSelected);
        view.setOnToggleTheme(this::handleToggleTheme);
        view.setOnToggleSidebar(this::handleToggleSidebar);
        view.setOnExport(this::handleExport);
        view.setOnImport(this::handleImport);
        view.setOnExit(this::handleExit);
    }

    private void wireHoldButton() {
        view.getHoldButton().addListener(new HoldListener() {
            @Override public void onHoldComplete() { handleHoldComplete(); }
            @Override public void onHoldStarted()  { /* view-only animation */ }
            @Override public void onHoldCancelled(double progress) { /* view-only */ }
        });
    }

    // ------------------------------------------------------- event handlers

    /**
     * On hold completion the model decides whether the increment is allowed.
     * The view's pulse/color already fired inside {@code HoldButton} — we just
     * decide whether to reflect a numeric change.
     */
    private void handleHoldComplete() {
        Counter selected = model.getSelectedCounter();
        if (selected == null) return;

        boolean success = model.attemptIncrementSelected();
        if (success) {
            view.getCounterPanel().bounceValueLabel();
            int v = selected.getCurrentValue();
            if (v == 7 || v == 30 || v == 100) {
                view.getCounterPanel().playMilestone(v);
            }
            saveState();
        }
        // If not success → "toy state": button already pulsed/changed color in
        // the view; the number is intentionally left untouched.
    }

    private void handleNewCounter() {
        String name = view.promptForName("New Counter");
        if (name == null || name.isBlank()) return;
        String startStr = view.promptForStartValue("0");
        if (startStr == null) return;
        int startVal;
        try {
            startVal = Integer.parseInt(startStr.trim());
        } catch (NumberFormatException ex) {
            view.showError("Start value must be an integer.");
            return;
        }
        model.addCounter(name.trim(), startVal);
        saveState();
    }

    private void handleRenameSelected() {
        Counter c = model.getSelectedCounter();
        if (c == null) return;
        handleRenameById(c.getId());
    }

    private void handleRenameById(String id) {
        Counter c = model.findCounterById(id);
        if (c == null) return;
        String newName = view.promptForName(c.getName());
        if (newName == null || newName.isBlank()) return;
        model.renameCounter(id, newName.trim());
        saveState();
    }

    private void handleDeleteSelected() {
        Counter c = model.getSelectedCounter();
        if (c == null) return;
        handleDeleteById(c.getId());
    }

    private void handleDeleteById(String id) {
        Counter c = model.findCounterById(id);
        if (c == null) return;
        if (!view.confirmDelete(c.getName())) return;
        model.deleteCounter(id);
        saveState();
    }

    private void handleResetSelected() {
        Counter c = model.getSelectedCounter();
        if (c == null) return;
        if (!view.confirmReset(c.getName())) return;
        model.resetCounter(c.getId());
        saveState();
    }

    private void handleToggleTheme() {
        model.setDarkTheme(!model.isDarkTheme());
        saveState();
    }

    private void handleToggleSidebar() {
        model.setSidebarExpanded(!model.isSidebarExpanded());
        saveState();
    }

    private void handleExport() {
        Path p = view.chooseExportFile();
        if (p == null) return;
        try {
            persistence.saveTo(model, p);
            view.showInfo("Exported to:\n" + p.toAbsolutePath());
        } catch (Exception ex) {
            view.showError("Export failed: " + ex.getMessage());
        }
    }

    private void handleImport() {
        Path p = view.chooseImportFile();
        if (p == null) return;
        try {
            StreakModel imported = persistence.loadFrom(p);
            imported.ensureValidSelection();
            // Swap the model content wholesale (keeping the same model instance
            // and listener list so any external references remain valid).
            copyStateFrom(imported);
            saveState();
            view.showInfo("Imported " + model.getCounters().size() + " counters.");
        } catch (Exception ex) {
            view.showError("Import failed: " + ex.getMessage());
        }
    }

    private void handleExit() {
        saveState();
        view.dispose();
        System.exit(0);
    }

    // -------------------------------------------------------------- helpers

    private void onModelChanged() {
        view.updateFromModel(model);
    }

    private void saveState() {
        try {
            persistence.save(model);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Copies the contents of the imported model into the current one so that
     * listeners, references, and any live view state stay attached to the same
     * instance.
     */
    private void copyStateFrom(StreakModel other) {
        model.getCounters().clear();
        model.getCounters().addAll(other.getCounters());
        model.selectCounter(other.getSelectedCounterId() != null
                ? other.getSelectedCounterId()
                : (model.getCounters().isEmpty() ? null : model.getCounters().get(0).getId()));
        model.setDarkTheme(other.isDarkTheme());
        model.setSidebarExpanded(other.isSidebarExpanded());
        model.ensureValidSelection();
        // Force a refresh
        model.setDarkTheme(model.isDarkTheme() == other.isDarkTheme()
                ? model.isDarkTheme() : other.isDarkTheme());
        // Note: the model's fireChanged runs on the setters above.
    }
}