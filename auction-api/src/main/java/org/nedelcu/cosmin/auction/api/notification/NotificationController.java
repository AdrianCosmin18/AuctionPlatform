package org.nedelcu.cosmin.auction.api.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auth.CurrentUserService;
import org.nedelcu.cosmin.auction.api.notification.dto.NotificationResponse;
import org.nedelcu.cosmin.auction.api.notification.dto.UnreadNotificationsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<NotificationResponse> getNotifications() {
        return notificationService.findByUserId(currentUserService.getCurrentUserId());
    }

    @GetMapping("/unread-count")
    public UnreadNotificationsResponse unreadCount() {
        return notificationService.unreadCount(currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable("id") Long id) {
        return notificationService.markAsRead(currentUserService.getCurrentUserId(), id);
    }

    @PostMapping("/read-all")
    public UnreadNotificationsResponse markAllAsRead() {
        return notificationService.markAllAsRead(currentUserService.getCurrentUserId());
    }
}
