package com.itsm.domain.incident;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Aggregate root of the incident domain.
 * <ul>
 *   <li><b>Encapsulation</b> - no public setters; every change goes through a business method.</li>
 *   <li><b>State pattern</b> - lifecycle actions are delegated to the current {@link IncidentState};
 *       illegal actions throw {@code InvalidStateTransitionException}.</li>
 *   <li><b>Composition</b> - notes, SLA tracker, resolution and feedback belong to the incident.</li>
 *   <li><b>Association</b> - reporter, technician, service, category, SLA policy are independent entities.</li>
 * </ul>
 */
@Entity
@Table(name = "incidents", indexes = {
        @Index(columnList = "status"), @Index(columnList = "priority"), @Index(columnList = "createdAt")})
public class Incident {

    public static final int NUMBER_OFFSET = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private long version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    private boolean categoryAutoAssigned;

    private double categoryConfidence;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private ITService service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Impact impact;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Urgency urgency;

    private int affectedUsers = 1;

    @Enumerated(EnumType.STRING)
    @Column(length = 5)
    private Priority priority;

    private double priorityScore;

    @Column(length = 500)
    private String priorityRationale;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IncidentStatus status = IncidentStatus.NEW;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    private Technician assignedTechnician;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sla_policy_id")
    private SlaPolicy slaPolicy;

    @Embedded
    private SlaTracker sla = new SlaTracker();

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SlaStatus slaStatus = SlaStatus.NOT_APPLICABLE;

    /** 0 = technician, 1 = team lead, 2 = administrator, 3 = IT manager. */
    private int escalationLevel;

    private Instant lastEscalatedAt;

    @Embedded
    private Resolution resolution;

    @Embedded
    private Feedback feedback;

    @Column(length = 1000)
    private String closureNotes;

    @Column(length = 140)
    private String closedBy;

    private Instant closedAt;

    @Column(length = 500)
    private String cancellationReason;

