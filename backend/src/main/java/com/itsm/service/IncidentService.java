package com.itsm.service;

import com.itsm.assignment.TechnicianScore;
import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.*;
import com.itsm.domain.user.Technician;
import com.itsm.domain.user.User;
import com.itsm.event.Events;
import com.itsm.event.IncidentEventPublisher;
import com.itsm.exception.*;
import com.itsm.intelligence.AutoCategorizer;
import com.itsm.intelligence.DuplicateDetector;
import com.itsm.intelligence.TfIdfModel;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.repository.UserRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.*;

/**
 * Application service orchestrating the incident lifecycle:
 * Report -> Classify -> Prioritise -> Assign -> Investigate -> Resolve -> Verify -> Close.
 * Business rules live in the domain (Incident + states); this class coordinates
 * repositories, strategies, intelligence and event publication.
 */
@Service
@Transactional
public class IncidentService {

    private static final double SAME_REPORTER_DUPLICATE = 0.85;

    private final IncidentRepository incidents;
    private final CategoryRepository categories;
    private final ITServiceRepository services;
    private final UserRepository users;
    private final IncidentFactory factory;
    private final PriorityService priority;
    private final SlaService sla;
    private final AssignmentService assignment;
    private final AutoCategorizer categorizer;
    private final DuplicateDetector duplicates;
    private final IncidentAccessPolicy access;
    private final IncidentEventPublisher events;
    private final CurrentUserService currentUser;
    private final StrategyRegistry registry;
    private final Clock clock;

    public IncidentService(IncidentRepository incidents, CategoryRepository categories, ITServiceRepository services,
                           UserRepository users, IncidentFactory factory, PriorityService priority, SlaService sla,
                           AssignmentService assignment, AutoCategorizer categorizer, DuplicateDetector duplicates,
                           IncidentAccessPolicy access, IncidentEventPublisher events, CurrentUserService currentUser,
                           StrategyRegistry registry, Clock clock) {
        this.incidents = incidents;
        this.categories = categories;
        this.services = services;
        this.users = users;
        this.factory = factory;
        this.priority = priority;
        this.sla = sla;
        this.assignment = assignment;
        this.categorizer = categorizer;
        this.duplicates = duplicates;
        this.access = access;
        this.events = events;
        this.currentUser = currentUser;
        this.registry = registry;
        this.clock = clock;
    }

    private Instant now() { return Instant.now(clock); }

    // ============================== Create ==============================

    public CreateIncidentResponse create(Requests.CreateIncident req) {
        User reporter = currentUser.user();
        Instant now = now();
        Incident incident = factory.create(req, reporter, now);

        if (!Boolean.TRUE.equals(req.force())) rejectSameReporterDuplicate(reporter, incident);

        incidents.save(incident);
        events.publish(new Events.IncidentCreated(incident, reporter, now));

        classify(incident, req.categoryId(), reporter, now);

        List<DuplicateDetector.Match> similar = duplicates.findSimilar(incident.searchableText(), incident.getService(),
                incident.getCategory().getId(), incident.getId());
        similar.stream().filter(DuplicateDetector.Match::likelyDuplicate).findFirst().ifPresent(m ->
                incident.addNote(null, "SYSTEM", "SYSTEM", NoteType.SYSTEM,
                        "Possible duplicate of " + m.incident().getNumber() + " (" + Math.round(m.similarity() * 100)
                                + "% similar: " + String.join(", ", m.reasons()) + ")", now));

        if (registry.autoAssignEnabled()) tryAutoAssign(incident, now);

        incident.refreshSlaStatus(now);
        return new CreateIncidentResponse(detail(incident, reporter), similar.stream().map(Mapper::duplicate).toList());
    }

    private void rejectSameReporterDuplicate(User reporter, Incident candidate) {
        List<Incident> mine = incidents.findByStatusIn(IncidentStatus.openStatuses()).stream()
                .filter(i -> i.getReporter().getId().equals(reporter.getId())).toList();
        if (mine.isEmpty()) return;
        List<String> corpus = new ArrayList<>(mine.stream().map(Incident::searchableText).toList());
        corpus.add(candidate.searchableText());
        TfIdfModel model = new TfIdfModel(corpus);
        for (Incident i : mine) {
            double sim = model.similarity(candidate.searchableText(), i.searchableText());
            if (sim >= SAME_REPORTER_DUPLICATE && i.getService().getId().equals(candidate.getService().getId())) {
                throw new DuplicateIncidentException("You already reported this problem as " + i.getNumber()
                        + " (" + Math.round(sim * 100) + "% similar). Add a comment there, or submit again with force=true.");
            }
        }
    }

