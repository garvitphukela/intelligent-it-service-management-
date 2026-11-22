package com.itsm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Observer pattern subject. Services publish events here; every registered
 * {@link IncidentEventListener} is notified. A failing listener is isolated so that,
 * e.g., a broken SMS gateway can never roll back an incident update.
 */
@Component
public class IncidentEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IncidentEventPublisher.class);

    private final List<IncidentEventListener> listeners;

    public IncidentEventPublisher(List<IncidentEventListener> listeners) {
        this.listeners = listeners.stream().sorted(Comparator.comparingInt(IncidentEventListener::order)).toList();
    }

    public void publish(IncidentEvent event) {
        for (IncidentEventListener l : listeners) {
            if (!l.supports(event)) continue;
            try {
                l.onEvent(event);
            } catch (RuntimeException ex) {
                log.warn("Listener {} failed for {}: {}", l.getClass().getSimpleName(), event.type(), ex.getMessage());
            }
        }
    }

    public int listenerCount() { return listeners.size(); }
}
