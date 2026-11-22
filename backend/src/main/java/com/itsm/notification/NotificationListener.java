package com.itsm.notification;

import com.itsm.event.IncidentEvent;
import com.itsm.event.IncidentEventListener;
import org.springframework.stereotype.Component;

/** Observer: forwards every incident event to the notification service. */
@Component
public class NotificationListener implements IncidentEventListener {

    private final NotificationService notifications;

    public NotificationListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @Override public boolean supports(IncidentEvent event) { return !event.audience().isEmpty(); }

    @Override public int order() { return 20; }

    @Override public void onEvent(IncidentEvent event) { notifications.dispatch(event); }
}
