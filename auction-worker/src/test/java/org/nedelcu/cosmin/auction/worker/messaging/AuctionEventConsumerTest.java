package org.nedelcu.cosmin.auction.worker.messaging;

import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventEnvelope;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.nedelcu.cosmin.auction.worker.audit.AuditService;

@ExtendWith(MockitoExtension.class)
class AuctionEventConsumerTest {

    @Mock
    private AuditService auditService;

    @Test
    void consumeSavesBidPlacedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, objectMapper);
        BidPlacedEvent payload = new BidPlacedEvent(
                10L,
                20L,
                30L,
                new BigDecimal("125.00"),
                new BigDecimal("125.00"),
                OffsetDateTime.now()
        );

        auctionEventConsumer.consume(new AuctionEventEnvelope(
                AuctionEventType.BID_PLACED.name(),
                objectMapper.writeValueAsString(payload)
        ));

        verify(auditService).save(AuctionEventType.BID_PLACED.name(), 10L, objectMapper.writeValueAsString(payload));
    }

    @Test
    void consumeSavesAuctionExtendedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, objectMapper);
        AuctionExtendedEvent payload = new AuctionExtendedEvent(
                11L,
                OffsetDateTime.now().plusMinutes(1),
                OffsetDateTime.now()
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
    }

    @Test
    void consumeSavesAuctionClosedEventToAudit() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        AuctionEventConsumer auctionEventConsumer = new AuctionEventConsumer(auditService, objectMapper);
        AuctionClosedEvent payload = new AuctionClosedEvent(
                12L,
                new BigDecimal("300.00"),
                OffsetDateTime.now()
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
    }
}
