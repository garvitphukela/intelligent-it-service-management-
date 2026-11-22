package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.priority.PriorityInput;
import com.itsm.priority.PriorityResult;
import org.springframework.stereotype.Service;

/** Applies the administrator-selected priority strategy. */
@Service
public class PriorityService {

    private final StrategyRegistry registry;

    public PriorityService(StrategyRegistry registry) {
        this.registry = registry;
    }

    public PriorityResult calculate(PriorityInput input) {
        return registry.activePriority().calculate(input);
    }

    public PriorityResult calculate(Incident i) {
        return calculate(new PriorityInput(i.getImpact(), i.getUrgency(), i.getService().getBusinessCriticality(),
                i.getAffectedUsers(), i.getService().getUserBase(), i.getCategory() == null ? null : i.getCategory().getName()));
    }

    /** @return true if the priority changed */
    public boolean apply(Incident i) {
        PriorityResult r = calculate(i);
        return i.applyPriority(r.priority(), r.score(), r.rationale() + " [" + registry.activePriority().displayName() + "]");
    }

    public String activeStrategyName() { return registry.activePriority().displayName(); }
}
