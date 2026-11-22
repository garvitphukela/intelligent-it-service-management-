package com.itsm.assignment;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.Technician;

import java.util.List;
import java.util.Map;

/**
 * Input to an assignment strategy.
 * @param candidates          technicians already filtered for eligibility (active, available, below max workload)
 * @param workload            technicianId -> number of active incidents
 * @param serviceExperience   technicianId -> number of incidents previously resolved on the same service
 */
public record AssignmentContext(Incident incident, List<Technician> candidates,
                                Map<Long, Integer> workload, Map<Long, Long> serviceExperience) {

    public int workloadOf(Technician t) { return workload.getOrDefault(t.getId(), 0); }

    public long serviceExperienceOf(Technician t) { return serviceExperience.getOrDefault(t.getId(), 0L); }

    public String requiredSkill() {
        return incident.getCategory() == null ? null : incident.getCategory().getRequiredSkill();
    }

    public double loadRatio(Technician t) {
        return (double) workloadOf(t) / Math.max(1, t.getMaxWorkload());
    }
}
