package org.nedelcu.cosmin.auction.api.notification;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.common.exception.ResourceNotFoundException;
import org.nedelcu.cosmin.auction.api.notification.dto.NotificationResponse;
import org.nedelcu.cosmin.auction.api.notification.dto.UnreadNotificationsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> findByUserId(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public UnreadNotificationsResponse unreadCount(Long userId) {
        return new UnreadNotificationsResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        NotificationEntity notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(OffsetDateTime.now());
        }

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public UnreadNotificationsResponse markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        return unreadCount(userId);
    }

    private NotificationResponse toResponse(NotificationEntity notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getAuctionId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}
