package com.itsm.service;

import com.itsm.domain.catalog.ITService;
import com.itsm.domain.incident.Incident;
import com.itsm.domain.user.User;
import com.itsm.exception.InvalidInputException;
import com.itsm.exception.ServiceNotFoundException;
import com.itsm.repository.ITServiceRepository;
import com.itsm.web.dto.Requests;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Factory pattern: validates the request and builds a well-formed NEW incident. */
@Component
public class IncidentFactory {

    private final ITServiceRepository services;

    public IncidentFactory(ITServiceRepository services) {
        this.services = services;
    }

    public Incident create(Requests.CreateIncident req, User reporter, Instant now) {
        ITService service = services.findById(req.serviceId())
                .orElseThrow(() -> new ServiceNotFoundException("IT service " + req.serviceId() + " does not exist"));
        if (!service.isActive()) throw new InvalidInputException("Service " + service.getName() + " is retired; choose another");
        int affected = req.affectedUsers() == null ? 1 : req.affectedUsers();
        return new Incident(sanitize(req.title()), sanitize(req.description()), reporter, service,
                req.impact(), req.urgency(), affected, now);
    }

    /** Strip control characters; HTML is escaped by the UI, so we only normalise whitespace here. */
    static String sanitize(String s) {
        return s == null ? null : s.replaceAll("[\\p{Cntrl}&&[^\n\t]]", "").strip();
    }
}
