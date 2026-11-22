package com.itsm.realtime;

import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/** Observer: streams every event to connected dashboards (real-time operations view). */
@Component
public class DashboardBroadcastListener implements IncidentEventListener {

    public record LiveEvent(String type, Long incidentId, String incidentNumber, String title, String description,
                            String priority, String status, String service, String actor, Instant at) {}

    private final SimpMessagingTemplate messaging;

    public DashboardBroadcastListener(SimpMessagingTemplate messaging) {
        this.messaging = messaging;
    }

    @Override public int order() { return 30; }

    @Override
    public void onEvent(IncidentEvent e) {
        var i = e.incident();
        messaging.convertAndSend("/topic/events", new LiveEvent(e.type(), i.getId(), i.getNumber(), i.getTitle(),
                e.description(), i.getPriority() == null ? null : i.getPriority().name(), i.getStatus().name(),
                i.getService().getName(), e.actorName(), e.occurredAt()));
    }
}
