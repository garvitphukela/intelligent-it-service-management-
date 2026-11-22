package com.itsm.service;

import com.itsm.domain.catalog.Category;
import com.itsm.domain.catalog.ITService;
import com.itsm.domain.catalog.SlaPolicy;
import com.itsm.exception.ConflictException;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.ServiceNotFoundException;
import com.itsm.repository.CategoryRepository;
import com.itsm.repository.ITServiceRepository;
import com.itsm.repository.SlaPolicyRepository;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Requests;
import com.itsm.web.dto.Views.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/** Configuration of IT services, categories and SLA policies. */
@Service
@Transactional
public class CatalogService {

    private final ITServiceRepository services;
    private final CategoryRepository categories;
    private final SlaPolicyRepository policies;

    public CatalogService(ITServiceRepository services, CategoryRepository categories, SlaPolicyRepository policies) {
        this.services = services;
        this.categories = categories;
        this.policies = policies;
    }

    // ---- services ----
    @Transactional(readOnly = true)
    public List<ServiceView> services(boolean activeOnly) {
        var list = activeOnly ? services.findByActiveTrueOrderByNameAsc()
                : services.findAll().stream().sorted(Comparator.comparing(ITService::getName)).toList();
        return list.stream().map(Mapper::service).toList();
    }

    public ServiceView createService(Requests.ServiceReq r) {
        services.findByNameIgnoreCase(r.name()).ifPresent(s -> { throw new ConflictException("Service " + r.name() + " already exists"); });
        ITService s = new ITService(r.name().trim(), r.description(), r.businessCriticality(), r.userBase() == null ? 100 : r.userBase(), r.ownerTeam());
        services.save(s);
        if (r.dependsOnIds() != null) s.replaceDependencies(services.findAllById(r.dependsOnIds()));
        return Mapper.service(s);
    }

    public ServiceView updateService(Long id, Requests.ServiceReq r) {
        ITService s = services.findById(id).orElseThrow(() -> new ServiceNotFoundException("Service " + id + " not found"));
        services.findByNameIgnoreCase(r.name()).filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw new ConflictException("Service " + r.name() + " already exists"); });
        s.update(r.name().trim(), r.description(), r.businessCriticality(), r.userBase() == null ? s.getUserBase() : r.userBase(),
                r.ownerTeam(), r.active() == null || r.active());
        if (r.dependsOnIds() != null) s.replaceDependencies(services.findAllById(r.dependsOnIds()));
        return Mapper.service(s);
    }

    // ---- categories ----
    @Transactional(readOnly = true)
    public List<CategoryView> categories(boolean activeOnly) {
        var list = activeOnly ? categories.findByActiveTrueOrderByNameAsc()
                : categories.findAll().stream().sorted(Comparator.comparing(Category::getName)).toList();
        return list.stream().map(Mapper::category).toList();
    }

    public CategoryView createCategory(Requests.CategoryReq r) {
        categories.findByNameIgnoreCase(r.name().trim()).ifPresent(c -> { throw new ConflictException("Category " + r.name() + " already exists"); });
        Category c = new Category(r.name(), r.description(), r.requiredSkill(), r.keywords());
        return Mapper.category(categories.save(c));
    }

    public CategoryView updateCategory(Long id, Requests.CategoryReq r) {
        Category c = categories.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category " + id + " not found"));
        c.update(r.name(), r.description(), r.requiredSkill(), r.keywords(), r.active() == null || r.active());
        return Mapper.category(c);
    }

    // ---- SLA policies ----
    @Transactional(readOnly = true)
    public List<SlaPolicyView> policies() {
        return policies.findAll().stream()
                .sorted(Comparator.comparing((SlaPolicy p) -> p.getService() == null ? "" : p.getService().getName())
                        .thenComparing(SlaPolicy::getPriority))
                .map(Mapper::sla).toList();
    }

    public SlaPolicyView createPolicy(Requests.SlaPolicyReq r) {
        ITService svc = r.serviceId() == null ? null : services.findById(r.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException("Service " + r.serviceId() + " not found"));
        boolean exists = svc == null ? policies.findByServiceIsNullAndPriority(r.priority()).isPresent()
                : policies.findByServiceIdAndPriority(svc.getId(), r.priority()).isPresent();
        if (exists) throw new ConflictException("A policy for " + r.priority() + (svc == null ? " (default)" : " on " + svc.getName()) + " already exists");
        SlaPolicy p = new SlaPolicy(r.name(), svc, r.priority(), r.responseMinutes(), r.resolutionMinutes(),
                r.warningThresholdPercent() == null ? 75 : r.warningThresholdPercent());
        return Mapper.sla(policies.save(p));
    }

    public SlaPolicyView updatePolicy(Long id, Requests.SlaPolicyReq r) {
        SlaPolicy p = policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("SLA policy " + id + " not found"));
        p.update(r.name(), r.responseMinutes(), r.resolutionMinutes(),
                r.warningThresholdPercent() == null ? p.getWarningThresholdPercent() : r.warningThresholdPercent());
        return Mapper.sla(p);
    }

    public void deletePolicy(Long id) {
        SlaPolicy p = policies.findById(id).orElseThrow(() -> new ResourceNotFoundException("SLA policy " + id + " not found"));
        if (p.getService() == null) {
            throw new ConflictException("Default SLA policies cannot be deleted (every priority needs a fallback); edit it instead");
        }
        policies.delete(p);
    }
}
