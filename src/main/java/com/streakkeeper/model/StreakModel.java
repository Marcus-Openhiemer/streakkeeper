// FILE: src/main/java/com/streakkeeper/model/StreakModel.java
// PACKAGE: com.streakkeeper.model
package com.streakkeeper.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Aggregated application state. Also acts as the event hub: all mutations go
 * through public methods here, each of which fires {@link ModelListener}s.
 * The controller only calls these methods; the view only reads and listens.
 *
 * <p>Gson serializes the <em>non-transient</em> fields to JSON. The listener
 * list is {@code transient} because listeners are runtime-only objects.</p>
 */
public class StreakModel {

    private List<Counter> counters = new ArrayList<>();
    private String selectedCounterId;
    private boolean darkTheme = false;
    private boolean sidebarExpanded = true;

    /** Not serialized by Gson — always reset by deserialization. */
    private transient List<ModelListener> listeners = new ArrayList<>();

    // ---------------------------------------------------------- observer API

    public void addListener(ModelListener l) {
        if (listeners == null) listeners = new ArrayList<>();
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    private void fireChanged() {
        if (listeners == null) return;
        // Copy to protect against re-entrant modification.
        for (ModelListener l : new ArrayList<>(listeners)) {
            l.onModelChanged();
        }
    }

    // ------------------------------------------------------------ selection

    public List<Counter> getCounters() {
        if (counters == null) counters = new ArrayList<>();
        return counters;
    }

    public Counter getSelectedCounter() {
        if (selectedCounterId == null) return null;
        for (Counter c : getCounters()) {
            if (c.getId().equals(selectedCounterId)) return c;
        }
        return null;
    }

    public String getSelectedCounterId() { return selectedCounterId; }

    /**
     * Changing the selected counter re-renders the whole main panel, so we
     * fire the listener. The equality guard prevents feedback loops when the
     * sidebar re-selects the already-selected item.
     */
    public void selectCounter(String id) {
        if (Objects.equals(id, selectedCounterId)) return;
        selectedCounterId = id;
        fireChanged();
    }

    // ------------------------------------------------------------------ CRUD

    public Counter addCounter(String name, int startValue) {
        Counter c = new Counter(name, startValue);
        getCounters().add(c);
        selectedCounterId = c.getId();
        fireChanged();
        return c;
    }

    public Counter findCounterById(String id) {
        if (id == null) return null;
        for (Counter c : getCounters()) {
            if (c.getId().equals(id)) return c;
        }
        return null;
    }

    public void renameCounter(String id, String newName) {
        Counter c = findCounterById(id);
        if (c == null) return;
        c.setName(newName);
        fireChanged();
    }

    public void deleteCounter(String id) {
        Counter c = findCounterById(id);
        if (c == null) return;
        getCounters().remove(c);
        if (Objects.equals(selectedCounterId, id)) {
            selectedCounterId = getCounters().isEmpty()
                    ? null
                    : getCounters().get(0).getId();
        }
        fireChanged();
    }

    public void resetCounter(String id) {
        Counter c = findCounterById(id);
        if (c == null) return;
        c.reset();
        fireChanged();
    }

    /**
     * Attempts the increment. Returns true on success, false when it's blocked
     * by the once-per-day rule (including clock-tampering guards). Importantly
     * we do <b>not</b> fire a model-change on failure — nothing in the model
     * changed, and the view already plays its "toy state" animation locally.
     */
    public boolean attemptIncrementSelected() {
        Counter c = getSelectedCounter();
        if (c == null) return false;
        if (!c.canIncrementToday()) return false;
        c.increment();
        fireChanged();
        return true;
    }

    // -------------------------------------------------------------- settings

    public boolean isDarkTheme() { return darkTheme; }

    public void setDarkTheme(boolean dark) {
        if (dark == darkTheme) return;
        darkTheme = dark;
        fireChanged();
    }

    public boolean isSidebarExpanded() { return sidebarExpanded; }

    public void setSidebarExpanded(boolean expanded) {
        if (expanded == sidebarExpanded) return;
        sidebarExpanded = expanded;
        fireChanged();
    }

    // --------------------------------------------------------------- helpers

    /**
     * Fixes up selection after load (e.g. previously selected counter was
     * deleted, or no counters exist yet). Called once by the controller before
     * the first paint.
     */
    public void ensureValidSelection() {
        if (getCounters().isEmpty()) {
            selectedCounterId = null;
            return;
        }
        if (selectedCounterId == null || findCounterById(selectedCounterId) == null) {
            selectedCounterId = getCounters().get(0).getId();
        }
    }

    /** Null-safe view of counters for iterating in tests. */
    public List<Counter> unmodifiableCounters() {
        return Collections.unmodifiableList(getCounters());
    }
}