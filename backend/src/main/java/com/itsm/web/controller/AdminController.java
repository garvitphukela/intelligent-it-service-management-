package com.itsm.web.controller;

import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.domain.user.Role;
import com.itsm.service.*;
import com.itsm.web.dto.NotificationDto;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Administration endpoints. URL-level rule restricts to ADMIN/MANAGER; methods re-check (defence in depth). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class AdminController {

    private final UserAdminService users;
    private final CatalogService catalog;
    private final StrategyRegistry strategies;
    private final SlaService sla;
    private final NotificationQueryService notifications;
    private final SlaMonitoringService monitoring;

    public AdminController(UserAdminService users, CatalogService catalog, StrategyRegistry strategies, SlaService sla,
                           NotificationQueryService notifications, SlaMonitoringService monitoring) {
        this.users = users;
        this.catalog = catalog;
        this.strategies = strategies;
        this.sla = sla;
        this.notifications = notifications;
        this.monitoring = monitoring;
    }

    // users
    @GetMapping("/users")
    public List<UserView> users(@RequestParam(required = false) Role role) { return users.list(role); }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(@Valid @RequestBody Requests.CreateUser req) { return users.create(req); }

    @PutMapping("/users/{id}")
    public UserView updateUser(@PathVariable Long id, @Valid @RequestBody Requests.UpdateUser req) { return users.update(id, req); }

    // services
    @GetMapping("/services")
    public List<ServiceView> services() { return catalog.services(false); }

    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceView createService(@Valid @RequestBody Requests.ServiceReq req) { return catalog.createService(req); }

    @PutMapping("/services/{id}")
    public ServiceView updateService(@PathVariable Long id, @Valid @RequestBody Requests.ServiceReq req) { return catalog.updateService(id, req); }

    // categories
    @GetMapping("/categories")
    public List<CategoryView> categories() { return catalog.categories(false); }

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryView createCategory(@Valid @RequestBody Requests.CategoryReq req) { return catalog.createCategory(req); }

    @PutMapping("/categories/{id}")
    public CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody Requests.CategoryReq req) { return catalog.updateCategory(id, req); }

    // SLA policies
    @GetMapping("/sla-policies")
    public List<SlaPolicyView> policies() { return catalog.policies(); }

    @PostMapping("/sla-policies")
    @ResponseStatus(HttpStatus.CREATED)
    public SlaPolicyView createPolicy(@Valid @RequestBody Requests.SlaPolicyReq req) { return catalog.createPolicy(req); }

    @PutMapping("/sla-policies/{id}")
    public SlaPolicyView updatePolicy(@PathVariable Long id, @Valid @RequestBody Requests.SlaPolicyReq req) { return catalog.updatePolicy(id, req); }

    @DeleteMapping("/sla-policies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePolicy(@PathVariable Long id) { catalog.deletePolicy(id); }

    // strategies / settings
    @GetMapping("/settings")
    public SettingsView settings() {
        return new SettingsView(strategies.activeAssignment().key(), strategies.activePriority().key(), strategies.autoAssignEnabled(),
                strategies.assignmentStrategies().stream().map(s -> new StrategyView(s.key(), s.displayName(), s.description())).toList(),
                strategies.priorityStrategies().stream().map(s -> new StrategyView(s.key(), s.displayName(), s.description())).toList(),
                sla.timeScale());
    }

    @PutMapping("/settings")
    public SettingsView updateSettings(@RequestBody Requests.Settings req) {
        strategies.update(req.assignmentStrategy(), req.priorityStrategy(), req.autoAssign());
        return settings();
    }

    // notification log
    @GetMapping("/notifications")
    public PageView<NotificationDto> notificationLog(@RequestParam(required = false) NotificationChannelType channel,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "50") int size) {
        return notifications.log(channel, page, size);
    }

    /** Run an SLA monitoring cycle immediately (handy for demos). */
    @PostMapping("/sla/run-monitor")
    public SlaMonitoringService.CycleResult runMonitor() { return monitoring.runCycle(); }
}
