// FILE: src/main/java/com/streakkeeper/model/ModelListener.java
// PACKAGE: com.streakkeeper.model
package com.streakkeeper.model;

/**
 * Observer notified whenever the model changes. Kept minimal on purpose:
 * the View doesn't need to know <i>what</i> changed — it just re-reads the
 * whole model. With tiny datasets (dozens of counters) this is simpler and
 * safer than fine-grained events.
 */
public interface ModelListener {
    void onModelChanged();
}