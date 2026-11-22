package com.itsm.web.controller;

import com.itsm.service.NotificationQueryService;
import com.itsm.web.dto.NotificationDto;
import com.itsm.web.dto.Views.PageView;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationQueryService notifications;

    public NotificationController(NotificationQueryService notifications) {
        this.notifications = notifications;
    }

    @GetMapping
    public PageView<NotificationDto> mine(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return notifications.mine(page, size);
    }

    @GetMapping("/unread-count")
    public Map<String, Long> unread() { return notifications.unreadCount(); }

    @PostMapping("/{id}/read")
    public void read(@PathVariable Long id) { notifications.markRead(id); }

    @PostMapping("/read-all")
    public Map<String, Integer> readAll() { return Map.of("updated", notifications.markAllRead()); }
}
