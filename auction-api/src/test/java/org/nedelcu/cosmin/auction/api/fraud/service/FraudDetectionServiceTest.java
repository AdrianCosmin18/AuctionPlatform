package org.nedelcu.cosmin.auction.api.fraud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudOverviewResponse;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudSeverity;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudSignalType;

@ExtendWith(MockitoExtension.class)
class FraudDetectionServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @InjectMocks
    private FraudDetectionService fraudDetectionService;

    @Test
    void getSignalsDetectsBurstBiddingAndConcentrationPatterns() {
        OffsetDateTime now = OffsetDateTime.now();

        AuctionEntity auction10 = auction(10L, 1L);
        AuctionEntity auction11 = auction(11L, 1L);
        AuctionEntity auction12 = auction(12L, 1L);

        List<BidEntity> bids = List.of(
                bid(1L, 10L, 3L, now.minusMinutes(5)),
                bid(2L, 10L, 2L, now.minusMinutes(4).minusSeconds(30)),
                bid(3L, 10L, 2L, now.minusMinutes(4)),
                bid(4L, 10L, 2L, now.minusMinutes(3).minusSeconds(30)),
                bid(5L, 11L, 2L, now.minusMinutes(3)),
                bid(6L, 11L, 2L, now.minusMinutes(2).minusSeconds(30)),
                bid(7L, 12L, 2L, now.minusMinutes(2)),
                bid(8L, 12L, 2L, now.minusMinutes(1).minusSeconds(30)),
                bid(9L, 12L, 3L, now.minusMinutes(1))
        );

        when(bidRepository.findAllByOrderByCreatedAtAsc()).thenReturn(bids);
        when(auctionRepository.findAll()).thenReturn(List.of(auction10, auction11, auction12));

        FraudOverviewResponse response = fraudDetectionService.getSignals();

        assertThat(response.totalSignals()).isEqualTo(2);
        assertThat(response.highSeveritySignals()).isEqualTo(0);
        assertThat(response.mediumSeveritySignals()).isEqualTo(2);
        assertThat(response.lowSeveritySignals()).isEqualTo(0);

        assertThat(response.signals())
                .extracting(signal -> signal.type())
                .containsExactlyInAnyOrder(FraudSignalType.BURST_BIDDING, FraudSignalType.SELLER_BIDDER_CONCENTRATION);
    }

    @Test
    void getSignalsDoesNotFlagAlternatingBidsAsBurstBidding() {
        AuctionEntity auction = auction(30L, 9L);
        OffsetDateTime now = OffsetDateTime.now();

        List<BidEntity> bids = List.of(
                bid(30L, 30L, 2L, now.minusMinutes(4)),
                bid(31L, 30L, 3L, now.minusMinutes(3).minusSeconds(30)),
                bid(32L, 30L, 2L, now.minusMinutes(3)),
                bid(33L, 30L, 4L, now.minusMinutes(2).minusSeconds(30)),
                bid(34L, 30L, 2L, now.minusMinutes(2))
        );

        when(bidRepository.findAllByOrderByCreatedAtAsc()).thenReturn(bids);
        when(auctionRepository.findAll()).thenReturn(List.of(auction));

        FraudOverviewResponse response = fraudDetectionService.getSignals();

        assertThat(response.signals())
                .extracting(signal -> signal.type())
                .doesNotContain(FraudSignalType.BURST_BIDDING);
    }

    @Test
    void getSignalsReturnsEmptyOverviewWhenNoThresholdsAreMet() {
        AuctionEntity auction = auction(20L, 7L);
        List<BidEntity> bids = List.of(
                bid(20L, 20L, 4L, OffsetDateTime.now().minusHours(2)),
                bid(21L, 20L, 5L, OffsetDateTime.now().minusHours(1))
        );

        when(bidRepository.findAllByOrderByCreatedAtAsc()).thenReturn(bids);
        when(auctionRepository.findAll()).thenReturn(List.of(auction));

        FraudOverviewResponse response = fraudDetectionService.getSignals();

        assertThat(response.totalSignals()).isZero();
        assertThat(response.signals()).isEmpty();
    }

    private AuctionEntity auction(Long auctionId, Long sellerId) {
        AuctionEntity auction = new AuctionEntity();
        auction.setId(auctionId);
        auction.setCreatedBy(sellerId);
        return auction;
    }

    private BidEntity bid(Long id, Long auctionId, Long bidderId, OffsetDateTime createdAt) {
        BidEntity bid = new BidEntity();
        bid.setId(id);
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(BigDecimal.TEN);
        bid.setCreatedAt(createdAt);
        return bid;
    }
}