    private void classify(Incident incident, Long categoryId, User actor, Instant now) {
        Category category;
        boolean automatic;
        double confidence;
        if (categoryId != null) {
            category = categories.findById(categoryId).filter(Category::isActive)
                    .orElseThrow(() -> new InvalidInputException("Unknown or inactive category " + categoryId));
            automatic = false;
            confidence = 1.0;
        } else {
            var best = categorizer.best(incident.searchableText());
            if (best.isPresent()) {
                category = best.get().category();
                confidence = best.get().confidence();
            } else {
                category = categories.findByNameIgnoreCase("APPLICATION")
                        .or(() -> categories.findByActiveTrueOrderByNameAsc().stream().findFirst())
                        .orElseThrow(() -> new InvalidInputException("No incident categories configured"));
                confidence = 0;
            }
            automatic = true;
        }
        incident.applyCategory(category, automatic, confidence);
        priority.apply(incident);
        sla.apply(incident);
        incident.markClassified(now);
        events.publish(new Events.IncidentClassified(incident, automatic ? null : actor, now, automatic, confidence));
        events.publish(new Events.PriorityAssessed(incident, null, now, null));
    }

    private void tryAutoAssign(Incident incident, Instant now) {
        Optional<TechnicianScore> choice = assignment.autoSelect(incident);
        if (choice.isEmpty()) {
            incident.addNote(null, "SYSTEM", "SYSTEM", NoteType.SYSTEM,
                    "No technician currently available - incident queued for manual assignment", now);
            return;
        }
        String prev = incident.getStatus().name();
        incident.assignTo(choice.get().technician(), now);
        events.publish(new Events.IncidentAssigned(incident, null, now, null, prev,
                assignment.activeStrategy().displayName(), choice.get().reason()));
    }

    // ============================== Read ==============================

    @Transactional(readOnly = true)
    public IncidentDetail get(Long id) {
        User user = currentUser.user();
        Incident i = load(id);
        access.require(access.canView(user, i), "view " + i.getNumber());
        return detail(i, user);
    }

    @Transactional(readOnly = true)
    public List<TechnicianScoreView> recommendations(Long id) {
        User user = currentUser.user();
        access.require(user.getRole().isStaff(), "see technician recommendations");
        Incident i = load(id);
        return assignment.recommend(i).stream().map(s -> new TechnicianScoreView(Mapper.summary(s.technician()), s.score(),
                s.workload(), s.technician().getMaxWorkload(), s.breakdown(), s.reason())).toList();
    }

    // ============================== Update ==============================

    public IncidentDetail update(Long id, Requests.UpdateIncident req) {
        User user = currentUser.user();
        Incident i = load(id);
        access.require(access.canEdit(user, i), "edit " + i.getNumber());
        Instant now = now();
        List<String> changes = new ArrayList<>();
        ITService service = null;
        if (req.serviceId() != null && !req.serviceId().equals(i.getService().getId())) {
            service = services.findById(req.serviceId()).orElseThrow(() -> new ServiceNotFoundException("Service " + req.serviceId() + " not found"));
            changes.add("service -> " + service.getName());
        }
        if (req.impact() != null && req.impact() != i.getImpact()) changes.add("impact -> " + req.impact());
        if (req.urgency() != null && req.urgency() != i.getUrgency()) changes.add("urgency -> " + req.urgency());
        if (req.affectedUsers() != null && req.affectedUsers() != i.getAffectedUsers()) changes.add("affected users -> " + req.affectedUsers());
        if (req.title() != null && !req.title().equals(i.getTitle())) changes.add("title");
        if (req.description() != null && !req.description().equals(i.getDescription())) changes.add("description");
        i.updateDetails(req.title(), req.description(), req.impact(), req.urgency(), req.affectedUsers(), service);

        if (req.categoryId() != null && (i.getCategory() == null || !req.categoryId().equals(i.getCategory().getId()))) {
            Category c = categories.findById(req.categoryId()).orElseThrow(() -> new InvalidInputException("Unknown category"));
            i.applyCategory(c, false, 1.0);
            changes.add("category -> " + c.getName());
        }
        if (changes.isEmpty()) return detail(i, user);

        Priority before = i.getPriority();
        boolean changed = priority.apply(i);
        if (changed || service != null) sla.apply(i);
        i.refreshSlaStatus(now);
        events.publish(new Events.IncidentUpdated(i, user, now, String.join(", ", changes)));
        if (changed) events.publish(new Events.PriorityAssessed(i, user, now, before));
        return detail(i, user);
    }

