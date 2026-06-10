package org.nedelcu.cosmin.auction.api.fraud.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudOverviewResponse;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudSeverity;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudSignalResponse;
import org.nedelcu.cosmin.auction.api.fraud.dto.FraudSignalType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {
    private static final Duration BURST_WINDOW = Duration.ofMinutes(5);
    private static final int BURST_MIN_BIDS = 3;
    private static final int CONCENTRATION_MIN_BIDS = 5;

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    public FraudOverviewResponse getSignals() {
        List<BidEntity> bids = bidRepository.findAllByOrderByCreatedAtAsc();
        Map<Long, AuctionEntity> auctionsById = auctionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(AuctionEntity::getId, auction -> auction));

        List<FraudSignalResponse> signals = new ArrayList<>();
        signals.addAll(detectBurstBidding(bids, auctionsById));
        signals.addAll(detectSellerBidderConcentration(bids, auctionsById));

        List<FraudSignalResponse> sortedSignals = signals.stream()
                .sorted(Comparator
                        .comparing(FraudSignalResponse::severity, Comparator.reverseOrder())
                        .thenComparing(FraudSignalResponse::lastSeenAt, Comparator.reverseOrder()))
                .toList();

        long high = sortedSignals.stream().filter(signal -> signal.severity() == FraudSeverity.HIGH).count();
        long medium = sortedSignals.stream().filter(signal -> signal.severity() == FraudSeverity.MEDIUM).count();
        long low = sortedSignals.stream().filter(signal -> signal.severity() == FraudSeverity.LOW).count();

        return new FraudOverviewResponse(sortedSignals.size(), high, medium, low, sortedSignals);
    }

    private List<FraudSignalResponse> detectBurstBidding(List<BidEntity> bids, Map<Long, AuctionEntity> auctionsById) {
        Map<Long, List<BidEntity>> bidsByAuction = new HashMap<>();

        for (BidEntity bid : bids) {
            bidsByAuction
                    .computeIfAbsent(bid.getAuctionId(), key -> new ArrayList<>())
                    .add(bid);
        }

        List<FraudSignalResponse> signals = new ArrayList<>();

        for (Map.Entry<Long, List<BidEntity>> entry : bidsByAuction.entrySet()) {
            List<BidEntity> auctionBids = entry.getValue();
            BurstWindow strongestWindow = findStrongestBurstWindow(auctionBids);

            if (strongestWindow == null || strongestWindow.bidCount < BURST_MIN_BIDS) {
                continue;
            }

            BidEntity anchorBid = auctionBids.get(strongestWindow.endIndex);
            AuctionEntity auction = auctionsById.get(anchorBid.getAuctionId());
            FraudSeverity severity = strongestWindow.bidCount >= 4
                    ? FraudSeverity.HIGH
                    : FraudSeverity.MEDIUM;

            signals.add(new FraudSignalResponse(
                    FraudSignalType.BURST_BIDDING,
                    severity,
                    anchorBid.getAuctionId(),
                    auction != null ? auction.getCreatedBy() : null,
                    anchorBid.getBidderId(),
                    strongestWindow.bidCount,
                    1,
                    (int) BURST_WINDOW.toSeconds(),
                    "Burst bidding pattern detected",
                    "Bidder #%d placed %d bids on auction #%d within %d minutes."
                            .formatted(
                                    anchorBid.getBidderId(),
                                    strongestWindow.bidCount,
                                    anchorBid.getAuctionId(),
                                    BURST_WINDOW.toMinutes()
                            ),
                    auctionBids.get(strongestWindow.startIndex).getCreatedAt(),
                    auctionBids.get(strongestWindow.endIndex).getCreatedAt()
            ));
        }

        return signals;
    }

    private BurstWindow findStrongestBurstWindow(List<BidEntity> bids) {
        BurstWindow strongest = null;
        int runStart = 0;

        for (int index = 0; index < bids.size(); index++) {
            if (index == 0) {
                continue;
            }

            BidEntity currentBid = bids.get(index);
            BidEntity previousBid = bids.get(index - 1);

            boolean sameBidder = previousBid.getBidderId().equals(currentBid.getBidderId());
            boolean withinWindow = Duration.between(bids.get(runStart).getCreatedAt(), currentBid.getCreatedAt()).compareTo(BURST_WINDOW) <= 0;

            if (!sameBidder || !withinWindow) {
                runStart = index;
                continue;
            }

            int count = index - runStart + 1;

            if (strongest == null || count > strongest.bidCount) {
                strongest = new BurstWindow(runStart, index, count);
            }
        }

        return strongest;
    }

    private List<FraudSignalResponse> detectSellerBidderConcentration(List<BidEntity> bids, Map<Long, AuctionEntity> auctionsById) {
        Map<Long, Integer> totalBidsByBidder = new HashMap<>();
        Map<String, PairMetrics> metricsByPair = new HashMap<>();

        for (BidEntity bid : bids) {
            AuctionEntity auction = auctionsById.get(bid.getAuctionId());

            if (auction == null || auction.getCreatedBy() == null) {
                continue;
            }

            totalBidsByBidder.merge(bid.getBidderId(), 1, Integer::sum);
            String key = auction.getCreatedBy() + ":" + bid.getBidderId();
            PairMetrics metrics = metricsByPair.computeIfAbsent(key, ignored -> new PairMetrics(auction.getCreatedBy(), bid.getBidderId()));
            metrics.bidCount++;
            metrics.auctionIds.add(bid.getAuctionId());

            if (metrics.firstSeenAt == null || bid.getCreatedAt().isBefore(metrics.firstSeenAt)) {
                metrics.firstSeenAt = bid.getCreatedAt();
            }

            if (metrics.lastSeenAt == null || bid.getCreatedAt().isAfter(metrics.lastSeenAt)) {
                metrics.lastSeenAt = bid.getCreatedAt();
            }
        }

        List<FraudSignalResponse> signals = new ArrayList<>();

        for (PairMetrics metrics : metricsByPair.values()) {
            int totalBidderBids = totalBidsByBidder.getOrDefault(metrics.bidderId, 0);

            if (metrics.bidCount < CONCENTRATION_MIN_BIDS || totalBidderBids == 0) {
                continue;
            }

            double share = (double) metrics.bidCount / totalBidderBids;
            int relatedAuctions = metrics.auctionIds.size();

            if (share < 0.75 || relatedAuctions < 2) {
                continue;
            }

            FraudSeverity severity = share >= 0.85 && metrics.bidCount >= 8 && relatedAuctions >= 3
                    ? FraudSeverity.HIGH
                    : share >= 0.80 && metrics.bidCount >= 6 ? FraudSeverity.MEDIUM : FraudSeverity.LOW;

            signals.add(new FraudSignalResponse(
                    FraudSignalType.SELLER_BIDDER_CONCENTRATION,
                    severity,
                    null,
                    metrics.sellerId,
                    metrics.bidderId,
                    metrics.bidCount,
                    relatedAuctions,
                    0,
                    "Seller-bidder concentration detected",
                    "Bidder #%d placed %d of %d total bids on %d auctions from seller #%d."
                            .formatted(
                                    metrics.bidderId,
                                    metrics.bidCount,
                                    totalBidderBids,
                                    relatedAuctions,
                                    metrics.sellerId
                            ),
                    metrics.firstSeenAt,
                    metrics.lastSeenAt
            ));
        }

        return signals;
    }

    private record BurstWindow(int startIndex, int endIndex, int bidCount) {
    }

    private static final class PairMetrics {
        private final Long sellerId;
        private final Long bidderId;
        private final java.util.Set<Long> auctionIds = new java.util.HashSet<>();
        private int bidCount;
        private OffsetDateTime firstSeenAt;
        private OffsetDateTime lastSeenAt;

        private PairMetrics(Long sellerId, Long bidderId) {
            this.sellerId = sellerId;
            this.bidderId = bidderId;
        }
    }
}
