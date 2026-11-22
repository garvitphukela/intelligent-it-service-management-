package com.itsm.event;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentNote;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Concrete incident events. Grouped in one file as small nested classes. */
public final class Events {

    private Events() {}

    public static final class IncidentCreated extends IncidentEvent {
        public IncidentCreated(Incident i, User actor, Instant at) { super(i, actor, at); }
        @Override public String type() { return "INCIDENT_CREATED"; }
        @Override public String description() {
            return incident().getNumber() + " created by " + actorName() + " for " + incident().getService().getName() + ": " + incident().getTitle();
        }
        @Override public String newState() { return IncidentStatus.NEW.name(); }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.REPORTER); }
    }

    public static final class IncidentClassified extends IncidentEvent {
        private final boolean automatic;
        private final double confidence;
        public IncidentClassified(Incident i, User actor, Instant at, boolean automatic, double confidence) {
            super(i, actor, at);
            this.automatic = automatic;
            this.confidence = confidence;
        }
        @Override public String type() { return "INCIDENT_CLASSIFIED"; }
        @Override public String description() {
            return "Incident classified as " + incident().getCategory().getName()
                    + (automatic ? String.format(Locale.ROOT, " (auto-detected, %.0f%% confidence)", confidence * 100) : " (selected by reporter)");
        }
        @Override public String previousState() { return IncidentStatus.NEW.name(); }
    }

    public static final class PriorityAssessed extends IncidentEvent {
        private final Priority previous;
        public PriorityAssessed(Incident i, User actor, Instant at, Priority previous) {
            super(i, actor, at);
            this.previous = previous;
        }
        public boolean isChange() { return previous != null; }
        @Override public String type() { return isChange() ? "PRIORITY_CHANGED" : "PRIORITY_CALCULATED"; }
        @Override public String description() {
            String s = isChange() ? "Priority changed from " + previous + " to " + incident().getPriority()
                    : "Priority calculated as " + incident().getPriority() + " - " + incident().getPriority().label();
            return s + ". " + incident().getPriorityRationale();
        }
        @Override public Set<Audience> audience() {
            return isChange() ? EnumSet.of(Audience.REPORTER, Audience.ASSIGNEE) : Set.of();
        }
        @Override public boolean isUrgent() { return incident().getPriority() == Priority.P1; }
    }

    public static final class IncidentAssigned extends IncidentEvent {
        private final Technician previousTechnician;
        private final String strategy;
        private final String reason;
        private final String previousStatus;
        public IncidentAssigned(Incident i, User actor, Instant at, Technician previousTechnician, String previousStatus, String strategy, String reason) {
            super(i, actor, at);
            this.previousTechnician = previousTechnician;
            this.previousStatus = previousStatus;
            this.strategy = strategy;
            this.reason = reason;
        }
        public Technician previousTechnician() { return previousTechnician; }
        @Override public String type() { return previousTechnician == null ? "INCIDENT_ASSIGNED" : "INCIDENT_REASSIGNED"; }
        @Override public String description() {
            Technician t = incident().getAssignedTechnician();
            String s = (previousTechnician == null ? "Assigned to " : "Reassigned from " + previousTechnician.getCode() + " to ")
                    + t.getDisplayTitle() + " " + t.getCode() + " (" + t.getFullName() + ")";
            if (strategy != null) s += " using " + strategy + " strategy";
            if (reason != null) s += " - " + reason;
            return s;
        }
        @Override public String previousState() { return previousStatus; }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.ASSIGNEE, Audience.REPORTER, Audience.PREVIOUS_ASSIGNEE); }
        @Override public boolean isUrgent() { return incident().getPriority() == Priority.P1; }
    }

    public static final class StatusChanged extends IncidentEvent {
        private final IncidentStatus from;
        private final String action;
        private final String comment;
        public StatusChanged(Incident i, User actor, Instant at, IncidentStatus from, String action, String comment) {
            super(i, actor, at);
            this.from = from;
            this.action = action;
            this.comment = comment;
        }
        @Override public String type() { return "INCIDENT_" + action.toUpperCase(Locale.ROOT); }
        @Override public String description() {
            String s = switch (incident().getStatus()) {
                case ACKNOWLEDGED -> "Technician acknowledged incident";
                case IN_PROGRESS -> from == IncidentStatus.AWAITING_INFO ? "Reporter provided information; investigation resumed"
                        : from == IncidentStatus.RESOLVED ? "Incident reopened by " + actorName() : "Investigation started";
                case AWAITING_INFO -> "Additional information requested from reporter (SLA paused)";
                case RESOLVED -> "Incident marked RESOLVED: " + incident().getResolution().getSummary();
                case VERIFIED -> "Resolution verified by " + actorName();
                case CLOSED -> "Incident CLOSED by " + actorName();
                case CANCELLED -> "Incident CANCELLED: " + incident().getCancellationReason();
                default -> "Status changed to " + incident().getStatus();
            };
            return comment == null || comment.isBlank() ? s : s + " - \"" + comment + "\"";
        }
        @Override public String previousState() { return from.name(); }
        @Override public Set<Audience> audience() {
            return switch (incident().getStatus()) {
                case ACKNOWLEDGED, IN_PROGRESS -> from == IncidentStatus.AWAITING_INFO || from == IncidentStatus.RESOLVED
                        ? EnumSet.of(Audience.ASSIGNEE) : EnumSet.of(Audience.REPORTER);
                case AWAITING_INFO, RESOLVED, CLOSED -> EnumSet.of(Audience.REPORTER);
                case VERIFIED -> EnumSet.of(Audience.ASSIGNEE);
                case CANCELLED -> EnumSet.of(Audience.REPORTER, Audience.ASSIGNEE);
                default -> Set.of();
            };
        }
    }

    public static final class NoteAdded extends IncidentEvent {
        private final IncidentNote note;
        public NoteAdded(Incident i, User actor, Instant at, IncidentNote note) {
            super(i, actor, at);
            this.note = note;
        }
        @Override public String type() { return "NOTE_ADDED"; }
        @Override public String description() {
            String kind = note.getType().isInternal() ? "Investigation note" : "Comment";
            String text = note.getText().length() > 140 ? note.getText().substring(0, 140) + "..." : note.getText();
            return kind + " added by " + actorName() + ": " + text;
        }
        @Override public Set<Audience> audience() {
            if (note.getType().isInternal()) return Set.of();
            return actor() != null && actor().getRole().isStaff() ? EnumSet.of(Audience.REPORTER) : EnumSet.of(Audience.ASSIGNEE);
        }
    }

    public static final class SlaWarning extends IncidentEvent {
        private final String reason;
        public SlaWarning(Incident i, Instant at, String reason) {
            super(i, null, at);
            this.reason = reason;
        }
        @Override public String type() { return "SLA_APPROACHING"; }
        @Override public String description() { return "SLA deadline approaching for " + incident().getPriority() + " incident: " + reason; }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.ASSIGNEE, Audience.TEAM_LEADS); }
        @Override public boolean isUrgent() { return incident().getPriority() == Priority.P1; }
    }

    public static final class SlaBreached extends IncidentEvent {
        public SlaBreached(Incident i, Instant at) { super(i, null, at); }
        @Override public String type() { return "SLA_VIOLATED"; }
        @Override public String description() {
            var sla = incident().getSla();
            String which = sla.isResponseBreached() && sla.isResolutionBreached() ? "response and resolution"
                    : sla.isResponseBreached() ? "response" : "resolution";
            return "SLA " + which + " target violated (" + incident().getSlaPolicy().getName() + ")";
        }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.ASSIGNEE, Audience.TEAM_LEADS, Audience.ADMINS); }
        @Override public boolean isUrgent() { return true; }
    }

    public static final class Escalated extends IncidentEvent {
        private final int level;
        private final String target;
        public Escalated(Incident i, Instant at, int level, String target) {
            super(i, null, at);
            this.level = level;
            this.target = target;
        }
        public static String levelName(int level) {
            return switch (level) {
                case 0 -> "Technician";
                case 1 -> "Team Lead";
                case 2 -> "System Administrator";
                default -> "IT Manager";
            };
        }
        @Override public String type() { return "INCIDENT_ESCALATED"; }
        @Override public String description() {
            return "Not acknowledged within SLA - escalated to " + levelName(level) + " (" + target + ")";
        }
        @Override public Set<Audience> audience() {
            return switch (level) {
                case 1 -> EnumSet.of(Audience.ASSIGNEE, Audience.PREVIOUS_ASSIGNEE, Audience.TEAM_LEADS);
                case 2 -> EnumSet.of(Audience.ASSIGNEE, Audience.ADMINS);
                default -> EnumSet.of(Audience.ASSIGNEE, Audience.ADMINS, Audience.MANAGERS);
            };
        }
        @Override public boolean isUrgent() { return true; }
    }

    public static final class DuplicateLinked extends IncidentEvent {
        public DuplicateLinked(Incident i, User actor, Instant at) { super(i, actor, at); }
        @Override public String type() { return "DUPLICATE_LINKED"; }
        @Override public String description() {
            return incident().getNumber() + " linked as duplicate of " + incident().getMasterIncident().getNumber()
                    + "; it will be resolved together with the master incident";
        }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.REPORTER); }
    }

    public static final class FeedbackSubmitted extends IncidentEvent {
        public FeedbackSubmitted(Incident i, User actor, Instant at) { super(i, actor, at); }
        @Override public String type() { return "FEEDBACK_SUBMITTED"; }
        @Override public String description() {
            return "Reporter rated the resolution " + incident().getFeedback().getRating() + "/5"
                    + (incident().getFeedback().getComment() == null ? "" : ": " + incident().getFeedback().getComment());
        }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.ASSIGNEE); }
    }

    public static final class IncidentUpdated extends IncidentEvent {
        private final String changes;
        public IncidentUpdated(Incident i, User actor, Instant at, String changes) {
            super(i, actor, at);
            this.changes = changes;
        }
        @Override public String type() { return "INCIDENT_UPDATED"; }
        @Override public String description() { return "Incident details updated by " + actorName() + ": " + changes; }
    }

    public static final class MajorIncidentDetected extends IncidentEvent {
        private final List<String> relatedNumbers;
        private final String rootService;
        public MajorIncidentDetected(Incident representative, Instant at, List<String> relatedNumbers, String rootService) {
            super(representative, null, at);
            this.relatedNumbers = relatedNumbers;
            this.rootService = rootService;
        }
        @Override public String type() { return "MAJOR_INCIDENT_DETECTED"; }
        @Override public String title() { return "Major incident suspected on " + rootService; }
        @Override public String description() {
            return relatedNumbers.size() + " correlated incidents (" + String.join(", ", relatedNumbers)
                    + ") point to a common cause: " + rootService;
        }
        @Override public Set<Audience> audience() { return EnumSet.of(Audience.TEAM_LEADS, Audience.ADMINS, Audience.MANAGERS); }
        @Override public boolean isUrgent() { return true; }
    }
}
