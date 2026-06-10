import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { AUCTION_CATEGORIES, findCategoryByCode } from '../../core/constants/auction-taxonomy';
import { AnalyticsDashboard, CategoryMetric } from '../../core/models/analytics-dashboard.model';
import { AnalyticsApiService } from '../../core/services/analytics-api.service';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, TagModule, MessageModule, ProgressSpinnerModule],
  templateUrl: './dashboard.page.html',
  styleUrl: './dashboard.page.scss'
})
export class DashboardPageComponent implements OnInit {
  private readonly analyticsApi = inject(AnalyticsApiService);

  dashboard: AnalyticsDashboard | null = null;
  loading = false;
  errorMessage: string | null = null;

  readonly metricCards = [
    {
      key: 'activeAuctions',
      label: 'Active auctions',
      accent: 'success',
      icon: 'pi pi-bolt'
    },
    {
      key: 'closeRatePercent',
      label: 'Close rate',
      accent: 'info',
      icon: 'pi pi-percentage'
    },
    {
      key: 'averageFinalPrice',
      label: 'Average final price',
      accent: 'warn',
      icon: 'pi pi-wallet'
    },
    {
      key: 'totalWatchlistEntries',
      label: 'Watchlist activity',
      accent: 'contrast',
      icon: 'pi pi-heart'
    }
  ] as const;

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading = true;
    this.errorMessage = null;

    this.analyticsApi
      .getDashboard()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (dashboard) => {
          this.dashboard = dashboard;
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load analytics dashboard.';
        }
      });
  }

  categoryLabel(categoryCode: string): string {
    return findCategoryByCode(categoryCode)?.label ?? (categoryCode === 'UNCATEGORIZED' ? 'Uncategorized' : categoryCode);
  }

  metricValue(metricKey: string): string {
    if (!this.dashboard) {
      return '-';
    }

    switch (metricKey) {
      case 'activeAuctions':
        return String(this.dashboard.activeAuctions);
      case 'closeRatePercent':
        return `${this.dashboard.closeRatePercent.toFixed(2)}%`;
      case 'averageFinalPrice':
        return new Intl.NumberFormat('en-US', {
          style: 'currency',
          currency: 'EUR',
          minimumFractionDigits: 2,
          maximumFractionDigits: 2
        }).format(this.dashboard.averageFinalPrice);
      case 'totalWatchlistEntries':
        return String(this.dashboard.totalWatchlistEntries);
      default:
        return '-';
    }
  }

  barWidth(metric: CategoryMetric, metrics: CategoryMetric[]): number {
    const max = Math.max(...metrics.map((entry) => entry.count), 0);

    if (max === 0) {
      return 0;
    }

    return Math.round((metric.count / max) * 100);
  }

  totalTrackedCategories(metrics: CategoryMetric[]): number {
    return new Set(metrics.map((metric) => metric.categoryCode)).size;
  }

  hasMetrics(metrics: CategoryMetric[] | null | undefined): boolean {
    return !!metrics && metrics.length > 0;
  }

  categoryCoverageText(metrics: CategoryMetric[]): string {
    const tracked = this.totalTrackedCategories(metrics);
    return `${tracked} / ${AUCTION_CATEGORIES.length} curated categories represented`;
  }
}
