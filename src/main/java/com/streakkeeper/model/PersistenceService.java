// FILE: src/main/java/com/streakkeeper/model/PersistenceService.java
// PACKAGE: com.streakkeeper.model
package com.streakkeeper.model;

import java.nio.file.Path;

/**
 * Boundary between the model and storage. Kept as an interface so tests (or
 * future alternative stores) can swap it without touching business logic.
 */
public interface PersistenceService {

    /** Loads the model from the default data file, or a fresh model if none exists. */
    StreakModel load();

    /** Persists the model to the default data file. */
    void save(StreakModel model);

    /** Loads from an arbitrary path (used by "Import"). */
    StreakModel loadFrom(Path path);

    /** Writes to an arbitrary path (used by "Export"). */
    void saveTo(StreakModel model, Path path);
}