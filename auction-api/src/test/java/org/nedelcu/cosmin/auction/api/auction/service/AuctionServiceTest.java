package org.nedelcu.cosmin.auction.api.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.BidResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.event.AuctionRealtimeEvent;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxService;
import org.nedelcu.cosmin.auction.api.common.websocket.AuctionEventBroadcaster;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuctionEventBroadcaster auctionEventBroadcaster;

    @InjectMocks
    private AuctionService auctionService;

    @Captor
    private ArgumentCaptor<Object> outboxPayloadCaptor;

    @Captor
    private ArgumentCaptor<Object> realtimePayloadCaptor;

    @Test
    void placeBidPublishesBidPlacedAndAuctionExtendedWhenInsideAntiSnipingWindow() {
        Long auctionId = 10L;
        OffsetDateTime initialEndTime = OffsetDateTime.now().plusSeconds(10);

        AuctionEntity auction = runningAuction(auctionId, initialEndTime, 15, 30);
        BidEntity savedBid = new BidEntity();
        savedBid.setId(55L);
        savedBid.setAuctionId(auctionId);
        savedBid.setBidderId(200L);
        savedBid.setAmount(new BigDecimal("125.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.saveAndFlush(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(bidRepository.save(any(BidEntity.class))).thenReturn(savedBid);

        BidResponse response = auctionService.placeBid(
                auctionId,
                new PlaceBidRequest(200L, new BigDecimal("125.00"))
        );

        assertThat(response.id()).isEqualTo(55L);
        assertThat(response.auctionExtended()).isTrue();
        assertThat(response.newEndTime()).isEqualTo(initialEndTime.plusSeconds(30));
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo("125.00");
        assertThat(auction.getEndTime()).isEqualTo(initialEndTime.plusSeconds(30));

        verify(outboxService, times(2)).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getAllValues())
                .hasExactlyElementsOfTypes(BidPlacedEvent.class, AuctionExtendedEvent.class);

        verify(auctionEventBroadcaster, times(2)).broadcastToAuction(any(), realtimePayloadCaptor.capture());
        assertThat(realtimePayloadCaptor.getAllValues())
                .hasExactlyElementsOfTypes(AuctionRealtimeEvent.class, AuctionRealtimeEvent.class);

        AuctionRealtimeEvent<?> bidRealtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getAllValues().get(0);
        assertThat(bidRealtimeEvent.type()).isEqualTo(AuctionEventType.BID_PLACED.name());
        assertThat(bidRealtimeEvent.payload()).isInstanceOf(BidPlacedEvent.class);

        AuctionRealtimeEvent<?> extendedRealtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getAllValues().get(1);
        assertThat(extendedRealtimeEvent.type()).isEqualTo(AuctionEventType.AUCTION_EXTENDED.name());
        assertThat(extendedRealtimeEvent.payload()).isInstanceOf(AuctionExtendedEvent.class);
    }

    @Test
    void closePublishesClosedEventToOutboxAndRealtimeChannel() {
        Long auctionId = 20L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(1), 30, 30);
        auction.setCurrentPrice(new BigDecimal("310.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.close(auctionId);

        assertThat(response.status()).isEqualTo(AuctionStatus.ENDED);

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getValue()).isInstanceOf(AuctionClosedEvent.class);

        verify(auctionEventBroadcaster).broadcastToAuction(any(), realtimePayloadCaptor.capture());
        AuctionRealtimeEvent<?> realtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getValue();
        assertThat(realtimeEvent.type()).isEqualTo(AuctionEventType.AUCTION_CLOSED.name());
        assertThat(realtimeEvent.payload()).isInstanceOf(AuctionClosedEvent.class);
    }

    private AuctionEntity runningAuction(
            Long id,
            OffsetDateTime endTime,
            int antiSnipingWindowSec,
            int antiSnipingExtendSec
    ) {
        AuctionEntity auction = new AuctionEntity();
        auction.setId(id);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setMinIncrement(new BigDecimal("5.00"));
        auction.setEndTime(endTime);
        auction.setAntiSnipingWindowSec(antiSnipingWindowSec);
        auction.setAntiSnipingExtendSec(antiSnipingExtendSec);
        return auction;
    }
}