    // ============================== Assignment ==============================

    public IncidentDetail assign(Long id, Long technicianId) {
        User user = currentUser.user();
        access.require(access.canAssign(user), "assign incidents");
        Incident i = load(id);
        Instant now = now();
        Technician previous = i.getAssignedTechnician();
        String prevStatus = i.getStatus().name();
        String strategyName;
        String reason;
        Technician target;
        if (technicianId == null) {
            TechnicianScore s = assignment.autoSelect(i).orElseThrow(() ->
                    new TechnicianUnavailableException("No technician is currently available (all busy, unavailable or at max workload)"));
            target = s.technician();
            strategyName = assignment.activeStrategy().displayName();
            reason = s.reason();
        } else {
            User u = users.findById(technicianId).orElseThrow(() -> new ResourceNotFoundException("Technician " + technicianId + " not found"));
            if (!(u instanceof Technician t)) throw new InvalidInputException(u.getFullName() + " is not a technician");
            if (previous != null && previous.getId().equals(t.getId())) {
                throw new ConflictException(i.getNumber() + " is already assigned to " + t.getFullName());
            }
            assignment.validateManualChoice(t);
            target = t;
            strategyName = null;
            reason = "manually assigned by " + user.getFullName();
        }
        i.assignTo(target, now);
        events.publish(new Events.IncidentAssigned(i, user, now, previous, prevStatus, strategyName, reason));
        i.refreshSlaStatus(now);
        return detail(i, user);
    }

    // ============================== Lifecycle ==============================

    public IncidentDetail acknowledge(Long id) {
        return transition(id, "acknowledged", null, (u, i, now) -> {
            access.require(access.canWork(u, i), "acknowledge " + i.getNumber());
            i.acknowledge(now);
        });
    }

    public IncidentDetail startWork(Long id) {
        return transition(id, "work_started", null, (u, i, now) -> {
            access.require(access.canWork(u, i), "work on " + i.getNumber());
            i.startWork(now);
        });
    }

    public IncidentDetail requestInfo(Long id, String message) {
        return transition(id, "info_requested", message, (u, i, now) -> {
            access.require(access.canWork(u, i), "request information on " + i.getNumber());
            i.requestInfo(now);
            i.addNote(u.getId(), u.getFullName(), u.getRole().name(), NoteType.INFO_REQUEST, message, now);
        });
    }

    public IncidentDetail provideInfo(Long id, String message) {
        return transition(id, "info_provided", message, (u, i, now) -> {
            access.require(access.canProvideInfo(u, i), "respond on " + i.getNumber());
            i.provideInfo(now);
            i.addNote(u.getId(), u.getFullName(), u.getRole().name(), NoteType.INFO_RESPONSE, message, now);
        });
    }

    public IncidentDetail resolve(Long id, Requests.Resolve req) {
        IncidentDetail result = transition(id, "resolved", null, (u, i, now) -> {
            access.require(access.canWork(u, i), "resolve " + i.getNumber());
            i.resolve(new Resolution(req.summary(), req.rootCause(), req.steps(), u.getFullName(), now), now);
        });
        resolveDuplicatesOf(load(id));
        return result;
    }

    public IncidentDetail verify(Long id) {
        return transition(id, "verified", null, (u, i, now) -> {
            access.require(access.canVerify(u, i), "verify " + i.getNumber());
            i.verify(now);
        });
    }

    public IncidentDetail reopen(Long id, String reason) {
        return transition(id, "reopened", reason, (u, i, now) -> {
            access.require(access.canVerify(u, i), "reopen " + i.getNumber());
            i.reopen(now);
            i.addNote(u.getId(), u.getFullName(), u.getRole().name(), NoteType.COMMENT, "Reopened: " + reason, now);
        });
    }

    public IncidentDetail close(Long id, String notes) {
        return transition(id, "closed", notes, (u, i, now) -> {
            access.require(access.canClose(u, i), "close " + i.getNumber());
            i.close(notes, u.getFullName(), now);
        });
    }

    public IncidentDetail cancel(Long id, String reason) {
        return transition(id, "cancelled", null, (u, i, now) -> {
            access.require(access.canCancel(u, i), "cancel " + i.getNumber());
            i.cancel(reason, now);
        });
    }

    @FunctionalInterface
    private interface TransitionAction {
        void apply(User user, Incident incident, Instant now);
    }

