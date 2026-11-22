package com.itsm.web.controller;

import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.incident.Priority;
import com.itsm.domain.incident.SlaStatus;
import com.itsm.service.*;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {

    private final IncidentService incidents;
    private final IncidentQueryService query;
    private final AuditQueryService audit;
    private final IntelligenceService intelligence;

    public IncidentController(IncidentService incidents, IncidentQueryService query, AuditQueryService audit, IntelligenceService intelligence) {
        this.incidents = incidents;
        this.query = query;
        this.audit = audit;
        this.intelligence = intelligence;
    }

    @GetMapping
    public PageView<IncidentSummary> search(@RequestParam(required = false) String number,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(required = false) List<IncidentStatus> status,
                                            @RequestParam(required = false) List<Priority> priority,
                                            @RequestParam(required = false) Long categoryId,
                                            @RequestParam(required = false) Long serviceId,
                                            @RequestParam(required = false) Long technicianId,
                                            @RequestParam(required = false) Long reporterId,
                                            @RequestParam(required = false) SlaStatus slaStatus,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                            @RequestParam(required = false) Boolean openOnly,
                                            @RequestParam(required = false) Boolean unassigned,
                                            @RequestParam(required = false) Boolean majorOnly,
                                            @RequestParam(required = false) Boolean mine,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size,
                                            @RequestParam(defaultValue = "createdAt") String sort,
                                            @RequestParam(defaultValue = "desc") String dir) {
        return query.search(new IncidentSearchCriteria(number, q, status, priority, categoryId, serviceId, technicianId,
                reporterId, slaStatus, from, to, openOnly, unassigned, majorOnly, mine), page, size, sort, dir);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateIncidentResponse create(@Valid @RequestBody Requests.CreateIncident req) { return incidents.create(req); }

    @GetMapping("/{id}")
    public IncidentDetail get(@PathVariable Long id) { return incidents.get(id); }

    @PatchMapping("/{id}")
    public IncidentDetail update(@PathVariable Long id, @Valid @RequestBody Requests.UpdateIncident req) { return incidents.update(id, req); }

    @PostMapping("/{id}/assign")
    public IncidentDetail assign(@PathVariable Long id, @RequestBody(required = false) Requests.Assign req) {
        return incidents.assign(id, req == null ? null : req.technicianId());
    }

    @PostMapping("/{id}/acknowledge")
    public IncidentDetail acknowledge(@PathVariable Long id) { return incidents.acknowledge(id); }

    @PostMapping("/{id}/start")
    public IncidentDetail start(@PathVariable Long id) { return incidents.startWork(id); }

    @PostMapping("/{id}/request-info")
    public IncidentDetail requestInfo(@PathVariable Long id, @Valid @RequestBody Requests.Message req) { return incidents.requestInfo(id, req.message()); }

    @PostMapping("/{id}/provide-info")
    public IncidentDetail provideInfo(@PathVariable Long id, @Valid @RequestBody Requests.Message req) { return incidents.provideInfo(id, req.message()); }

    @PostMapping("/{id}/resolve")
    public IncidentDetail resolve(@PathVariable Long id, @Valid @RequestBody Requests.Resolve req) { return incidents.resolve(id, req); }

    @PostMapping("/{id}/verify")
    public IncidentDetail verify(@PathVariable Long id) { return incidents.verify(id); }

    @PostMapping("/{id}/reopen")
    public IncidentDetail reopen(@PathVariable Long id, @Valid @RequestBody Requests.Message req) { return incidents.reopen(id, req.message()); }

    @PostMapping("/{id}/close")
    public IncidentDetail close(@PathVariable Long id, @Valid @RequestBody(required = false) Requests.OptionalMessage req) {
        return incidents.close(id, req == null ? null : req.message());
    }

    @PostMapping("/{id}/cancel")
    public IncidentDetail cancel(@PathVariable Long id, @Valid @RequestBody Requests.Message req) { return incidents.cancel(id, req.message()); }

    @PostMapping("/{id}/notes")
    public IncidentDetail note(@PathVariable Long id, @Valid @RequestBody Requests.Note req) { return incidents.addNote(id, req); }

    @PostMapping("/{id}/feedback")
    public IncidentDetail feedback(@PathVariable Long id, @Valid @RequestBody Requests.FeedbackReq req) { return incidents.feedback(id, req); }

    @PostMapping("/{id}/link-duplicate")
    public IncidentDetail linkDuplicate(@PathVariable Long id, @Valid @RequestBody Requests.LinkDuplicate req) {
        return incidents.linkDuplicate(id, req.masterId());
    }

    @GetMapping("/{id}/audit")
    public List<AuditView> history(@PathVariable Long id) { return audit.forIncident(id); }

    @GetMapping("/{id}/recommendations")
    public List<TechnicianScoreView> recommendations(@PathVariable Long id) { return incidents.recommendations(id); }

    @GetMapping("/{id}/suggested-resolutions")
    public List<ResolutionSuggestionView> suggestions(@PathVariable Long id) {
        incidents.get(id); // access check
        return intelligence.suggestedResolutions(id);
    }

    @GetMapping("/{id}/similar")
    public List<DuplicateView> similar(@PathVariable Long id) {
        incidents.get(id);
        return intelligence.similarTo(id);
    }
}
