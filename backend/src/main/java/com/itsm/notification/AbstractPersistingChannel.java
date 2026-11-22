package com.itsm.notification;

import com.itsm.domain.notification.Notification;
import com.itsm.repository.NotificationRepository;

import java.time.Clock;
import java.time.Instant;

/**
 * Template Method: records every delivery attempt in the notification log, then
 * lets the subclass perform the actual transport in {@link #deliver}.
 */
public abstract class AbstractPersistingChannel implements NotificationChannel {

    protected final NotificationRepository repository;
    protected final Clock clock;

    protected AbstractPersistingChannel(NotificationRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public final DeliveryResult send(NotificationMessage m) {
        Notification n = new Notification(m.recipient(), type(), m.eventType(), m.title(), m.body(),
                m.incidentId(), m.incidentNumber(), Instant.now(clock));
        DeliveryResult result;
        try {
            result = deliver(m, n);
        } catch (RuntimeException ex) {
            result = DeliveryResult.failed(ex.getMessage());
        }
        n.markDelivery(result.status());
        repository.save(n);
        afterPersist(n);
        return result;
    }

    protected abstract DeliveryResult deliver(NotificationMessage message, Notification record);

    protected void afterPersist(Notification saved) {}
}