    /** Template for every lifecycle action: authorise+mutate, refresh SLA, publish event, map. */
    private IncidentDetail transition(Long id, String action, String comment, TransitionAction body) {
        User user = currentUser.user();
        Incident i = load(id);
        access.require(access.canView(user, i), "access " + i.getNumber());
        Instant now = now();
        IncidentStatus from = i.getStatus();
        body.apply(user, i, now);
        i.refreshSlaStatus(now);
        events.publish(new Events.StatusChanged(i, user, now, from, action, comment));
        return detail(i, user);
    }

    /** Duplicates follow their master: drive each through the state machine to RESOLVED. */
    private void resolveDuplicatesOf(Incident master) {
        Instant now = now();
        for (Incident child : incidents.findByMasterIncidentId(master.getId())) {
            if (!child.getStatus().isOpen()) continue;
            IncidentStatus from = child.getStatus();
            Technician tech = master.getAssignedTechnician();
            if (child.can(IncidentAction.ASSIGN) && child.getAssignedTechnician() == null && tech != null) child.assignTo(tech, now);
            if (child.can(IncidentAction.ACKNOWLEDGE)) child.acknowledge(now);
            if (child.can(IncidentAction.START_WORK)) child.startWork(now);
            if (child.can(IncidentAction.PROVIDE_INFO)) child.provideInfo(now);
            if (child.can(IncidentAction.RESOLVE)) {
                var r = master.getResolution();
                child.resolve(new Resolution(r.getSummary(), r.getRootCause(), "Resolved via master incident " + master.getNumber()
                        + (r.getSteps() == null ? "" : ". " + r.getSteps()), r.getResolvedBy(), now), now);
                child.refreshSlaStatus(now);
                events.publish(new Events.StatusChanged(child, null, now, from, "resolved", "resolved together with " + master.getNumber()));
            }
        }
    }

    // ============================== Collaboration ==============================

    public IncidentDetail addNote(Long id, Requests.Note req) {
        User user = currentUser.user();
        Incident i = load(id);
        access.require(access.canView(user, i), "comment on " + i.getNumber());
        if (i.getStatus().isTerminal()) throw new InvalidStateTransitionException(i.getStatus().name(), "comment on");
        boolean internal = Boolean.TRUE.equals(req.internal()) && access.canSeeInternalNotes(user);
        Instant now = now();
        IncidentNote note = i.addNote(user.getId(), user.getFullName(), user.getRole().name(),
                internal ? NoteType.WORK_NOTE : NoteType.COMMENT, req.text(), now);
        events.publish(new Events.NoteAdded(i, user, now, note));
        return detail(i, user);
    }

    public IncidentDetail feedback(Long id, Requests.FeedbackReq req) {
        User user = currentUser.user();
        Incident i = load(id);
        access.require(access.canGiveFeedback(user, i), "give feedback on " + i.getNumber());
        if (i.getFeedback() != null && i.getFeedback().getRating() != null) {
            throw new ConflictException("Feedback was already submitted for " + i.getNumber());
        }
        Instant now = now();
        i.submitFeedback(new Feedback(req.rating(), req.comment(), now));
        events.publish(new Events.FeedbackSubmitted(i, user, now));
        return detail(i, user);
    }

    public IncidentDetail linkDuplicate(Long id, Long masterId) {
        User user = currentUser.user();
        access.require(access.canLinkDuplicates(user), "link duplicate incidents");
        Incident i = load(id);
        Incident master = load(masterId);
        if (!master.getStatus().isOpen() && master.getStatus() != IncidentStatus.RESOLVED) {
            throw new ConflictException("Cannot link to " + master.getNumber() + " because it is " + master.getStatus());
        }
        if (!incidents.findByMasterIncidentId(i.getId()).isEmpty()) {
            throw new ConflictException(i.getNumber() + " already has duplicates linked to it; it cannot become a duplicate");
        }
        Instant now = now();
        i.linkAsDuplicateOf(master);
        i.addNote(user.getId(), user.getFullName(), user.getRole().name(), NoteType.SYSTEM,
                "Marked as duplicate of " + master.getNumber(), now);
        events.publish(new Events.DuplicateLinked(i, user, now));
        if (master.getStatus() == IncidentStatus.RESOLVED) resolveDuplicatesOf(master);
        return detail(i, user);
    }

    // ============================== Helpers ==============================

    Incident load(Long id) {
        return incidents.findById(id).orElseThrow(() -> new IncidentNotFoundException("Incident " + id + " not found"));
    }

    IncidentDetail detail(Incident i, User viewer) {
        return Mapper.detail(i, access.canSeeInternalNotes(viewer), access.allowedActions(viewer, i),
                i.getId() == null ? List.of() : incidents.findByMasterIncidentId(i.getId()), now(), sla.timeScale());
    }
}
