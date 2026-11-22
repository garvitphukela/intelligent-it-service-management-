package com.itsm.assignment;

import java.util.List;
import java.util.Optional;

/**
 * Strategy pattern: pluggable technician assignment algorithms. New strategies are added
 * by writing one class annotated with {@code @Component}; the registry discovers them
 * automatically and administrators can switch at runtime.
 */
public interface AssignmentStrategy {

    String key();

    String displayName();

    String description();

    /** Rank eligible candidates, best first. Never returns null. */
    List<TechnicianScore> rank(AssignmentContext context);

    default Optional<TechnicianScore> select(AssignmentContext context) {
        return rank(context).stream().findFirst();
    }
}
