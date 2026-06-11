package org.nedelcu.cosmin.auction.api.fraud.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private static final int BIDDER_PAIR_MIN_BIDS = 6;
    private static final double BIDDER_PAIR_MIN_SHARE = 0.80;
    private static final int BIDDER_PAIR_MIN_ALTERNATIONS = 4;
    private static final int CONCENTRATION_MIN_BIDS = 5;

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;

    public FraudOverviewResponse getSignals() {
        List<BidEntity> bids = bidRepository.findAllByOrderByCreatedAtAsc();
        Map<Long, AuctionEntity> auctionsById = auctionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(AuctionEntity::getId, auction -> auction));

        List<FraudSignalResponse> signals = new ArrayList<>();
        signals.addAll(detectBidderPairDominance(bids, auctionsById));
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

    private List<FraudSignalResponse> detectBidderPairDominance(List<BidEntity> bids, Map<Long, AuctionEntity> auctionsById) {
        Map<Long, List<BidEntity>> bidsByAuction = new HashMap<>();

        for (BidEntity bid : bids) {
            bidsByAuction
                    .computeIfAbsent(bid.getAuctionId(), key -> new ArrayList<>())
                    .add(bid);
        }

        List<FraudSignalResponse> signals = new ArrayList<>();

        for (Map.Entry<Long, List<BidEntity>> entry : bidsByAuction.entrySet()) {
            List<BidEntity> auctionBids = entry.getValue();

            if (auctionBids.size() < BIDDER_PAIR_MIN_BIDS) {
                continue;
            }

            Map<Long, Integer> countsByBidder = new HashMap<>();
            for (BidEntity bid : auctionBids) {
                countsByBidder.merge(bid.getBidderId(), 1, Integer::sum);
            }

            if (countsByBidder.size() < 2) {
                continue;
            }

            List<Map.Entry<Long, Integer>> topBidders = countsByBidder.entrySet().stream()
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(2)
                    .toList();

            long firstBidderId = topBidders.get(0).getKey();
            int firstBidderCount = topBidders.get(0).getValue();
            long secondBidderId = topBidders.get(1).getKey();
            int secondBidderCount = topBidders.get(1).getValue();
            int combinedBidCount = firstBidderCount + secondBidderCount;
            double share = (double) combinedBidCount / auctionBids.size();

            if (combinedBidCount < BIDDER_PAIR_MIN_BIDS || share < BIDDER_PAIR_MIN_SHARE) {
                continue;
            }

            int alternations = countAlternations(auctionBids, Set.of(firstBidderId, secondBidderId));
            if (alternations < BIDDER_PAIR_MIN_ALTERNATIONS) {
                continue;
            }

            AuctionEntity auction = auctionsById.get(entry.getKey());
            FraudSeverity severity = share >= 0.90 && alternations >= 6 && combinedBidCount >= 8
                    ? FraudSeverity.HIGH
                    : share >= 0.85 && alternations >= 5 ? FraudSeverity.MEDIUM : FraudSeverity.LOW;
            int windowSeconds = (int) Math.max(0, Duration.between(
                    auctionBids.get(0).getCreatedAt(),
                    auctionBids.get(auctionBids.size() - 1).getCreatedAt()
            ).toSeconds());

            signals.add(new FraudSignalResponse(
                    FraudSignalType.BIDDER_PAIR_DOMINANCE,
                    severity,
                    entry.getKey(),
                    auction != null ? auction.getStatus() : null,
                    auction != null ? auction.getCreatedBy() : null,
                    firstBidderId,
                    combinedBidCount,
                    1,
                    windowSeconds,
                    "Bidder pair dominance detected",
                    "Bidders #%d and #%d placed %d of %d bids on auction #%d, with %d alternations between them."
                            .formatted(
                                    firstBidderId,
                                    secondBidderId,
                                    combinedBidCount,
                                    auctionBids.size(),
                                    entry.getKey(),
                                    alternations
                            ),
                    auctionBids.get(0).getCreatedAt(),
                    auctionBids.get(auctionBids.size() - 1).getCreatedAt()
            ));
        }

        return signals;
    }

    private int countAlternations(List<BidEntity> bids, Set<Long> selectedBidderIds) {
        int alternations = 0;

        for (int index = 1; index < bids.size(); index++) {
            Long previousBidderId = bids.get(index - 1).getBidderId();
            Long currentBidderId = bids.get(index).getBidderId();

            if (selectedBidderIds.contains(previousBidderId)
                    && selectedBidderIds.contains(currentBidderId)
                    && !previousBidderId.equals(currentBidderId)) {
                alternations++;
            }
        }

        return alternations;
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
