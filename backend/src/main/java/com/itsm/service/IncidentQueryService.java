package com.itsm.service;

import com.itsm.domain.incident.Incident;
import com.itsm.domain.incident.IncidentStatus;
import com.itsm.domain.user.Role;
import com.itsm.domain.user.User;
import com.itsm.exception.InvalidInputException;
import com.itsm.repository.IncidentRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Views.IncidentSummary;
import com.itsm.web.dto.Views.PageView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Dynamic, role-scoped incident search built with JPA Criteria (Specification). */
@Service
@Transactional(readOnly = true)
public class IncidentQueryService {

    private static final Set<String> SORTABLE = Set.of("createdAt", "updatedAt", "priority", "status", "id");

    private final IncidentRepository incidents;
    private final CurrentUserService currentUser;

    public IncidentQueryService(IncidentRepository incidents, CurrentUserService currentUser) {
        this.incidents = incidents;
        this.currentUser = currentUser;
    }

    public PageView<IncidentSummary> search(IncidentSearchCriteria c, int page, int size, String sort, String dir) {
        User user = currentUser.user();
        if (size < 1 || size > 200) throw new InvalidInputException("Page size must be between 1 and 200");
        String sortField = sort == null || sort.isBlank() ? "createdAt" : sort;
        if (!SORTABLE.contains(sortField)) throw new InvalidInputException("Cannot sort by " + sortField);
        Sort.Direction direction = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        // priority enum P1..P4 sorts naturally; "asc" means most critical first
        var pageable = PageRequest.of(Math.max(0, page), size, Sort.by(direction, sortField).and(Sort.by(Sort.Direction.DESC, "id")));
        return PageView.of(incidents.findAll(spec(c, user), pageable).map(Mapper::incident));
    }

    static Specification<Incident> spec(IncidentSearchCriteria c, User user) {
        return (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (user.getRole() == Role.EMPLOYEE) {
                p.add(cb.equal(root.get("reporter").get("id"), user.getId()));
            }
            if (Boolean.TRUE.equals(c.mine()) && user.getRole().isSupportEngineer()) {
                p.add(cb.equal(root.get("assignedTechnician").get("id"), user.getId()));
            }
            if (c.number() != null && !c.number().isBlank()) {
                Long id = Incident.idFromNumber(c.number());
                p.add(id == null ? cb.disjunction() : cb.equal(root.get("id"), id));
            }
            if (c.q() != null && !c.q().isBlank()) {
                String like = "%" + c.q().toLowerCase(Locale.ROOT).trim() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("description")), like)));
            }
            if (c.status() != null && !c.status().isEmpty()) p.add(root.get("status").in(c.status()));
            if (Boolean.TRUE.equals(c.openOnly())) p.add(root.get("status").in(IncidentStatus.openStatuses()));
            if (c.priority() != null && !c.priority().isEmpty()) p.add(root.get("priority").in(c.priority()));
            if (c.categoryId() != null) p.add(cb.equal(root.get("category").get("id"), c.categoryId()));
            if (c.serviceId() != null) p.add(cb.equal(root.get("service").get("id"), c.serviceId()));
            if (c.technicianId() != null) p.add(cb.equal(root.get("assignedTechnician").get("id"), c.technicianId()));
            if (c.reporterId() != null) p.add(cb.equal(root.get("reporter").get("id"), c.reporterId()));
            if (c.slaStatus() != null) p.add(cb.equal(root.get("slaStatus"), c.slaStatus()));
            if (Boolean.TRUE.equals(c.unassigned())) p.add(cb.isNull(root.get("assignedTechnician")));
            if (Boolean.TRUE.equals(c.majorOnly())) p.add(cb.isTrue(root.get("majorIncident")));
            if (c.from() != null) p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.from().atStartOfDay().toInstant(ZoneOffset.UTC)));
            if (c.to() != null) p.add(cb.lessThan(root.get("createdAt"), c.to().plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC)));
            return cb.and(p.toArray(Predicate[]::new));
        };
    }
}
