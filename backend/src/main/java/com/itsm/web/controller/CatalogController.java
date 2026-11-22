package com.itsm.web.controller;

import com.itsm.service.CatalogService;
import com.itsm.service.TechnicianService;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Read-only catalogue for every signed-in user (forms) plus technician directory. */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;
    private final TechnicianService technicians;

    public CatalogController(CatalogService catalog, TechnicianService technicians) {
        this.catalog = catalog;
        this.technicians = technicians;
    }

    @GetMapping("/catalog/services")
    public List<ServiceView> services() { return catalog.services(true); }

    @GetMapping("/catalog/categories")
    public List<CategoryView> categories() { return catalog.categories(true); }

    @GetMapping("/technicians")
    @PreAuthorize("hasAnyRole('TECHNICIAN','TEAM_LEAD','ADMIN','MANAGER')")
    public List<UserView> technicians() { return technicians.list(); }

    @GetMapping("/technicians/me/workload")
    @PreAuthorize("hasAnyRole('TECHNICIAN','TEAM_LEAD')")
    public WorkloadView myWorkload() { return technicians.myWorkload(); }

    @GetMapping("/technicians/{id}/workload")
    @PreAuthorize("hasAnyRole('TEAM_LEAD','ADMIN','MANAGER')")
    public WorkloadView workload(@PathVariable Long id) { return technicians.workloadOf(id); }

    @PostMapping("/technicians/me/availability")
    @PreAuthorize("hasAnyRole('TECHNICIAN','TEAM_LEAD')")
    public UserView availability(@jakarta.validation.Valid @RequestBody Requests.Availability req) { return technicians.setAvailability(req.available()); }
}
