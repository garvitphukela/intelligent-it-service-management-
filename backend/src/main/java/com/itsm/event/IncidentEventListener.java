package com.itsm.event;

/** Observer contract. Implementations are discovered automatically by the publisher. */
public interface IncidentEventListener {

    void onEvent(IncidentEvent event);

    /** Lets a listener ignore events it does not care about. */
    default boolean supports(IncidentEvent event) { return true; }

    /** Lower runs first (audit before notifications before broadcast). */
    default int order() { return 100; }
}
