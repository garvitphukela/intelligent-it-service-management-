package com.itsm.notification;

import com.itsm.domain.user.User;

/** Channel-independent message. Channels adapt it to their own wire format. */
public record NotificationMessage(User recipient, String eventType, String title, String body,
                                  Long incidentId, String incidentNumber, boolean urgent) {}
