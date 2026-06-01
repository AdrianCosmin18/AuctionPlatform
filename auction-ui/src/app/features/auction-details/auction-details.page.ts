import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { Subject, Subscription, finalize, takeUntil } from 'rxjs';
import { AuctionBusinessEvent, AuctionClosedEvent, AuctionExtendedEvent, BidPlacedEvent } from '../../core/models/auction-business-events.model';
import { Auction } from '../../core/models/auction.model';
import { AuctionRealtimeEvent } from '../../core/models/auction-realtime-event.model';
import { AuctionStatus } from '../../core/models/auction-status.type';
import { Bid } from '../../core/models/bid.model';
import { AuctionApiService } from '../../core/services/auction-api.service';
import { AuctionWsService } from '../../core/services/auction-ws.service';

@Component({
  selector: 'app-auction-details-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    CardModule,
    TagModule,
    ButtonModule,
    TableModule,
    DividerModule,
    InputNumberModule,
    ProgressSpinnerModule,
    MessageModule,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './auction-details.page.html',
  styleUrl: './auction-details.page.scss'
})
export class AuctionDetailsPageComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AuctionApiService);
  private readonly ws = inject(AuctionWsService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  auction: Auction | null = null;
  bids: Bid[] = [];
  recentEvents: AuctionRealtimeEvent[] = [];
  loading = false;
  placingBid = false;
  actionLoading = false;
  errorMessage: string | null = null;
  liveMessage: string | null = null;
  private liveSubscription?: Subscription;

  readonly bidForm = this.fb.nonNullable.group({
    bidderId: [2, [Validators.required, Validators.min(1)]],
    amount: [0, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = Number(params.get('id'));

      if (!Number.isFinite(id) || id <= 0) {
        this.errorMessage = 'ID-ul licitatiei este invalid.';
        return;
      }

      this.connectToAuction(id);
      this.loadAuction(id);
      this.loadBids(id);
    });
  }

  ngOnDestroy(): void {
    this.liveSubscription?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadAuction(auctionId: number): void {
    this.loading = true;
    this.errorMessage = null;

    this.api
      .getAuction(auctionId)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (auction) => {
          this.auction = auction;
          this.syncBidDefaultAmount();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Nu am putut incarca licitatia.';
        }
      });
  }

  loadBids(auctionId: number): void {
    this.api.getBids(auctionId).subscribe({
      next: (bids) => {
        this.bids = bids;
      },
      error: (error) => {
        this.errorMessage = error?.error?.detail ?? 'Nu am putut incarca bid-urile.';
      }
    });
  }

  startAuction(): void {
    if (!this.auction) {
      return;
    }

    this.actionLoading = true;
    this.api
      .startAuction(this.auction.id)
      .pipe(finalize(() => (this.actionLoading = false)))
      .subscribe({
        next: (auction) => (this.auction = auction),
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Pornirea licitatiei a esuat.';
        }
      });
  }

  closeAuction(): void {
    if (!this.auction) {
      return;
    }

    this.actionLoading = true;
    this.api
      .closeAuction(this.auction.id)
      .pipe(finalize(() => (this.actionLoading = false)))
      .subscribe({
        next: (auction) => (this.auction = auction),
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Inchiderea licitatiei a esuat.';
        }
      });
  }

  placeBid(): void {
    if (!this.auction || this.bidForm.invalid) {
      this.bidForm.markAllAsTouched();
      return;
    }

    const { bidderId, amount } = this.bidForm.getRawValue();
    this.placingBid = true;
    this.liveMessage = null;

    this.api
      .placeBid(this.auction.id, { bidderId, amount })
      .pipe(finalize(() => (this.placingBid = false)))
      .subscribe({
        next: (bid) => {
          this.bids = [bid, ...this.bids];
          if (this.auction) {
            this.auction = {
              ...this.auction,
              currentPrice: bid.amount,
              endTime: bid.newEndTime ?? this.auction.endTime
            };
          }

          this.liveMessage = bid.auctionExtended
            ? 'Bid acceptat. Licitatia a fost extinsa automat.'
            : 'Bid acceptat.';
          this.syncBidDefaultAmount();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Bid-ul nu a putut fi plasat.';
        }
      });
  }

  statusSeverity(status: AuctionStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (status) {
      case 'RUNNING':
        return 'success';
      case 'DRAFT':
        return 'warn';
      case 'ENDED':
        return 'secondary';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'info';
    }
  }

  eventSummary(event: AuctionRealtimeEvent): string {
    switch (event.type) {
      case 'BID_PLACED': {
        const payload = event.payload as BidPlacedEvent;
        return `Bid #${payload.bidId} la ${payload.amount}`;
      }
      case 'AUCTION_EXTENDED': {
        const payload = event.payload as AuctionExtendedEvent;
        return `Nou end time: ${payload.newEndTime}`;
      }
      case 'AUCTION_CLOSED': {
        const payload = event.payload as AuctionClosedEvent;
        return `Final price: ${payload.finalPrice}`;
      }
      default:
        return '';
    }
  }

  trackEvent(index: number, event: AuctionRealtimeEvent): string {
    return `${event.type}-${event.occurredAt}-${index}`;
  }

  private connectToAuction(auctionId: number): void {
    this.recentEvents = [];
    this.liveSubscription?.unsubscribe();
    this.liveSubscription = this.ws
      .watchAuction(auctionId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event) => {
          this.recentEvents = [event, ...this.recentEvents].slice(0, 10);
          this.handleRealtimeEvent(event);
        },
        error: () => {
          this.errorMessage = 'Conexiunea live pentru licitatie a fost intrerupta.';
        }
      });
  }

  private handleRealtimeEvent(event: AuctionRealtimeEvent<AuctionBusinessEvent>): void {
    if (!this.auction) {
      return;
    }

    switch (event.type) {
      case 'BID_PLACED': {
        const payload = event.payload as BidPlacedEvent;
        this.auction = { ...this.auction, currentPrice: payload.currentPrice };
        this.loadBids(this.auction.id);
        this.syncBidDefaultAmount();
        break;
      }
      case 'AUCTION_EXTENDED': {
        const payload = event.payload as AuctionExtendedEvent;
        this.auction = { ...this.auction, endTime: payload.newEndTime };
        break;
      }
      case 'AUCTION_CLOSED': {
        this.loadAuction(this.auction.id);
        break;
      }
    }
  }

  private syncBidDefaultAmount(): void {
    if (!this.auction) {
      return;
    }

    const nextAmount = Number(this.auction.currentPrice) + Number(this.auction.minIncrement);
    this.bidForm.patchValue({ amount: nextAmount }, { emitEvent: false });
  }
}
