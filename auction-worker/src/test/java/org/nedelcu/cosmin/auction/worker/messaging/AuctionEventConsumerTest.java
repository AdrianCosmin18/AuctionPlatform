package org.nedelcu.cosmin.auction.worker.messaging;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventEnvelope;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.nedelcu.cosmin.auction.worker.audit.AuditService;
import org.nedelcu.cosmin.auction.worker.notification.NotificationService;

@ExtendWith(MockitoExtension.class)
class AuctionEventConsumerTest {

    @Mock
    private AuditService auditService;

    @Mock
    private NotificationService notificationService;

    @Test
    void consumeSavesBidPlacedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, notificationService, objectMapper);
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        BidPlacedEvent payload = new BidPlacedEvent(
                10L,
                20L,
                30L,
                new BigDecimal("125.00"),
                new BigDecimal("125.00"),
                occurredAt
        );

        auctionEventConsumer.consume(new AuctionEventEnvelope(
                AuctionEventType.BID_PLACED.name(),
                objectMapper.writeValueAsString(payload)
        ));

        verify(auditService).save(AuctionEventType.BID_PLACED.name(), 10L, objectMapper.writeValueAsString(payload));
        verify(notificationService).handleBidPlaced(payload);
    }

    @Test
    void consumeSavesAuctionExtendedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, notificationService, objectMapper);
        OffsetDateTime occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        AuctionExtendedEvent payload = new AuctionExtendedEvent(
                11L,
                occurredAt.plusMinutes(1),
                occurredAt
        );

        auctionEventConsumer.consume(new AuctionEventEnvelope(
                AuctionEventType.AUCTION_EXTENDED.name(),
                objectMapper.writeValueAsString(payload)
        ));

        verify(auditService).save(
                AuctionEventType.AUCTION_EXTENDED.name(),
                11L,
                objectMapper.writeValueAsString(payload)
        );
        verify(notificationService).handleAuctionExtended(payload);
    }

    @Test
    void consumeSavesAuctionClosedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, notificationService, objectMapper);
        OffsetDateTime closedAt = OffsetDateTime.now(ZoneOffset.UTC);
        AuctionClosedEvent payload = new AuctionClosedEvent(
                12L,
                99L,
                44L,
                new BigDecimal("300.00"),
                AuctionCloseReason.MANUAL,
                closedAt
        );

        auctionEventConsumer.consume(new AuctionEventEnvelope(
                AuctionEventType.AUCTION_CLOSED.name(),
                objectMapper.writeValueAsString(payload)
        ));

        verify(auditService).save(
                AuctionEventType.AUCTION_CLOSED.name(),
                12L,
                objectMapper.writeValueAsString(payload)
        );
        verify(notificationService).handleAuctionClosed(payload);
    }
}
