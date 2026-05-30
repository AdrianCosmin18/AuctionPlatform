package org.nedelcu.cosmin.auction.api.common.outbox;

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
import lombok.Getter;
import lombok.Setter;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
public class OutboxEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "outbox_events_seq_generator")
    @SequenceGenerator(name = "outbox_events_seq_generator", sequenceName = "outbox_events_seq", allocationSize = 1)
    private Long id;

    @Column(name = "aggregate_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxAggregateType aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private AuctionEventType eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxEventStatus status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;
}
