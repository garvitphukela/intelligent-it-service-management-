package com.itsm.web.dto;

import com.itsm.domain.audit.AuditEntry;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.incident.*;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.event.Events;
import com.itsm.intelligence.DuplicateDetector;
import com.itsm.web.dto.Views.*;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Entity -> view conversions. Must be called inside a transaction (lazy associations). */
public final class Mapper {

    private Mapper() {}

    public static UserSummary summary(User u) {
        if (u == null) return null;
        return new UserSummary(u.getId(), u.getCode(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getDisplayTitle());
    }

    public static UserView user(User u, Integer workload) {
        Technician t = u instanceof Technician tech ? tech : null;
        return new UserView(u.getId(), u.getCode(), u.getFullName(), u.getEmail(), u.getRole().name(), u.getDisplayTitle(),
                u.getPhone(), u.getDepartment(), u.isActive(), u.getCreatedAt(),
                t == null ? null : t.getSkills(), t == null ? null : t.getMaxWorkload(),
                t == null ? null : t.getExperienceYears(), t == null ? null : t.isAvailable(),
                t == null ? null : workload,
                u.getPreferredChannels().stream().map(Enum::name).collect(Collectors.toSet()));
    }

    public static ServiceView service(ITService s) {
        return new ServiceView(s.getId(), s.getName(), s.getDescription(), s.getBusinessCriticality(), s.getUserBase(),
                s.getOwnerTeam(), s.isActive(),
                s.getDependsOn().stream().sorted(Comparator.comparing(ITService::getName)).map(d -> new Ref(d.getId(), d.getName())).toList());
    }

    public static CategoryView category(Category c) {
        return new CategoryView(c.getId(), c.getName(), c.getDescription(), c.getRequiredSkill(), c.getKeywords(), c.isActive());
    }

    public static SlaPolicyView sla(SlaPolicy p) {
        return new SlaPolicyView(p.getId(), p.getName(), p.getService() == null ? null : p.getService().getId(),
                p.getService() == null ? "All services (default)" : p.getService().getName(), p.getPriority().name(),
                p.getResponseMinutes(), p.getResolutionMinutes(), p.getWarningThresholdPercent());
    }

    public static IncidentSummary incident(Incident i) {
        Technician t = i.getAssignedTechnician();
        return new IncidentSummary(i.getId(), i.getNumber(), i.getTitle(), i.getStatus().name(),
                i.getPriority() == null ? null : i.getPriority().name(),
                i.getCategory() == null ? null : i.getCategory().getName(),
                i.getService().getName(), i.getService().getId(),
                i.getReporter().getFullName(), i.getReporter().getCode(),
                t == null ? null : t.getFullName(), t == null ? null : t.getCode(), t == null ? null : t.getId(),
                i.getSlaStatus().name(), i.getSla().getResponseDueAt(), i.getSla().getResolutionDueAt(),
                i.getCreatedAt(), i.getUpdatedAt(), i.getEscalationLevel(), i.isMajorIncident(),
                i.getMasterIncident() == null ? null : i.getMasterIncident().getNumber());
    }

    public static IncidentDetail detail(Incident i, boolean includeInternalNotes, List<String> allowedActions,
                                        List<Incident> duplicates, Instant now, double timeScale) {
        SlaTracker s = i.getSla();
        SlaPolicy p = i.getSlaPolicy();
        SlaView slaView = new SlaView(p == null ? null : p.getName(), p == null ? null : p.getResponseMinutes(),
                p == null ? null : p.getResolutionMinutes(), s.getStartedAt(), s.getResponseDueAt(), s.getResolutionDueAt(),
                s.getRespondedAt(), s.getResolvedAt(), s.getPausedAt(), s.isResponseBreached(), s.isResolutionBreached(),
                s.statusAt(now, i.getStatus()).name(), Math.round(s.elapsedFraction(now) * 1000) / 10.0, timeScale);
        Resolution r = i.getResolution();
        Feedback f = i.getFeedback();
        List<NoteView> notes = i.getNotes().stream()
                .filter(n -> includeInternalNotes || !n.getType().isInternal())
                .map(n -> new NoteView(n.getId(), n.getType().name(), n.getText(), n.getAuthorName(), n.getAuthorRole(), n.getCreatedAt()))
                .toList();
        return new IncidentDetail(i.getId(), i.getNumber(), i.getTitle(), i.getDescription(), i.getStatus().name(),
                i.getPriority() == null ? null : i.getPriority().name(), i.getPriorityScore(), i.getPriorityRationale(),
                i.getImpact().name(), i.getUrgency().name(), i.getAffectedUsers(),
                i.getCategory() == null ? null : new Ref(i.getCategory().getId(), i.getCategory().getName()),
                i.isCategoryAutoAssigned(), i.getCategoryConfidence(),
                service(i.getService()), summary(i.getReporter()), summary(i.getAssignedTechnician()),
                slaView, i.getEscalationLevel(), Events.Escalated.levelName(i.getEscalationLevel()),
                r == null || r.getSummary() == null ? null : new ResolutionView(r.getSummary(), r.getRootCause(), r.getSteps(), r.getResolvedBy(), r.getResolvedAt()),
                f == null || f.getRating() == null ? null : new FeedbackView(f.getRating(), f.getComment(), f.getSubmittedAt()),
                i.getClosureNotes(), i.getClosedBy(), i.getClosedAt(), i.getCancellationReason(),
                i.getMasterIncident() == null ? null : new Ref(i.getMasterIncident().getId(), i.getMasterIncident().getNumber()),
                duplicates.stream().map(d -> new Ref(d.getId(), d.getNumber())).toList(),
                i.isMajorIncident(), notes, allowedActions, i.getCreatedAt(), i.getUpdatedAt(), i.getVersion());
    }

    public static DuplicateView duplicate(DuplicateDetector.Match m) {
        Incident i = m.incident();
        return new DuplicateView(i.getId(), i.getNumber(), i.getTitle(), i.getStatus().name(), i.getService().getName(),
                m.similarity(), m.likelyDuplicate(), m.reasons());
    }

    public static AuditView audit(AuditEntry a) {
        return new AuditView(a.getId(), a.getTimestamp(), a.getActorName(), a.getAction(), a.getIncidentId(),
                a.getIncidentNumber(), a.getPreviousState(), a.getNewState(), a.getDescription());
    }
}
