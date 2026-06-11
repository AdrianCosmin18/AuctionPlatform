package org.nedelcu.cosmin.auction.api.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionImageEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionWatchlistEntity;
import org.nedelcu.cosmin.auction.api.auction.dto.MyBidAuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.event.AuctionRealtimeEvent;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionImageRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionWatchlistRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxService;
import org.nedelcu.cosmin.auction.api.common.websocket.AuctionEventBroadcaster;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionStartedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionSuspendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionImageRepository auctionImageRepository;

    @Mock
    private AuctionMediaStorageService auctionMediaStorageService;

    @Mock
    private OutboxService outboxService;

    @Mock
    private AuctionEventBroadcaster auctionEventBroadcaster;

    @Mock
    private AuctionWatchlistRepository auctionWatchlistRepository;

    @InjectMocks
    private AuctionService auctionService;

    @Captor
    private ArgumentCaptor<Object> outboxPayloadCaptor;

    @Captor
    private ArgumentCaptor<Object> realtimePayloadCaptor;

    @Captor
    private ArgumentCaptor<List<AuctionImageEntity>> auctionImagesCaptor;

    @Test
    void createPersistsOrderedAuctionImagesAndReturnsThemInResponse() {
        OffsetDateTime endTime = OffsetDateTime.now().plusHours(3);
        AuctionEntity savedAuction = new AuctionEntity();
        savedAuction.setId(99L);
        savedAuction.setTitle("Camera Sony");
        savedAuction.setDescription("Mirrorless body");
        savedAuction.setCategoryCode("RARE_BOOKS");
        savedAuction.setSubcategoryCode("SIGNED_COPIES");
        savedAuction.setCreatorAuthor("Autor Demo");
        savedAuction.setEstimatedYear(1924);
        savedAuction.setLanguageCode("Romanian");
        savedAuction.setItemCondition("GOOD");
        savedAuction.setAuthenticityStatus("VERIFIED");
        savedAuction.setProvenance("Private collection");
        savedAuction.setStartPrice(new BigDecimal("500.00"));
        savedAuction.setCurrentPrice(new BigDecimal("500.00"));
        savedAuction.setMinIncrement(new BigDecimal("25.00"));
        savedAuction.setStatus(AuctionStatus.DRAFT);
        savedAuction.setEndTime(endTime);
        savedAuction.setAntiSnipingWindowSec(30);
        savedAuction.setAntiSnipingExtendSec(30);
        savedAuction.setCreatedBy(1L);
        savedAuction.setVersion(0L);

        when(auctionRepository.save(any(AuctionEntity.class))).thenReturn(savedAuction);
        when(auctionImageRepository.findByAuctionIdOrderByDisplayOrderAsc(99L)).thenAnswer(invocation -> {
            AuctionImageEntity first = new AuctionImageEntity();
            first.setId(1L);
            first.setAuctionId(99L);
            first.setImageUrl("https://img.test/sony-front.jpg");
            first.setDisplayOrder(0);

            AuctionImageEntity second = new AuctionImageEntity();
            second.setId(2L);
            second.setAuctionId(99L);
            second.setImageUrl("https://img.test/sony-back.jpg");
            second.setDisplayOrder(1);

            return List.of(first, second);
        });

        AuctionResponse response = auctionService.create(new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Camera Sony",
                "Mirrorless body",
                "RARE_BOOKS",
                "SIGNED_COPIES",
                "Autor Demo",
                1924,
                "Romanian",
                "GOOD",
                "VERIFIED",
                "Private collection",
                new BigDecimal("500.00"),
                new BigDecimal("25.00"),
                null,
                null,
                endTime,
                30,
                30,
                List.of("https://img.test/sony-front.jpg", "https://img.test/sony-back.jpg")
        ), 1L);

        verify(auctionImageRepository).saveAll(auctionImagesCaptor.capture());
        assertThat(auctionImagesCaptor.getValue()).hasSize(2);
        assertThat(auctionImagesCaptor.getValue().get(0).getDisplayOrder()).isEqualTo(0);
        assertThat(auctionImagesCaptor.getValue().get(1).getDisplayOrder()).isEqualTo(1);
        assertThat(response.images()).hasSize(2);
        assertThat(response.images().get(0).imageUrl()).isEqualTo("https://img.test/sony-front.jpg");
        assertThat(response.images().get(1).imageUrl()).isEqualTo("https://img.test/sony-back.jpg");
        assertThat(response.categoryCode()).isEqualTo("RARE_BOOKS");
        assertThat(response.subcategoryCode()).isEqualTo("SIGNED_COPIES");
        assertThat(response.authenticityStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void createRejectsSubcategoryThatDoesNotBelongToCategory() {
        OffsetDateTime endTime = OffsetDateTime.now().plusHours(2);

        assertThatThrownBy(() -> auctionService.create(new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Lot invalid",
                "Mismatch category",
                "RARE_BOOKS",
                "MILITARY_MAPS",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                null,
                null,
                endTime,
                30,
                30,
                List.of()
        ), 1L))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("Unsupported subcategory");
    }

    @Test
    void createRejectsReservePriceBelowOpeningPrice() {
        OffsetDateTime endTime = OffsetDateTime.now().plusHours(2);

        assertThatThrownBy(() -> auctionService.create(new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Lot invalid reserve",
                "Reserve below opening price",
                "RARE_BOOKS",
                "SIGNED_COPIES",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("90.00"),
                null,
                endTime,
                30,
                30,
                List.of()
        ), 1L))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("Reserve price must be greater than or equal to the opening price");
    }

    @Test
    void updateDraftAuctionReusesFormFieldsAndAppendsNewImages() {
        Long auctionId = 44L;
        OffsetDateTime endTime = OffsetDateTime.now().plusHours(6);

        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setCreatedAt(OffsetDateTime.now().minusDays(1));
        auction.setCreatedBy(5L);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(auctionImageRepository.findByAuctionIdOrderByDisplayOrderAsc(auctionId)).thenReturn(List.of(existingImage(1L, auctionId, 0)));

        AuctionResponse response = auctionService.update(auctionId, new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Updated lot",
                "Updated description",
                "RARE_BOOKS",
                "SIGNED_COPIES",
                "Updated author",
                1910,
                "Romanian",
                "GOOD",
                "VERIFIED",
                "Updated provenance",
                new BigDecimal("250.00"),
                new BigDecimal("15.00"),
                null,
                null,
                endTime,
                120,
                45,
                List.of("https://img.test/new-image.jpg")
        ), 5L);

        verify(auctionImageRepository).saveAll(auctionImagesCaptor.capture());
        assertThat(auctionImagesCaptor.getValue()).hasSize(1);
        assertThat(auctionImagesCaptor.getValue().get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(auction.getTitle()).isEqualTo("Updated lot");
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo("250.00");
        assertThat(response.status()).isEqualTo(AuctionStatus.DRAFT);
    }

    @Test
    void updateRejectsNonDraftAuction() {
        Long auctionId = 45L;
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setStatus(AuctionStatus.RUNNING);
        auction.setCreatedBy(1L);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.update(auctionId, new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Invalid update",
                null,
                "RARE_BOOKS",
                "SIGNED_COPIES",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("5.00"),
                null,
                null,
                OffsetDateTime.now().plusHours(1),
                30,
                30,
                List.of()
        ), 1L))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("Only DRAFT auctions can be edited");
    }

    @Test
    void watchAuctionAddsEntryAndReturnsUpdatedWatchState() {
        Long auctionId = 61L;
        Long userId = 2L;
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setStatus(AuctionStatus.RUNNING);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionWatchlistRepository.existsByUserIdAndAuctionId(userId, auctionId)).thenReturn(false, true);
        when(auctionWatchlistRepository.findWatcherCountsByAuctionIds(List.of(auctionId))).thenReturn(List.of(countView(auctionId, 1L)));

        AuctionResponse response = auctionService.watchAuction(auctionId, userId);

        verify(auctionWatchlistRepository).save(any(AuctionWatchlistEntity.class));
        assertThat(response.watchersCount()).isEqualTo(1L);
        assertThat(response.watchedByCurrentUser()).isTrue();
    }

    @Test
    void watchAuctionRejectsOwnAuction() {
        Long auctionId = 611L;
        Long userId = 2L;
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setCreatedBy(userId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        assertThatThrownBy(() -> auctionService.watchAuction(auctionId, userId))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("You cannot watch your own auction");
    }

    @Test
    void unwatchAuctionRemovesEntryAndReturnsUpdatedWatchState() {
        Long auctionId = 62L;
        Long userId = 2L;
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setStatus(AuctionStatus.RUNNING);
        AuctionWatchlistEntity watch = new AuctionWatchlistEntity();
        watch.setId(10L);
        watch.setAuctionId(auctionId);
        watch.setUserId(userId);

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionWatchlistRepository.findByUserIdAndAuctionId(userId, auctionId)).thenReturn(Optional.of(watch));
        when(auctionWatchlistRepository.findWatcherCountsByAuctionIds(List.of(auctionId))).thenReturn(List.of());

        AuctionResponse response = auctionService.unwatchAuction(auctionId, userId);

        verify(auctionWatchlistRepository).delete(watch);
        assertThat(response.watchersCount()).isEqualTo(0L);
        assertThat(response.watchedByCurrentUser()).isFalse();
    }

    @Test
    void findCreatedByUserReturnsCreatorAuctionsWithWatchState() {
        Long userId = 2L;
        Long createdAuctionId = 70L;
        AuctionEntity createdAuction = new AuctionEntity();
        createdAuction.setId(createdAuctionId);
        createdAuction.setCreatedBy(userId);
        createdAuction.setStatus(AuctionStatus.DRAFT);
        createdAuction.setTitle("Seller draft");

        when(auctionRepository.findByCreatedByOrderByCreatedAtDesc(userId)).thenReturn(List.of(createdAuction));
        when(auctionWatchlistRepository.findWatcherCountsByAuctionIds(List.of(createdAuctionId)))
                .thenReturn(List.of(countView(createdAuctionId, 3L)));
        when(auctionWatchlistRepository.findWatchedAuctionIdsByUserIdAndAuctionIds(userId, List.of(createdAuctionId)))
                .thenReturn(List.of(createdAuctionId));

        List<AuctionResponse> response = auctionService.findCreatedByUser(userId);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(createdAuctionId);
        assertThat(response.get(0).watchersCount()).isEqualTo(3L);
        assertThat(response.get(0).watchedByCurrentUser()).isTrue();
    }

    @Test
    void findBiddingActivityAggregatesLatestAndHighestBidPerAuction() {
        Long userId = 2L;
        Long auctionId = 80L;

        BidEntity latestBid = new BidEntity();
        latestBid.setId(201L);
        latestBid.setAuctionId(auctionId);
        latestBid.setBidderId(userId);
        latestBid.setAmount(new BigDecimal("145.00"));
        latestBid.setCreatedAt(OffsetDateTime.now().minusMinutes(1));

        BidEntity olderHigherBid = new BidEntity();
        olderHigherBid.setId(200L);
        olderHigherBid.setAuctionId(auctionId);
        olderHigherBid.setBidderId(userId);
        olderHigherBid.setAmount(new BigDecimal("150.00"));
        olderHigherBid.setCreatedAt(OffsetDateTime.now().minusMinutes(10));

        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusHours(2), 30, 30);
        auction.setTitle("Tracked bid lot");

        BidEntity topBid = new BidEntity();
        topBid.setId(202L);
        topBid.setAuctionId(auctionId);
        topBid.setBidderId(userId);
        topBid.setAmount(new BigDecimal("150.00"));

        when(bidRepository.findByBidderIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(latestBid, olderHigherBid));
        when(auctionRepository.findAllById(List.of(auctionId))).thenReturn(List.of(auction));
        when(auctionWatchlistRepository.findWatcherCountsByAuctionIds(List.of(auctionId)))
                .thenReturn(List.of(countView(auctionId, 4L)));
        when(auctionWatchlistRepository.findWatchedAuctionIdsByUserIdAndAuctionIds(userId, List.of(auctionId)))
                .thenReturn(List.of());
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId))
                .thenReturn(Optional.of(topBid));

        List<MyBidAuctionResponse> response = auctionService.findBiddingActivity(userId);

        assertThat(response).hasSize(1);
        MyBidAuctionResponse myBid = response.get(0);
        assertThat(myBid.auction().id()).isEqualTo(auctionId);
        assertThat(myBid.totalBids()).isEqualTo(2L);
        assertThat(myBid.latestBidAmount()).isEqualByComparingTo("145.00");
        assertThat(myBid.highestBidAmount()).isEqualByComparingTo("150.00");
        assertThat(myBid.leading()).isTrue();
        assertThat(myBid.won()).isFalse();
    }

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
                new PlaceBidRequest(new BigDecimal("125.00")),
                200L
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
    void placeBidRejectsWhenUserIsAlreadyHighestBidder() {
        Long auctionId = 101L;
        Long userId = 200L;

        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(5), 15, 30);
        BidEntity topBid = new BidEntity();
        topBid.setId(501L);
        topBid.setAuctionId(auctionId);
        topBid.setBidderId(userId);
        topBid.setAmount(new BigDecimal("120.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId)).thenReturn(Optional.of(topBid));

        assertThatThrownBy(() -> auctionService.placeBid(
                auctionId,
                new PlaceBidRequest(new BigDecimal("125.00")),
                userId
        ))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("highest bidder");
    }

    @Test
    void startPublishesStartedEventToOutboxAndRealtimeChannel() {
        Long auctionId = 15L;
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setCreatedBy(1L);
        auction.setEndTime(OffsetDateTime.now().plusHours(1));

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.start(auctionId, 1L);

        assertThat(response.status()).isEqualTo(AuctionStatus.RUNNING);
        assertThat(response.startTime()).isNotNull();

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getValue()).isInstanceOf(AuctionStartedEvent.class);

        verify(auctionEventBroadcaster).broadcastToAuction(any(), realtimePayloadCaptor.capture());
        AuctionRealtimeEvent<?> realtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getValue();
        assertThat(realtimeEvent.type()).isEqualTo(AuctionEventType.AUCTION_STARTED.name());
        assertThat(realtimeEvent.payload()).isInstanceOf(AuctionStartedEvent.class);
    }

    @Test
    void closePublishesClosedEventToOutboxAndRealtimeChannel() {
        Long auctionId = 20L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(1), 30, 30);
        auction.setCurrentPrice(new BigDecimal("310.00"));
        auction.setCreatedBy(1L);
        BidEntity winningBid = new BidEntity();
        winningBid.setId(77L);
        winningBid.setAuctionId(auctionId);
        winningBid.setBidderId(901L);
        winningBid.setAmount(new BigDecimal("310.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId))
                .thenReturn(Optional.of(winningBid));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.close(auctionId, 1L);

        assertThat(response.status()).isEqualTo(AuctionStatus.ENDED);
        assertThat(response.winnerId()).isEqualTo(901L);
        assertThat(response.winningBidId()).isEqualTo(77L);
        assertThat(response.finalPrice()).isEqualByComparingTo("310.00");
        assertThat(response.closedReason()).isEqualTo(AuctionCloseReason.MANUAL);
        assertThat(response.reserveMet()).isNull();

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getValue()).isInstanceOf(AuctionClosedEvent.class);
        AuctionClosedEvent closedEvent = (AuctionClosedEvent) outboxPayloadCaptor.getValue();
        assertThat(closedEvent.winnerId()).isEqualTo(901L);
        assertThat(closedEvent.winningBidId()).isEqualTo(77L);
        assertThat(closedEvent.finalPrice()).isEqualByComparingTo("310.00");
        assertThat(closedEvent.reserveMet()).isNull();
        assertThat(closedEvent.closedReason()).isEqualTo(AuctionCloseReason.MANUAL);

        verify(auctionEventBroadcaster).broadcastToAuction(any(), realtimePayloadCaptor.capture());
        AuctionRealtimeEvent<?> realtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getValue();
        assertThat(realtimeEvent.type()).isEqualTo(AuctionEventType.AUCTION_CLOSED.name());
        assertThat(realtimeEvent.payload()).isInstanceOf(AuctionClosedEvent.class);
    }

    @Test
    void closeExpiredAuctionPublishesClosedEventForExpiredRunningAuction() {
        Long auctionId = 30L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().minusSeconds(5), 30, 30);
        auction.setCurrentPrice(new BigDecimal("415.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId))
                .thenReturn(Optional.empty());
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.closeExpiredAuction(auctionId);

        assertThat(response.status()).isEqualTo(AuctionStatus.ENDED);
        assertThat(response.winnerId()).isNull();
        assertThat(response.finalPrice()).isEqualByComparingTo("415.00");
        assertThat(response.closedReason()).isEqualTo(AuctionCloseReason.EXPIRED);
        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getValue()).isInstanceOf(AuctionClosedEvent.class);
        verify(auctionEventBroadcaster).broadcastToAuction(any(), any(AuctionRealtimeEvent.class));
    }

    @Test
    void closeDoesNotAssignWinnerWhenReservePriceWasNotMet() {
        Long auctionId = 32L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(1), 30, 30);
        auction.setCurrentPrice(new BigDecimal("220.00"));
        auction.setReservePrice(new BigDecimal("300.00"));
        auction.setReserveMet(false);
        auction.setCreatedBy(1L);

        BidEntity topBid = new BidEntity();
        topBid.setId(88L);
        topBid.setAuctionId(auctionId);
        topBid.setBidderId(901L);
        topBid.setAmount(new BigDecimal("220.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId))
                .thenReturn(Optional.of(topBid));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.close(auctionId, 1L);

        assertThat(response.status()).isEqualTo(AuctionStatus.ENDED);
        assertThat(response.winnerId()).isNull();
        assertThat(response.winningBidId()).isNull();
        assertThat(response.finalPrice()).isEqualByComparingTo("220.00");
        assertThat(response.reserveMet()).isFalse();

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        AuctionClosedEvent closedEvent = (AuctionClosedEvent) outboxPayloadCaptor.getValue();
        assertThat(closedEvent.winnerId()).isNull();
        assertThat(closedEvent.winningBidId()).isNull();
        assertThat(closedEvent.reserveMet()).isFalse();
    }

    @Test
    void createRejectsBuyNowPriceBelowOrEqualOpeningPrice() {
        OffsetDateTime endTime = OffsetDateTime.now().plusHours(2);

        assertThatThrownBy(() -> auctionService.create(new org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest(
                "Lot invalid buy now",
                "Buy now below opening price",
                "RARE_BOOKS",
                "SIGNED_COPIES",
                null,
                null,
                null,
                null,
                null,
                null,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                null,
                new BigDecimal("100.00"),
                endTime,
                30,
                30,
                List.of()
        ), 1L))
                .isInstanceOf(org.nedelcu.cosmin.auction.api.common.exception.BusinessException.class)
                .hasMessageContaining("Buy Now price must be greater than the opening price");
    }

    @Test
    void buyNowClosesAuctionImmediatelyWithDedicatedReason() {
        Long auctionId = 90L;
        Long buyerId = 2L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(5), 30, 30);
        auction.setCreatedBy(1L);
        auction.setBuyNowPrice(new BigDecimal("500.00"));
        auction.setReservePrice(new BigDecimal("300.00"));
        auction.setReserveMet(false);
        auction.setCurrentPrice(new BigDecimal("220.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.buyNow(auctionId, buyerId);

        assertThat(response.status()).isEqualTo(AuctionStatus.ENDED);
        assertThat(response.winnerId()).isEqualTo(buyerId);
        assertThat(response.winningBidId()).isNull();
        assertThat(response.finalPrice()).isEqualByComparingTo("500.00");
        assertThat(response.closedReason()).isEqualTo(AuctionCloseReason.BUY_NOW);
        assertThat(response.reserveMet()).isTrue();

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        AuctionClosedEvent closedEvent = (AuctionClosedEvent) outboxPayloadCaptor.getValue();
        assertThat(closedEvent.winnerId()).isEqualTo(buyerId);
        assertThat(closedEvent.winningBidId()).isNull();
        assertThat(closedEvent.finalPrice()).isEqualByComparingTo("500.00");
        assertThat(closedEvent.closedReason()).isEqualTo(AuctionCloseReason.BUY_NOW);
        assertThat(closedEvent.reserveMet()).isTrue();
    }

    @Test
    void suspendMarksRunningAuctionAsSuspendedAndPublishesDedicatedEvent() {
        Long auctionId = 91L;
        Long adminId = 99L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(5), 30, 30);
        auction.setCreatedBy(1L);
        auction.setCurrentPrice(new BigDecimal("220.00"));

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(AuctionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuctionResponse response = auctionService.suspend(auctionId, "Suspicious bidding pattern detected.", adminId);

        assertThat(response.status()).isEqualTo(AuctionStatus.SUSPENDED);
        assertThat(response.winnerId()).isNull();
        assertThat(response.finalPrice()).isNull();
        assertThat(response.suspendedBy()).isEqualTo(adminId);
        assertThat(response.suspensionReason()).isEqualTo("Suspicious bidding pattern detected.");

        verify(outboxService).saveEvent(any(), any(), any(), outboxPayloadCaptor.capture());
        assertThat(outboxPayloadCaptor.getValue()).isInstanceOf(AuctionSuspendedEvent.class);
        AuctionSuspendedEvent suspendedEvent = (AuctionSuspendedEvent) outboxPayloadCaptor.getValue();
        assertThat(suspendedEvent.suspendedBy()).isEqualTo(adminId);
        assertThat(suspendedEvent.reason()).isEqualTo("Suspicious bidding pattern detected.");

        verify(auctionEventBroadcaster).broadcastToAuction(any(), realtimePayloadCaptor.capture());
        AuctionRealtimeEvent<?> realtimeEvent = (AuctionRealtimeEvent<?>) realtimePayloadCaptor.getValue();
        assertThat(realtimeEvent.type()).isEqualTo(AuctionEventType.AUCTION_SUSPENDED.name());
        assertThat(realtimeEvent.payload()).isInstanceOf(AuctionSuspendedEvent.class);
    }

    @Test
    void closeExpiredAuctionReturnsRunningAuctionWhenItIsNotYetExpired() {
        Long auctionId = 31L;
        AuctionEntity auction = runningAuction(auctionId, OffsetDateTime.now().plusMinutes(2), 30, 30);

        when(auctionRepository.findByIdForUpdate(auctionId)).thenReturn(Optional.of(auction));

        AuctionResponse response = auctionService.closeExpiredAuction(auctionId);

        assertThat(response.status()).isEqualTo(AuctionStatus.RUNNING);
        verify(auctionRepository, times(0)).save(any(AuctionEntity.class));
        verify(outboxService, times(0)).saveEvent(any(), any(), any(), any());
        verify(auctionEventBroadcaster, times(0)).broadcastToAuction(any(), any());
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

    private AuctionImageEntity existingImage(Long id, Long auctionId, int displayOrder) {
        AuctionImageEntity image = new AuctionImageEntity();
        image.setId(id);
        image.setAuctionId(auctionId);
        image.setImageUrl("https://img.test/existing.jpg");
        image.setDisplayOrder(displayOrder);
        return image;
    }

    private AuctionWatchlistRepository.AuctionWatchlistCountView countView(Long auctionId, long count) {
        return new AuctionWatchlistRepository.AuctionWatchlistCountView() {
            @Override
            public Long getAuctionId() {
                return auctionId;
            }

            @Override
            public long getWatcherCount() {
                return count;
            }
        };
    }
}
