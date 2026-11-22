package com.itsm.priority;

import com.itsm.domain.incident.Impact;
import com.itsm.domain.incident.Urgency;

/** Everything a priority strategy may consider. Immutable value object. */
public record PriorityInput(Impact impact, Urgency urgency, int businessCriticality,
                            int affectedUsers, int serviceUserBase, String categoryName) {
    public PriorityInput {
        if (impact == null || urgency == null) throw new IllegalArgumentException("Impact and urgency are required");
        if (businessCriticality < 1 || businessCriticality > 5) throw new IllegalArgumentException("Criticality must be 1..5");
        affectedUsers = Math.max(1, affectedUsers);
        serviceUserBase = Math.max(1, serviceUserBase);
    }
}
