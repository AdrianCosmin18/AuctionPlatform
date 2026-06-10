package org.nedelcu.cosmin.auction.worker.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.nedelcu.cosmin.auction.worker.auction.AuctionEntity;
import org.nedelcu.cosmin.auction.worker.auction.AuctionRepository;
import org.nedelcu.cosmin.auction.worker.bid.BidRepository;
import org.nedelcu.cosmin.auction.worker.watchlist.AuctionWatchlistRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionWatchlistRepository auctionWatchlistRepository;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void handleBidPlacedCreatesSellerAndOutbidNotifications() {
        AuctionEntity auction = new AuctionEntity();
        auction.setId(10L);
        auction.setCreatedBy(1L);

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(10L)).thenReturn(List.of(2L, 3L, 4L));

        notificationService.handleBidPlaced(new BidPlacedEvent(
                10L,
                20L,
                2L,
                new BigDecimal("125.00"),
                new BigDecimal("125.00"),
                OffsetDateTime.now()
        ));

        verify(notificationRepository, times(3)).save(any(NotificationEntity.class));
        verify(emailNotificationService, times(3)).deliver(any(NotificationEntity.class));
    }

    @Test
    void handleAuctionClosedCreatesWinnerLoserSellerAndWatcherNotifications() {
        AuctionEntity auction = new AuctionEntity();
        auction.setId(15L);
        auction.setCreatedBy(1L);

        when(auctionRepository.findById(15L)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(15L)).thenReturn(List.of(2L, 3L));
        when(auctionWatchlistRepository.findDistinctUserIdsByAuctionId(15L)).thenReturn(List.of(2L, 3L, 4L, 5L));

        notificationService.handleAuctionClosed(new AuctionClosedEvent(
                15L,
                2L,
                99L,
                new BigDecimal("300.00"),
                true,
                AuctionCloseReason.MANUAL,
                OffsetDateTime.now()
        ));

        verify(notificationRepository, times(5)).save(any(NotificationEntity.class));
        verify(emailNotificationService, times(5)).deliver(any(NotificationEntity.class));
    }

    @Test
    void handleAuctionClosedWithoutReserveMetCreatesSellerLoserAndWatcherNotifications() {
        AuctionEntity auction = new AuctionEntity();
        auction.setId(16L);
        auction.setCreatedBy(1L);

        when(auctionRepository.findById(16L)).thenReturn(Optional.of(auction));
        when(bidRepository.findDistinctBidderIdsByAuctionId(16L)).thenReturn(List.of(2L));
        when(auctionWatchlistRepository.findDistinctUserIdsByAuctionId(16L)).thenReturn(List.of(2L, 3L));

        notificationService.handleAuctionClosed(new AuctionClosedEvent(
                16L,
                null,
                null,
                new BigDecimal("220.00"),
                false,
                AuctionCloseReason.EXPIRED,
                OffsetDateTime.now()
        ));

        verify(notificationRepository, times(3)).save(any(NotificationEntity.class));
        verify(emailNotificationService, times(3)).deliver(any(NotificationEntity.class));
    }
}
