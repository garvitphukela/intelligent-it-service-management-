package com.itsm.service;

import com.itsm.assignment.AssignmentContext;
import com.itsm.assignment.AssignmentStrategy;
import com.itsm.assignment.SmartRecommendationStrategy;
import com.itsm.assignment.TechnicianScore;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.exception.TechnicianUnavailableException;
import com.itsm.exception.WorkloadExceededException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/** Builds assignment contexts and runs the active (or a named) strategy. */
@Service
public class AssignmentService {

    private final UserRepository users;
    private final IncidentRepository incidents;
    private final StrategyRegistry registry;

    public AssignmentService(UserRepository users, IncidentRepository incidents, StrategyRegistry registry) {
        this.users = users;
        this.incidents = incidents;
        this.registry = registry;
    }

    public Map<Long, Integer> currentWorkloads() {
        Map<Long, Integer> map = new HashMap<>();
        for (Object[] row : incidents.countByTechnicianForStatuses(IncidentStatus.activeWorkStatuses())) {
            map.put((Long) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    public int workloadOf(Technician t) {
        return (int) incidents.countByAssignedTechnicianIdAndStatusIn(t.getId(), IncidentStatus.activeWorkStatuses());
    }

    public AssignmentContext context(Incident incident, boolean eligibleOnly) {
        Map<Long, Integer> load = currentWorkloads();
        List<Technician> candidates = users.findActiveTechnicians().stream()
                .filter(t -> !eligibleOnly || t.canTakeMoreWork(load.getOrDefault(t.getId(), 0)))
                .toList();
        Map<Long, Long> history = new HashMap<>();
        for (Object[] row : incidents.resolvedCountByTechnicianForService(incident.getService().getId())) {
            history.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return new AssignmentContext(incident, candidates, load, history);
    }

    /** Picks a technician using the active strategy. Empty if nobody is eligible. */
    public Optional<TechnicianScore> autoSelect(Incident incident) {
        AssignmentContext ctx = context(incident, true);
        if (ctx.candidates().isEmpty()) return Optional.empty();
        return registry.activeAssignment().select(ctx);
    }

    /** Ranked recommendations (Smart strategy) including currently busy technicians for transparency. */
    public List<TechnicianScore> recommend(Incident incident) {
        return registry.assignment(SmartRecommendationStrategy.KEY).rank(context(incident, true));
    }

    /** Validates a manual choice. */
    public void validateManualChoice(Technician t) {
        if (!t.isActive() || !t.isAvailable()) {
            throw new TechnicianUnavailableException(t.getFullName() + " (" + t.getCode() + ") is not available for new work");
        }
        int load = workloadOf(t);
        if (load >= t.getMaxWorkload()) {
            throw new WorkloadExceededException(t.getFullName() + " already has " + load + "/" + t.getMaxWorkload() + " active incidents");
        }
    }

    public AssignmentStrategy activeStrategy() { return registry.activeAssignment(); }
}
