// FILE: src/main/java/com/streakkeeper/model/Counter.java
// PACKAGE: com.streakkeeper.model
package com.streakkeeper.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity: a single counter the user is tracking.
 *
 * <p>Design choices &amp; rationale:</p>
 * <ul>
 *   <li>{@code lastIncrementedDate} is the canonical "day guard" for the
 *       once-per-calendar-day rule.</li>
 *   <li>{@code lastIncrementEpochMillis} is an <b>extra</b> monotonic-ish safety
 *       timestamp. {@link System#currentTimeMillis()} is wall-clock and not
 *       truly monotonic, but combined with the date check it defeats the common
 *       clock-rollback attack: if the user moves the clock back after an
 *       increment, the new "today" is <i>not after</i> the stored
 *       {@code lastIncrementedDate}, AND the new wall-clock is <i>less</i> than
 *       the stored epoch — both conditions cause us to refuse an increment.
 *       (A fully tamper-proof design would require a remote server; that is
 *       out of scope for a local desktop app, and this is documented in the
 *       brief as "extra safety".)</li>
 *   <li>{@code incrementHistory} is used for the heatmap and streak-length
 *       calculation.</li>
 * </ul>
 */
public class Counter {

    private String id;
    private String name;
    private int currentValue;
    private int startValue;
    private LocalDate lastIncrementedDate;
    private long lastIncrementEpochMillis;
    private List<LocalDate> incrementHistory;

    /** No-arg ctor required for Gson. */
    public Counter() {
        this.id = UUID.randomUUID().toString();
        this.incrementHistory = new ArrayList<>();
    }

    public Counter(String name, int startValue) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.startValue = startValue;
        this.currentValue = startValue;
        this.incrementHistory = new ArrayList<>();
    }

    // ---------------------------------------------------------------- business

    /**
     * Returns true if we are allowed to increment right now.
     *
     * Rules:
     *   1. If we already incremented today → refuse.
     *   2. If {@code lastIncrementedDate} is in the FUTURE (clock was moved
     *      forward then moved back) → refuse.
     *   3. If wall-clock now is BEFORE the last increment timestamp
     *      (clock was moved backward) → refuse.
     *
     * Otherwise it's a new day — allow.
     */
    public boolean canIncrementToday() {
        LocalDate today = LocalDate.now();
        if (lastIncrementedDate == null) {
            // Never incremented — always allowed.
            return true;
        }
        // Guard 1 & 2: today must be strictly AFTER lastIncrementedDate.
        if (!today.isAfter(lastIncrementedDate)) {
            return false;
        }
        // Guard 3: clock moved backward?
        if (System.currentTimeMillis() < lastIncrementEpochMillis) {
            return false;
        }
        return true;
    }

    /**
     * Performs the increment. Caller is expected to have checked
     * {@link #canIncrementToday()} first — we throw defensively to catch
     * programming errors rather than silently corrupting the streak.
     */
    public void increment() {
        if (!canIncrementToday()) {
            throw new IllegalStateException("Counter cannot be incremented today");
        }
        LocalDate today = LocalDate.now();
        currentValue++;
        lastIncrementedDate = today;
        lastIncrementEpochMillis = System.currentTimeMillis();
        if (incrementHistory == null) incrementHistory = new ArrayList<>();
        incrementHistory.add(today);
    }

    /** Resets value to the start value and clears all history. */
    public void reset() {
        this.currentValue = startValue;
        this.lastIncrementedDate = null;
        this.lastIncrementEpochMillis = 0L;
        if (incrementHistory == null) incrementHistory = new ArrayList<>();
        else incrementHistory.clear();
    }

    /**
     * Human-readable "last incremented" relative time.
     * Handles the (defensive) case where the stored date is in the future by
     * reporting "today" — that keeps the UI sane even if the clock was mangled.
     */
    public String getDaysAgo() {
        if (lastIncrementedDate == null) return "never";
        long days = ChronoUnit.DAYS.between(lastIncrementedDate, LocalDate.now());
        if (days <= 0) return "today";
        if (days == 1) return "1 day ago";
        return days + " days ago";
    }

    /**
     * Counts consecutive days ending at today or yesterday. Yesterday counts as
     * "still alive" — the user can extend the streak by incrementing today.
     * Returns 0 if the last increment is older than that.
     */
    public int getCurrentStreak() {
        List<LocalDate> history = getIncrementHistory();
        if (history.isEmpty()) return 0;

        // Sort unique dates descending.
        List<LocalDate> sorted = new ArrayList<>(history);
        sorted.sort((a, b) -> b.compareTo(a));
        List<LocalDate> unique = new ArrayList<>();
        for (LocalDate d : sorted) {
            if (unique.isEmpty() || !unique.get(unique.size() - 1).equals(d)) {
                unique.add(d);
            }
        }

        LocalDate today = LocalDate.now();
        LocalDate latest = unique.get(0);
        if (!latest.equals(today) && !latest.equals(today.minusDays(1))) {
            return 0;
        }
        int streak = 1;
        LocalDate cursor = latest;
        for (int i = 1; i < unique.size(); i++) {
            if (unique.get(i).equals(cursor.minusDays(1))) {
                streak++;
                cursor = unique.get(i);
            } else {
                break;
            }
        }
        return streak;
    }

    // ---------------------------------------------------------------- accessors

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCurrentValue() { return currentValue; }
    public void setCurrentValue(int currentValue) { this.currentValue = currentValue; }

    public int getStartValue() { return startValue; }
    public void setStartValue(int startValue) { this.startValue = startValue; }

    public LocalDate getLastIncrementedDate() { return lastIncrementedDate; }
    public void setLastIncrementedDate(LocalDate lastIncrementedDate) {
        this.lastIncrementedDate = lastIncrementedDate;
    }

    public long getLastIncrementEpochMillis() { return lastIncrementEpochMillis; }
    public void setLastIncrementEpochMillis(long v) { this.lastIncrementEpochMillis = v; }

    public List<LocalDate> getIncrementHistory() {
        if (incrementHistory == null) incrementHistory = new ArrayList<>();
        return incrementHistory;
    }
    public void setIncrementHistory(List<LocalDate> h) {
        this.incrementHistory = h == null ? new ArrayList<>() : h;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Counter counter = (Counter) o;
        return Objects.equals(id, counter.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}