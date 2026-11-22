package com.itsm.notification;

import com.itsm.config.ItsmProperties;
import com.itsm.domain.notification.Notification;
import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Adapter: adapts Spring's {@link JavaMailSender} (SMTP) to our {@link NotificationChannel}.
 * When e-mail is disabled (default in dev), delivery is simulated and only logged.
 */
@Component
public class EmailNotificationChannel extends AbstractPersistingChannel {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationChannel.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final ItsmProperties props;

    public EmailNotificationChannel(NotificationRepository repository, Clock clock,
                                    ObjectProvider<JavaMailSender> mailSender, ItsmProperties props) {
        super(repository, clock);
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override public NotificationChannelType type() { return NotificationChannelType.EMAIL; }

    @Override
    protected DeliveryResult deliver(NotificationMessage m, Notification record) {
        String to = m.recipient().getEmail();
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!props.getNotifications().isEmailEnabled() || sender == null) {
            log.info("[EMAIL simulated] to={} subject=\"{}\"", to, m.title());
            return DeliveryResult.simulated("would e-mail " + to);
        }
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(props.getNotifications().getEmailFrom());
        mail.setTo(to);
        mail.setSubject("[I-ITSM] " + m.title());
        mail.setText(m.body() + "\n\n-- I-ITSM automated notification");
        sender.send(mail);
        return DeliveryResult.sent("e-mailed " + to);
    }
}
