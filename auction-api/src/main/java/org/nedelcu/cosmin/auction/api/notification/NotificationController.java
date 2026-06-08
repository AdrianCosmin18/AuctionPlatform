package org.nedelcu.cosmin.auction.api.notification;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.notification.dto.NotificationResponse;
import org.nedelcu.cosmin.auction.api.notification.dto.UnreadNotificationsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private static final long DEFAULT_USER_ID = 2L;

    private final NotificationService notificationService;

    @GetMapping
    public List<NotificationResponse> getNotifications(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return notificationService.findByUserId(resolveCurrentUserId(currentUserId));
    }

    @GetMapping("/unread-count")
    public UnreadNotificationsResponse unreadCount(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return notificationService.unreadCount(resolveCurrentUserId(currentUserId));
    }

    @PostMapping("/{id}/read")
    public NotificationResponse markAsRead(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return notificationService.markAsRead(resolveCurrentUserId(currentUserId), id);
    }

    @PostMapping("/read-all")
    public UnreadNotificationsResponse markAllAsRead(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return notificationService.markAllAsRead(resolveCurrentUserId(currentUserId));
    }

    private Long resolveCurrentUserId(Long currentUserId) {
        return currentUserId != null ? currentUserId : DEFAULT_USER_ID;
    }
}
