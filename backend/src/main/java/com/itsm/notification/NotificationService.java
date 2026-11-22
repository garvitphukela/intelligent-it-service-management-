package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.domain.user.User;
import com.itsm.event.Audience;
import com.itsm.event.Events;
import com.itsm.event.IncidentEvent;
import com.itsm.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Turns an event into concrete messages: resolves the event's audience to users,
 * then sends through each user's preferred channels (SMS only for urgent events).
 */
@Service
public class NotificationService {

    private final UserRepository users;
    private final NotificationChannelFactory channels;

    public NotificationService(UserRepository users, NotificationChannelFactory channels) {
        this.users = users;
        this.channels = channels;
    }

    public int dispatch(IncidentEvent event) {
        Set<User> recipients = resolveAudience(event);
        if (event.actor() != null) recipients.removeIf(u -> u.getId().equals(event.actor().getId()));
        int sent = 0;
        for (User user : recipients) {
            if (!user.isActive()) continue;
            NotificationMessage msg = new NotificationMessage(user, event.type(), event.title(), event.description(),
                    event.incident().getId(), event.incident().getNumber(), event.isUrgent());
            for (NotificationChannelType type : channelsFor(user, event)) {
                var channel = channels.forType(type);
                if (channel.isPresent()) {
                    channel.get().send(msg);
                    sent++;
                }
            }
        }
        return sent;
    }

    private Set<NotificationChannelType> channelsFor(User user, IncidentEvent event) {
        Set<NotificationChannelType> set = EnumSet.of(NotificationChannelType.IN_APP);
        Set<NotificationChannelType> prefs = user.getPreferredChannels();
        if (prefs.contains(NotificationChannelType.EMAIL)) set.add(NotificationChannelType.EMAIL);
        if (event.isUrgent() && prefs.contains(NotificationChannelType.SMS)) set.add(NotificationChannelType.SMS);
        return set;
    }

    Set<User> resolveAudience(IncidentEvent event) {
        Set<User> result = new LinkedHashSet<>();
        var incident = event.incident();
        for (Audience a : event.audience()) {
            switch (a) {
                case REPORTER -> result.add(incident.getReporter());
                case ASSIGNEE -> { if (incident.getAssignedTechnician() != null) result.add(incident.getAssignedTechnician()); }
                case PREVIOUS_ASSIGNEE -> {
                    if (event instanceof Events.IncidentAssigned assigned && assigned.previousTechnician() != null) {
                        result.add(assigned.previousTechnician());
                    }
                }
                case TEAM_LEADS -> result.addAll(users.findActiveTeamLeads());
                case ADMINS -> result.addAll(users.findActiveAdministrators());
                case MANAGERS -> result.addAll(users.findActiveManagers());
            }
        }
        return result;
    }
}
