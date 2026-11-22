package com.itsm.notification;

import com.itsm.domain.notification.NotificationChannelType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Factory: returns the channel implementation for a channel type. */
@Component
public class NotificationChannelFactory {

    private final Map<NotificationChannelType, NotificationChannel> channels = new EnumMap<>(NotificationChannelType.class);

    public NotificationChannelFactory(List<NotificationChannel> available) {
        available.forEach(c -> channels.put(c.type(), c));
    }

    public Optional<NotificationChannel> forType(NotificationChannelType type) {
        return Optional.ofNullable(channels.get(type));
    }
}
