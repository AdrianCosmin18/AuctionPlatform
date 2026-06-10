import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { InputTextarea } from 'primeng/inputtextarea';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { finalize } from 'rxjs';
import { FraudOverview, FraudSeverity, FraudSignal, FraudSignalType } from '../../core/models/fraud-overview.model';
import { AuctionApiService } from '../../core/services/auction-api.service';
import { FraudApiService } from '../../core/services/fraud-api.service';

@Component({
  selector: 'app-fraud-signals-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CardModule,
    ButtonModule,
    DialogModule,
    InputTextarea,
    MessageModule,
    ProgressSpinnerModule,
    TableModule,
    TagModule,
    ToastModule,
    DatePipe
  ],
  templateUrl: './fraud-signals.page.html',
  styleUrl: './fraud-signals.page.scss'
})
export class FraudSignalsPageComponent implements OnInit {
  private readonly fraudApi = inject(FraudApiService);
  private readonly auctionApi = inject(AuctionApiService);
  private readonly messageService = inject(MessageService);

  overview: FraudOverview | null = null;
  loading = false;
  suspendLoading = false;
  errorMessage: string | null = null;
  suspendDialogVisible = false;
  suspendReason = '';
  selectedSignal: FraudSignal | null = null;
  private readonly suspendedAuctionIds = new Set<number>();

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

  canSuspend(signal: FraudSignal): boolean {
    return !!signal.auctionId && !this.suspendedAuctionIds.has(signal.auctionId);
  }

  openSuspendDialog(signal: FraudSignal): void {
    if (!signal.auctionId) {
      return;
    }

    this.selectedSignal = signal;
    this.suspendReason = '';
    this.suspendDialogVisible = true;
  }

  closeSuspendDialog(): void {
    this.suspendDialogVisible = false;
    this.suspendReason = '';
    this.selectedSignal = null;
  }

  suspendSelectedAuction(): void {
    if (!this.selectedSignal?.auctionId) {
      return;
    }

    const reason = this.suspendReason.trim();
    if (!reason) {
      this.errorMessage = 'Suspension reason is required.';
      return;
    }

    this.suspendLoading = true;
    this.errorMessage = null;
    this.auctionApi
      .suspendAuction(this.selectedSignal.auctionId, { reason })
      .pipe(finalize(() => (this.suspendLoading = false)))
      .subscribe({
        next: (auction) => {
          this.suspendedAuctionIds.add(auction.id);
          this.showToast('warn', 'Auction suspended', `Auction #${auction.id} was suspended and notifications were sent.`);
          this.closeSuspendDialog();
          this.loadSignals();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to suspend the auction.';
        }
      });
  }

  private showToast(severity: 'success' | 'info' | 'warn' | 'error', summary: string, detail: string): void {
    this.messageService.add({
      key: 'fraud-signals',
      severity,
      summary,
      detail
    });
  }
}
