package com.itsm.priority;

/**
 * Strategy pattern: interchangeable algorithms for computing incident priority.
 * The active implementation is chosen at runtime by the administrator.
 */
public interface PriorityStrategy {

    /** Stable identifier persisted in settings. */
    String key();

    String displayName();

    String description();

    PriorityResult calculate(PriorityInput input);
}
