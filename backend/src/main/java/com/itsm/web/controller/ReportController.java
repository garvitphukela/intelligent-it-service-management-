package com.itsm.web.controller;

import com.itsm.service.AuditQueryService;
import com.itsm.service.ReportService;
import com.itsm.web.dto.Views.AuditView;
import com.itsm.web.dto.Views.PageView;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reports;
    private final AuditQueryService audit;

    public ReportController(ReportService reports, AuditQueryService audit) {
        this.reports = reports;
        this.audit = audit;
    }

    @GetMapping("/reports/overview")
    public ReportService.Overview overview() { return reports.overview(); }

    @GetMapping("/reports/technician-workload")
    public List<ReportService.TechnicianLoad> workload() { return reports.technicianWorkload(); }

    @GetMapping("/reports/service-analysis")
    public List<ReportService.ServiceStat> services() { return reports.serviceAnalysis(); }

    @GetMapping("/reports/sla-compliance")
    public List<ReportService.SlaCompliance> sla() { return reports.slaCompliance(); }

    @GetMapping("/reports/trend")
    public List<ReportService.TrendPoint> trend(@RequestParam(defaultValue = "14") int days) { return reports.trend(days); }

    @GetMapping("/reports/breakdown")
    public Map<String, List<ReportService.Count>> breakdown() {
        return Map.of("category", reports.byCategory(), "status", reports.byStatus(), "priority", reports.byPriority());
    }

    @GetMapping("/audit")
    public PageView<AuditView> audit(@RequestParam(required = false) String action,
                                     @RequestParam(required = false) String actor,
                                     @RequestParam(required = false) String incident,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "50") int size) {
        return audit.search(action, actor, incident, page, size);
    }
}
