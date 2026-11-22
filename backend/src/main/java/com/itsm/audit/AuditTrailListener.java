package com.itsm.audit;

import com.itsm.domain.audit.AuditEntry;
import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventListener;
import com.itsm.repository.AuditEntryRepository;
import org.springframework.stereotype.Component;

/** Observer: writes an immutable audit record for every incident event. */
@Component
public class AuditTrailListener implements IncidentEventListener {

    private final AuditEntryRepository repository;

    public AuditTrailListener(AuditEntryRepository repository) {
        this.repository = repository;
    }

    @Override public int order() { return 10; }

    @Override
    public void onEvent(IncidentEvent e) {
        var incident = e.incident();
        String desc = e.description();
        if (desc != null && desc.length() > 1000) desc = desc.substring(0, 997) + "...";
        repository.save(new AuditEntry(e.occurredAt(), e.actor() == null ? null : e.actor().getId(), e.actorName(),
                e.type(), incident.getId(), incident.getNumber(), e.previousState(), e.newState(), desc));
    }
}
