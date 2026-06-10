package org.nedelcu.cosmin.auction.api.analytics.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.analytics.dto.AnalyticsDashboardResponse;
import org.nedelcu.cosmin.auction.api.analytics.dto.CategoryMetricResponse;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionWatchlistRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private static final String UNCATEGORIZED = "UNCATEGORIZED";

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionWatchlistRepository auctionWatchlistRepository;

    public AnalyticsDashboardResponse getDashboard() {
        long totalAuctions = auctionRepository.count();
        long activeAuctions = auctionRepository.countByStatus(AuctionStatus.RUNNING);
        long endedAuctions = auctionRepository.countByStatus(AuctionStatus.ENDED);
        long successfulClosures = auctionRepository.countClosedWithWinnerByStatus(AuctionStatus.ENDED);
        BigDecimal closeRatePercent = calculateCloseRatePercent(successfulClosures, endedAuctions);
        BigDecimal averageFinalPrice = calculateAverage(auctionRepository.findFinalPricesForSuccessfulClosures(AuctionStatus.ENDED));
        long totalWatchlistEntries = auctionWatchlistRepository.count();

        return new AnalyticsDashboardResponse(
                totalAuctions,
                activeAuctions,
                endedAuctions,
                successfulClosures,
                closeRatePercent,
                averageFinalPrice,
                totalWatchlistEntries,
                bidRepository.countBidsByCategory().stream()
                        .map(metric -> new CategoryMetricResponse(normalizeCategoryCode(metric.getCategoryCode()), metric.getMetricCount()))
                        .toList(),
                auctionWatchlistRepository.countWatchlistEntriesByCategory().stream()
                        .map(metric -> new CategoryMetricResponse(normalizeCategoryCode(metric.getCategoryCode()), metric.getMetricCount()))
                        .toList()
        );
    }

    private BigDecimal calculateCloseRatePercent(long successfulClosures, long endedAuctions) {
        if (endedAuctions == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(successfulClosures)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(endedAuctions), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverage(List<BigDecimal> amounts) {
        if (amounts.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.divide(BigDecimal.valueOf(amounts.size()), 2, RoundingMode.HALF_UP);
    }

    private String normalizeCategoryCode(String categoryCode) {
        return categoryCode != null && !categoryCode.isBlank() ? categoryCode : UNCATEGORIZED;
    }
}
