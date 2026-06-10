package org.nedelcu.cosmin.auction.api.analytics.dto;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsDashboardResponse(
        long totalAuctions,
        long activeAuctions,
        long endedAuctions,
        long successfulClosures,
        BigDecimal closeRatePercent,
        BigDecimal averageFinalPrice,
        long totalWatchlistEntries,
        List<CategoryMetricResponse> bidsPerCategory,
        List<CategoryMetricResponse> watchlistByCategory
) {
}
