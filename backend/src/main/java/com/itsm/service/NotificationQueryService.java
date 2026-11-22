package com.itsm.service;

import com.itsm.domain.notification.NotificationChannelType;
import com.itsm.exception.ResourceNotFoundException;
import com.itsm.exception.UnauthorizedOperationException;
import com.itsm.repository.NotificationRepository;
import com.itsm.security.CurrentUserService;
import com.itsm.web.dto.NotificationDto;
import com.itsm.web.dto.Views.PageView;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@Transactional
public class NotificationQueryService {

    private final NotificationRepository notifications;
    private final CurrentUserService currentUser;

    public NotificationQueryService(NotificationRepository notifications, CurrentUserService currentUser) {
        this.notifications = notifications;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageView<NotificationDto> mine(int page, int size) {
        Long me = currentUser.principal().id();
        return PageView.of(notifications.findByRecipientIdAndChannelOrderByCreatedAtDesc(me, NotificationChannelType.IN_APP,
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)))).map(NotificationDto::from));
    }

    @Transactional(readOnly = true)
    public Map<String, Long> unreadCount() {
        return Map.of("unread", notifications.countByRecipientIdAndChannelAndReadFlagFalse(currentUser.principal().id(), NotificationChannelType.IN_APP));
    }

    public void markRead(Long id) {
        var n = notifications.findById(id).orElseThrow(() -> new ResourceNotFoundException("Notification " + id + " not found"));
        if (!n.getRecipient().getId().equals(currentUser.principal().id())) {
            throw new UnauthorizedOperationException("This notification belongs to another user");
        }
        n.markRead();
    }

    public int markAllRead() {
        return notifications.markAllRead(currentUser.principal().id());
    }

    /** Admin: full delivery log across all channels (shows simulated e-mail/SMS). */
    @Transactional(readOnly = true)
    public PageView<NotificationDto> log(NotificationChannelType channel, int page, int size) {
        var pageable = PageRequest.of(Math.max(0, page), Math.min(200, Math.max(1, size)));
        var p = channel == null ? notifications.findAllByOrderByCreatedAtDesc(pageable)
                : notifications.findByChannelOrderByCreatedAtDesc(channel, pageable);
        return PageView.of(p.map(NotificationDto::from));
    }
}
