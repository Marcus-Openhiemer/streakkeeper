// FILE: src/main/java/com/streakkeeper/view/HoldListener.java
// PACKAGE: com.streakkeeper.view
package com.streakkeeper.view;

/**
 * Callback surface for the {@link HoldButton}. Deliberately framework-free
 * (no Swing types) so the controller can implement it without dragging Swing
 * into the controller layer.
 */
public interface HoldListener {

    /** Hold reached 100% (5 s). Controller decides whether to actually increment. */
    void onHoldComplete();

    /** User has just pressed and started the hold. */
    void onHoldStarted();

    /** User released before the hold completed. Reports the progress at release (0..1). */
    void onHoldCancelled(double progress);
}