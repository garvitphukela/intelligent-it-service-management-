package com.itsm.notification;

import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.repository.NotificationRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;

/** Adapter around {@link ThirdPartySmsGatewayClient}. Used only for urgent events. */
@Component
public class SmsNotificationChannel extends AbstractPersistingChannel {

    private static final int MAX_SMS = 160;

    private final ThirdPartySmsGatewayClient gateway;

    public SmsNotificationChannel(NotificationRepository repository, Clock clock, ThirdPartySmsGatewayClient gateway) {
        super(repository, clock);
        this.gateway = gateway;
    }

    @Override public NotificationChannelType type() { return NotificationChannelType.SMS; }

    @Override
    protected DeliveryResult deliver(NotificationMessage m, Notification record) {
        String text = (m.title() + ": " + m.body());
        if (text.length() > MAX_SMS) text = text.substring(0, MAX_SMS - 3) + "...";
        var response = gateway.dispatch(m.recipient().getPhone(), text, m.urgent() ? 1 : 3);
        return switch (response.code()) {
            case 200, 202 -> DeliveryResult.simulated("sms sid " + response.messageSid());
            case 400 -> DeliveryResult.failed("recipient has no phone number");
            default -> DeliveryResult.failed("gateway error " + response.code());
        };
    }
}
