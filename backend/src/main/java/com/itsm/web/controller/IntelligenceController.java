package com.itsm.web.controller;

import com.itsm.service.IntelligenceService;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/intelligence")
public class IntelligenceController {

    private final IntelligenceService intelligence;

    public IntelligenceController(IntelligenceService intelligence) {
        this.intelligence = intelligence;
    }

    /** Live analysis while the reporter types: category, duplicates, known fixes, predicted priority + SLA. */
    @PostMapping("/analyze")
    public AnalysisView analyze(@RequestBody Requests.Analyze req) { return intelligence.analyze(req); }

    @GetMapping("/correlations")
    @PreAuthorize("hasAnyRole('TECHNICIAN','TEAM_LEAD','ADMIN','MANAGER')")
    public List<CorrelationView> correlations() { return intelligence.correlations(); }

    @GetMapping("/sla-risks")
    @PreAuthorize("hasAnyRole('TECHNICIAN','TEAM_LEAD','ADMIN','MANAGER')")
    public List<SlaRiskView> slaRisks() { return intelligence.slaRisks(); }
}
