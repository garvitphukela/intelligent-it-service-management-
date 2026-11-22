package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Views.UserView;
import com.itsm.web.dto.Views.WorkloadView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Transactional
public class TechnicianService {

    private final UserRepository users;
    private final IncidentRepository incidents;
    private final AssignmentService assignment;
    private final CurrentUserService currentUser;

    public TechnicianService(UserRepository users, IncidentRepository incidents, AssignmentService assignment, CurrentUserService currentUser) {
        this.users = users;
        this.incidents = incidents;
        this.assignment = assignment;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public WorkloadView myWorkload() {
        User u = currentUser.user();
        if (!(u instanceof Technician t)) throw new UnauthorizedOperationException("Only technicians have a workload");
        return workload(t);
    }

    @Transactional(readOnly = true)
    public WorkloadView workloadOf(Long technicianId) {
        User u = users.findById(technicianId).orElseThrow(() -> new ResourceNotFoundException("Technician not found"));
        if (!(u instanceof Technician t)) throw new ResourceNotFoundException(u.getFullName() + " is not a technician");
        return workload(t);
    }

    private WorkloadView workload(Technician t) {
        List<Incident> active = incidents.findByAssignedTechnicianIdAndStatusIn(t.getId(), IncidentStatus.activeWorkStatuses());
        List<Incident> done = incidents.findByAssignedTechnicianIdAndStatusIn(t.getId(),
                EnumSet.of(IncidentStatus.RESOLVED, IncidentStatus.VERIFIED, IncidentStatus.CLOSED));
        Double avgRes = done.stream().map(i -> i.getSla().resolutionTime()).filter(Objects::nonNull)
                .mapToLong(Duration::getSeconds).average().stream().map(s -> Math.round(s / 6.0) / 10.0).boxed().findFirst().orElse(null);
        Double avgRating = done.stream().filter(i -> i.getFeedback() != null && i.getFeedback().getRating() != null)
                .mapToInt(i -> i.getFeedback().getRating()).average().stream().map(r -> Math.round(r * 10) / 10.0).boxed().findFirst().orElse(null);
        return new WorkloadView(Mapper.user(t, active.size()), active.size(), t.getMaxWorkload(), done.size(), avgRes, avgRating,
                active.stream().sorted(Comparator.comparing((Incident i) -> i.getPriority()).thenComparing(Incident::getCreatedAt))
                        .map(Mapper::incident).toList());
    }

    public UserView setAvailability(boolean available) {
        User u = currentUser.user();
        if (!(u instanceof Technician t)) throw new UnauthorizedOperationException("Only technicians can change availability");
        t.setAvailable(available);
        return Mapper.user(t, assignment.workloadOf(t));
    }

    @Transactional(readOnly = true)
    public List<UserView> list() {
        Map<Long, Integer> load = assignment.currentWorkloads();
        return users.findActiveTechnicians().stream().map(t -> Mapper.user(t, load.getOrDefault(t.getId(), 0))).toList();
    }
}
