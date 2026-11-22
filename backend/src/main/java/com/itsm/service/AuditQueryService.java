package com.itsm.service;

import com.itsm.domain.audit.AuditEntry;
import com.itsm.domain.user.User;
import com.itsm.repository.AuditEntryRepository;
import com.itsm.repository.IncidentRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.exception.IncidentNotFoundException;
import com.itsm.web.dto.Mapper;
import com.itsm.web.dto.Views.AuditView;
import com.itsm.web.dto.Views.PageView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class AuditQueryService {

    private final AuditEntryRepository audit;
    private final IncidentRepository incidents;
    private final IncidentAccessPolicy access;
    private final CurrentUserService currentUser;

    public AuditQueryService(AuditEntryRepository audit, IncidentRepository incidents, IncidentAccessPolicy access,
                             CurrentUserService currentUser) {
        this.audit = audit;
        this.incidents = incidents;
        this.access = access;
        this.currentUser = currentUser;
    }

    public List<AuditView> forIncident(Long incidentId) {
        User user = currentUser.user();
        var i = incidents.findById(incidentId).orElseThrow(() -> new IncidentNotFoundException("Incident " + incidentId + " not found"));
        access.require(access.canView(user, i), "view the history of " + i.getNumber());
        return audit.findByIncidentIdOrderByTimestampAscIdAsc(incidentId).stream().map(Mapper::audit).toList();
    }

    public PageView<AuditView> search(String action, String actor, String incidentNumber, int page, int size) {
        Specification<AuditEntry> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (action != null && !action.isBlank()) p.add(cb.equal(root.get("action"), action.trim().toUpperCase(Locale.ROOT)));
            if (actor != null && !actor.isBlank()) p.add(cb.like(cb.lower(root.get("actorName")), "%" + actor.toLowerCase(Locale.ROOT) + "%"));
            if (incidentNumber != null && !incidentNumber.isBlank()) p.add(cb.equal(cb.upper(root.get("incidentNumber")), incidentNumber.trim().toUpperCase(Locale.ROOT)));
            return cb.and(p.toArray(Predicate[]::new));
        };
        var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)), Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        return PageView.of(audit.findAll(spec, pageable).map(Mapper::audit));
    }
}
