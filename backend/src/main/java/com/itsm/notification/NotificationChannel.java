package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannelType;

/**
 * Target interface of the Adapter pattern. Each channel adapts a different delivery
 * mechanism (database + WebSocket, SMTP, an SMS vendor SDK) to this single contract.
 * Adding e.g. Slack or push notifications means adding one class.
 */
public interface NotificationChannel {

    NotificationChannelType type();

    DeliveryResult send(NotificationMessage message);
}
