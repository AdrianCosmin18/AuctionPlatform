package org.nedelcu.cosmin.auction.api.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.analytics.dto.AnalyticsDashboardResponse;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionWatchlistRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionWatchlistRepository auctionWatchlistRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getDashboardAggregatesMetricsAndCategoryBreakdowns() {
        when(auctionRepository.count()).thenReturn(12L);
        when(auctionRepository.countByStatus(AuctionStatus.RUNNING)).thenReturn(4L);
        when(auctionRepository.countByStatus(AuctionStatus.ENDED)).thenReturn(6L);
        when(auctionRepository.countClosedWithWinnerByStatus(AuctionStatus.ENDED)).thenReturn(3L);
        when(auctionRepository.findFinalPricesForSuccessfulClosures(AuctionStatus.ENDED))
                .thenReturn(List.of(new BigDecimal("100.00"), new BigDecimal("250.00"), new BigDecimal("400.00")));
        when(auctionWatchlistRepository.count()).thenReturn(15L);
        when(bidRepository.countBidsByCategory()).thenReturn(List.of(metric("RARE_BOOKS", 8L), metric(null, 2L)));
        when(auctionWatchlistRepository.countWatchlistEntriesByCategory())
                .thenReturn(List.of(watchMetric("RARE_BOOKS", 5L), watchMetric("MAPS", 3L)));

        AnalyticsDashboardResponse response = analyticsService.getDashboard();

        assertThat(response.totalAuctions()).isEqualTo(12L);
        assertThat(response.activeAuctions()).isEqualTo(4L);
        assertThat(response.endedAuctions()).isEqualTo(6L);
        assertThat(response.successfulClosures()).isEqualTo(3L);
        assertThat(response.closeRatePercent()).isEqualByComparingTo("50.00");
        assertThat(response.averageFinalPrice()).isEqualByComparingTo("250.00");
        assertThat(response.totalWatchlistEntries()).isEqualTo(15L);
        assertThat(response.bidsPerCategory()).hasSize(2);
        assertThat(response.bidsPerCategory().get(1).categoryCode()).isEqualTo("UNCATEGORIZED");
        assertThat(response.watchlistByCategory()).hasSize(2);
    }

    @Test
    void getDashboardReturnsZeroRatesWhenNoEndedAuctionsExist() {
        when(auctionRepository.count()).thenReturn(0L);
        when(auctionRepository.countByStatus(AuctionStatus.RUNNING)).thenReturn(0L);
        when(auctionRepository.countByStatus(AuctionStatus.ENDED)).thenReturn(0L);
        when(auctionRepository.countClosedWithWinnerByStatus(AuctionStatus.ENDED)).thenReturn(0L);
        when(auctionRepository.findFinalPricesForSuccessfulClosures(AuctionStatus.ENDED)).thenReturn(List.of());
        when(auctionWatchlistRepository.count()).thenReturn(0L);
        when(bidRepository.countBidsByCategory()).thenReturn(List.of());
        when(auctionWatchlistRepository.countWatchlistEntriesByCategory()).thenReturn(List.of());

        AnalyticsDashboardResponse response = analyticsService.getDashboard();

        assertThat(response.closeRatePercent()).isEqualByComparingTo("0.00");
        assertThat(response.averageFinalPrice()).isEqualByComparingTo("0.00");
        assertThat(response.bidsPerCategory()).isEmpty();
        assertThat(response.watchlistByCategory()).isEmpty();
    }

    private BidRepository.CategoryMetricView metric(String categoryCode, long count) {
        return new BidRepository.CategoryMetricView() {
            @Override
            public String getCategoryCode() {
                return categoryCode;
            }

            @Override
            public long getMetricCount() {
                return count;
            }
        };
    }

    private AuctionWatchlistRepository.CategoryMetricView watchMetric(String categoryCode, long count) {
        return new AuctionWatchlistRepository.CategoryMetricView() {
            @Override
            public String getCategoryCode() {
                return categoryCode;
            }

            @Override
            public long getMetricCount() {
                return count;
            }
        };
    }
}
