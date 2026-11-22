package com.itsm.domain.notification;

import com.itsm.domain.user.User;
import jakarta.persistence.*;

import java.time.Instant;

/** A delivered (or simulated) notification. One row per recipient per channel. */
@Entity
@Table(name = "notifications", indexes = @Index(columnList = "recipient_id,readFlag"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id")
    private User recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationChannelType channel;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String message;

    private Long incidentId;

    private String incidentNumber;

    private boolean readFlag;

    /** Delivery status reported by the channel adapter (SENT, SIMULATED, FAILED). */
    @Column(length = 20)
    private String deliveryStatus;

    @Column(nullable = false)
    private Instant createdAt;

    protected Notification() {}

    public Notification(User recipient, NotificationChannelType channel, String eventType, String title,
                        String message, Long incidentId, String incidentNumber, Instant createdAt) {
        this.recipient = recipient;
        this.channel = channel;
        this.eventType = eventType;
        this.title = title;
        this.message = message;
        this.incidentId = incidentId;
        this.incidentNumber = incidentNumber;
        this.createdAt = createdAt;
    }

    public void markRead() { this.readFlag = true; }
    public void markDelivery(String status) { this.deliveryStatus = status; }

    public Long getId() { return id; }
    public User getRecipient() { return recipient; }
    public NotificationChannelType getChannel() { return channel; }
    public String getEventType() { return eventType; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Long getIncidentId() { return incidentId; }
    public String getIncidentNumber() { return incidentNumber; }
    public boolean isRead() { return readFlag; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
