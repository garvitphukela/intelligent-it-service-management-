package com.itsm.notification;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.repository.NotificationRepository;
import com.itsm.web.dto.NotificationDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Stores the notification for the in-app bell and pushes it live over WebSocket. */
@Component
public class InAppNotificationChannel extends AbstractPersistingChannel {

    private final SimpMessagingTemplate messaging;

    public InAppNotificationChannel(NotificationRepository repository, Clock clock, SimpMessagingTemplate messaging) {
        super(repository, clock);
        this.messaging = messaging;
    }

    @Override public NotificationChannelType type() { return NotificationChannelType.IN_APP; }

    @Override
    protected DeliveryResult deliver(NotificationMessage message, Notification record) {
        return DeliveryResult.sent("stored for in-app inbox");
    }

    @Override
    protected void afterPersist(Notification saved) {
        messaging.convertAndSendToUser(String.valueOf(saved.getRecipient().getId()), "/queue/notifications", NotificationDto.from(saved));
    }
}
