import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { FraudOverview, FraudSeverity, FraudSignal, FraudSignalType } from '../../core/models/fraud-overview.model';
import { FraudApiService } from '../../core/services/fraud-api.service';

@Component({
  selector: 'app-fraud-signals-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, MessageModule, ProgressSpinnerModule, TableModule, TagModule, DatePipe],
  templateUrl: './fraud-signals.page.html',
  styleUrl: './fraud-signals.page.scss'
})
export class FraudSignalsPageComponent implements OnInit {
  private readonly fraudApi = inject(FraudApiService);

  overview: FraudOverview | null = null;
  loading = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadSignals();
  }

  loadSignals(): void {
    this.loading = true;
    this.errorMessage = null;

    this.fraudApi
      .getSignals()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (overview) => {
          this.overview = overview;
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load fraud signals.';
        }
      });
  }

  severityLabel(severity: FraudSeverity): string {
    return severity;
  }

  severityStyle(severity: FraudSeverity): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (severity) {
      case 'HIGH':
        return 'danger';
      case 'MEDIUM':
        return 'warn';
      case 'LOW':
        return 'secondary';
      default:
        return 'info';
    }
  }

  signalTypeLabel(type: FraudSignalType): string {
    switch (type) {
      case 'BURST_BIDDING':
        return 'Burst bidding';
      case 'SELLER_BIDDER_CONCENTRATION':
        return 'Seller-bidder concentration';
      default:
        return type;
    }
  }

  riskCards(): Array<{ label: string; value: number; severity: FraudSeverity | 'TOTAL'; hint: string }> {
    return [
      {
        label: 'Total signals',
        value: this.overview?.totalSignals ?? 0,
        severity: 'TOTAL',
        hint: 'All detected risk patterns'
      },
      {
        label: 'High severity',
        value: this.overview?.highSeveritySignals ?? 0,
        severity: 'HIGH',
        hint: 'Needs manual review first'
      },
      {
        label: 'Medium severity',
        value: this.overview?.mediumSeveritySignals ?? 0,
        severity: 'MEDIUM',
        hint: 'Investigate if pattern repeats'
      },
      {
        label: 'Low severity',
        value: this.overview?.lowSeveritySignals ?? 0,
        severity: 'LOW',
        hint: 'Weak signal, track over time'
      }
    ];
  }

  cardClass(severity: FraudSeverity | 'TOTAL'): string {
    switch (severity) {
      case 'HIGH':
        return 'risk-card--high';
      case 'MEDIUM':
        return 'risk-card--medium';
      case 'LOW':
        return 'risk-card--low';
      default:
        return 'risk-card--total';
    }
  }

  reviewTarget(signal: FraudSignal): string {
    if (signal.auctionId) {
      return `Auction #${signal.auctionId}`;
    }

    return signal.sellerId ? `Seller #${signal.sellerId}` : 'Cross-auction pattern';
  }
}
