package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.NoteType;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.user.TeamLead;
import com.itsm.domain.user.Technician;
import com.itsm.event.Events;
import com.itsm.event.IncidentEventPublisher;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Automatic escalation of critical incidents that nobody has acknowledged:
 * <pre>
 *   100% of response window -> Team Lead (incident re-assigned to a team lead)
 *   150%                    -> System Administrator(s) notified
 *   200%                    -> IT Manager(s) notified
 * </pre>
 */
@Service
public class EscalationService {

    static final Set<Priority> ESCALATING = EnumSet.of(Priority.P1, Priority.P2);
    static final Set<IncidentStatus> UNACKNOWLEDGED = EnumSet.of(IncidentStatus.CLASSIFIED, IncidentStatus.ASSIGNED);

    private final UserRepository users;
    private final AssignmentService assignment;
    private final IncidentEventPublisher events;

    public EscalationService(UserRepository users, AssignmentService assignment, IncidentEventPublisher events) {
        this.users = users;
        this.assignment = assignment;
        this.events = events;
    }

    /** Target escalation level for an incident at {@code now} (pure, unit-testable). */
    public static int targetLevel(Incident i, Instant now) {
        if (!ESCALATING.contains(i.getPriority()) || !UNACKNOWLEDGED.contains(i.getStatus()) || !i.getSla().isStarted()) return 0;
        Duration window = Duration.between(i.getSla().getStartedAt(), i.getSla().getResponseDueAt());
        Duration elapsed = Duration.between(i.getSla().getStartedAt(), now);
        double ratio = (double) elapsed.toMillis() / Math.max(1, window.toMillis());
        if (ratio >= 2.0) return 3;
        if (ratio >= 1.5) return 2;
        if (ratio >= 1.0) return 1;
        return 0;
    }

    /** @return true if the incident was escalated */
    public boolean check(Incident i, Instant now) {
        int target = targetLevel(i, now);
        if (target <= i.getEscalationLevel()) return false;
        for (int level = i.getEscalationLevel() + 1; level <= target; level++) {
            escalateTo(i, level, now);
        }
        return true;
    }

    private void escalateTo(Incident i, int level, Instant now) {
        String target;
        if (level == 1) {
            Optional<TeamLead> lead = pickTeamLead(i);
            if (lead.isPresent()) {
                Technician previous = i.getAssignedTechnician();
                String prevStatus = i.getStatus().name();
                i.assignTo(lead.get(), now);
                events.publish(new Events.IncidentAssigned(i, null, now, previous, prevStatus, "Escalation", "not acknowledged within response SLA"));
                target = lead.get().getFullName() + " " + lead.get().getCode();
            } else {
                target = "no team lead available";
            }
        } else if (level == 2) {
            target = users.findActiveAdministrators().size() + " administrator(s)";
        } else {
            target = users.findActiveManagers().size() + " IT manager(s)";
        }
        i.escalate(level, now);
        i.addNote(null, "SYSTEM", "SYSTEM", NoteType.SYSTEM,
                "Escalated to " + Events.Escalated.levelName(level) + " (" + target + ") - not acknowledged in time", now);
        events.publish(new Events.Escalated(i, now, level, target));
    }

    private Optional<TeamLead> pickTeamLead(Incident i) {
        Map<Long, Integer> load = assignment.currentWorkloads();
        String skill = i.getCategory() == null ? null : i.getCategory().getRequiredSkill();
        Long current = i.getAssignedTechnician() == null ? null : i.getAssignedTechnician().getId();
        return users.findActiveTeamLeads().stream()
                .filter(l -> l.isAvailable() && !l.getId().equals(current))
                .min(Comparator.comparing((TeamLead l) -> l.hasSkill(skill) ? 0 : 1)
                        .thenComparing(l -> load.getOrDefault(l.getId(), 0)));
    }
}
