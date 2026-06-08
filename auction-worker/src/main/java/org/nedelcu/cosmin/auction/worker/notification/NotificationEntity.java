package org.nedelcu.cosmin.auction.worker.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.nedelcu.cosmin.auction.shared.notification.NotificationType;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "notifications_seq_generator")
    @SequenceGenerator(name = "notifications_seq_generator", sequenceName = "notifications_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "auction_id")
    private Long auctionId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;

    private String message;

    @Column(name = "is_read")
    private boolean read;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "read_at")
    private OffsetDateTime readAt;

    public NotificationEntity() {
    }

    public NotificationEntity(
            Long id,
            Long userId,
            Long auctionId,
            NotificationType type,
            String title,
            String message,
            boolean read,
            OffsetDateTime createdAt,
            OffsetDateTime readAt
    ) {
        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }
}
