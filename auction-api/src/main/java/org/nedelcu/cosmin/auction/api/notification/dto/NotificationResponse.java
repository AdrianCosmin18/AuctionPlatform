package org.nedelcu.cosmin.auction.api.notification.dto;

import java.time.OffsetDateTime;
import org.nedelcu.cosmin.auction.shared.notification.NotificationType;

public record NotificationResponse(
        Long id,
        Long auctionId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime readAt
) {
}
