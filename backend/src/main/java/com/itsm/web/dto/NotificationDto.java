package com.itsm.web.dto;

import com.itsm.domain.notification.Notification;

import java.time.Instant;

public record NotificationDto(Long id, String channel, String eventType, String title, String message,
                              Long incidentId, String incidentNumber, boolean read, String deliveryStatus,
                              Instant createdAt, String recipientName, String recipientCode) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(n.getId(), n.getChannel().name(), n.getEventType(), n.getTitle(), n.getMessage(),
                n.getIncidentId(), n.getIncidentNumber(), n.isRead(), n.getDeliveryStatus(), n.getCreatedAt(),
                n.getRecipient().getFullName(), n.getRecipient().getCode());
    }
}