    /** Set when this incident has been identified as a duplicate of another (master) incident. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "master_incident_id")
    private Incident masterIncident;

    private boolean majorIncident;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<IncidentNote> notes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @Transient
    private IncidentState state;

    protected Incident() {}

    /** Use {@code IncidentFactory} rather than calling this directly. */
    public Incident(String title, String description, User reporter, ITService service,
                    Impact impact, Urgency urgency, int affectedUsers, Instant createdAt) {
        if (title == null || title.isBlank()) throw new IllegalArgumentException("Title is required");
        if (description == null || description.isBlank()) throw new IllegalArgumentException("Description is required");
        if (reporter == null) throw new IllegalArgumentException("Reporter is required");
        if (service == null) throw new IllegalArgumentException("Affected service is required");
        this.title = title.trim();
        this.description = description.trim();
        this.reporter = reporter;
        this.service = service;
        this.impact = impact == null ? Impact.LOW : impact;
        this.urgency = urgency == null ? Urgency.LOW : urgency;
        this.affectedUsers = Math.max(1, affectedUsers);
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // ===================== State pattern delegation =====================

    private IncidentState state() {
        if (state == null || state.status() != status) {
            state = IncidentStateFactory.forStatus(status);
        }
        return state;
    }

    /** Package-private: only {@link IncidentState} implementations may move the incident. */
    void moveTo(IncidentStatus next, Instant at) {
        this.status = next;
        this.state = IncidentStateFactory.forStatus(next);
        this.updatedAt = at;
    }

    public Set<IncidentStatus> allowedNextStatuses() { return state().allowedTransitions(); }

    public boolean can(IncidentAction action) { return state().supports(action); }

    public void markClassified(Instant at) { state().classify(this, at); }

    public void assignTo(Technician technician, Instant at) {
        if (technician == null) throw new IllegalArgumentException("Technician is required");
        state().assign(this, technician, at);
    }

    public void acknowledge(Instant at) { state().acknowledge(this, at); }

    public void startWork(Instant at) { state().startWork(this, at); }

    public void requestInfo(Instant at) { state().requestInfo(this, at); }

    public void provideInfo(Instant at) { state().provideInfo(this, at); }

    public void resolve(Resolution resolution, Instant at) { state().resolve(this, resolution, at); }

    public void verify(Instant at) { state().verify(this, at); }

    public void reopen(Instant at) { state().reopen(this, at); }

    public void close(String notes, String closedBy, Instant at) { state().close(this, notes, closedBy, at); }

    public void cancel(String reason, Instant at) { state().cancel(this, reason, at); }

    // ---- package-private hooks used by the state objects ----

    void recordAssignment(Technician technician) { this.assignedTechnician = technician; }

    void recordResolution(Resolution resolution, Instant at) {
        this.resolution = resolution;
        this.sla.markResolved(at);
    }

    void clearResolution() {
        this.resolution = null;
        this.sla.clearResolution();
    }

    void recordClosure(String notes, String by, Instant at) {
        this.closureNotes = notes;
        this.closedBy = by;
        this.closedAt = at;
    }

    void recordCancellation(String reason) { this.cancellationReason = reason; }

    // ===================== Classification / priority / SLA =====================

    public void applyCategory(Category category, boolean automatic, double confidence) {
        if (status.isTerminal()) throw new IllegalStateException("Cannot re-categorise a closed incident");
        this.category = category;
        this.categoryAutoAssigned = automatic;
        this.categoryConfidence = confidence;
        touch();
    }

    /** @return true if the priority actually changed */
    public boolean applyPriority(Priority priority, double score, String rationale) {
        boolean changed = this.priority != priority;
        this.priority = priority;
        this.priorityScore = score;
        this.priorityRationale = rationale;
        touch();
        return changed;
    }

    public void applySla(SlaPolicy policy, java.time.Duration responseWindow, java.time.Duration resolutionWindow) {
        this.slaPolicy = policy;
        this.sla.start(createdAt, responseWindow, resolutionWindow, policy.getWarningThresholdPercent());
    }

    public void refreshSlaStatus(Instant now) {
        this.slaStatus = sla.statusAt(now, status);
    }

    public void forceSlaStatus(SlaStatus s) { this.slaStatus = s; }

    public void updateDetails(String title, String description, Impact impact, Urgency urgency, Integer affectedUsers, ITService service) {
        if (status.isTerminal()) throw new IllegalStateException("Closed incidents cannot be edited");
        if (title != null && !title.isBlank()) this.title = title.trim();
        if (description != null && !description.isBlank()) this.description = description.trim();
        if (impact != null) this.impact = impact;
        if (urgency != null) this.urgency = urgency;
        if (affectedUsers != null) this.affectedUsers = Math.max(1, affectedUsers);
        if (service != null) this.service = service;
        touch();
    }

    // ===================== Collaboration =====================

    public IncidentNote addNote(Long authorId, String authorName, String authorRole, NoteType type, String text, Instant at) {
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Note text is required");
        IncidentNote note = new IncidentNote(this, authorId, authorName, authorRole, type, text.trim(), at);
        notes.add(note);
        this.updatedAt = at;
        return note;
    }

    public void submitFeedback(Feedback feedback) {
        if (status != IncidentStatus.RESOLVED && status != IncidentStatus.VERIFIED && status != IncidentStatus.CLOSED) {
            throw new IllegalStateException("Feedback can only be given after resolution");
        }
        this.feedback = feedback;
    }

    public void linkAsDuplicateOf(Incident master) {
        if (master == null || master == this || (master.id != null && master.id.equals(this.id))) {
            throw new IllegalArgumentException("An incident cannot be a duplicate of itself");
        }
        if (master.masterIncident != null) {
            throw new IllegalArgumentException(master.getNumber() + " is itself a duplicate; link to its master instead");
        }
        this.masterIncident = master;
        touch();
    }

    public void markMajorIncident() { this.majorIncident = true; }

    public void escalate(int level, Instant at) {
        this.escalationLevel = level;
        this.lastEscalatedAt = at;
        this.updatedAt = at;
    }

    private void touch() { this.updatedAt = Instant.now(); }

    // ===================== Accessors =====================

    public Long getId() { return id; }
    public String getNumber() { return id == null ? "INC-NEW" : "INC" + (NUMBER_OFFSET + id); }
    public long getVersion() { return version; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public User getReporter() { return reporter; }
    public Category getCategory() { return category; }
    public boolean isCategoryAutoAssigned() { return categoryAutoAssigned; }
    public double getCategoryConfidence() { return categoryConfidence; }
    public ITService getService() { return service; }
    public Impact getImpact() { return impact; }
    public Urgency getUrgency() { return urgency; }
    public int getAffectedUsers() { return affectedUsers; }
    public Priority getPriority() { return priority; }
    public double getPriorityScore() { return priorityScore; }
    public String getPriorityRationale() { return priorityRationale; }
    public IncidentStatus getStatus() { return status; }
    public Technician getAssignedTechnician() { return assignedTechnician; }
    public SlaPolicy getSlaPolicy() { return slaPolicy; }
    public SlaTracker getSla() { return sla; }
    public SlaStatus getSlaStatus() { return slaStatus; }
    public int getEscalationLevel() { return escalationLevel; }
    public Instant getLastEscalatedAt() { return lastEscalatedAt; }
    public Resolution getResolution() { return resolution; }
    public Feedback getFeedback() { return feedback; }
    public String getClosureNotes() { return closureNotes; }
    public String getClosedBy() { return closedBy; }
    public Instant getClosedAt() { return closedAt; }
    public String getCancellationReason() { return cancellationReason; }
    public Incident getMasterIncident() { return masterIncident; }
    public boolean isMajorIncident() { return majorIncident; }
    public List<IncidentNote> getNotes() { return Collections.unmodifiableList(notes); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Text used by the similarity / categorisation engines. */
    public String searchableText() { return title + " " + description; }

    public static Long idFromNumber(String number) {
        if (number == null) return null;
        String digits = number.trim().toUpperCase().replace("INC", "").replace("-", "");
        if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit)) return null;
        long n = Long.parseLong(digits);
        return n > NUMBER_OFFSET ? n - NUMBER_OFFSET : n;
    }
}
