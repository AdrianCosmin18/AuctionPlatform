export interface CategoryMetric {
  categoryCode: string;
  count: number;
}

export interface AnalyticsDashboard {
  totalAuctions: number;
  activeAuctions: number;
  endedAuctions: number;
  successfulClosures: number;
  closeRatePercent: number;
  averageFinalPrice: number;
  totalWatchlistEntries: number;
  bidsPerCategory: CategoryMetric[];
  watchlistByCategory: CategoryMetric[];
}
