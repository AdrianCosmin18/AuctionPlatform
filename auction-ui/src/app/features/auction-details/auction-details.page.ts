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
import { BehaviorSubject, Subject, Subscription, combineLatest, finalize, forkJoin, map, takeUntil } from 'rxjs';
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
  private auctionId: number | null = null;
  private liveSubscription?: Subscription;

  readonly auction$ = new BehaviorSubject<Auction | null>(null);
  readonly bids$ = new BehaviorSubject<Bid[]>([]);
  readonly recentEvents$ = new BehaviorSubject<AuctionRealtimeEvent<AuctionBusinessEvent>[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(true);
  readonly placingBid$ = new BehaviorSubject<boolean>(false);
  readonly actionLoading$ = new BehaviorSubject<boolean>(false);
  readonly error$ = new BehaviorSubject<string | null>(null);
  readonly liveMessage$ = new BehaviorSubject<string | null>(null);
  readonly vm$ = combineLatest({
    auction: this.auction$,
    bids: this.bids$,
    recentEvents: this.recentEvents$,
    loading: this.loading$,
    placingBid: this.placingBid$,
    actionLoading: this.actionLoading$,
    error: this.error$,
    liveMessage: this.liveMessage$
  }).pipe(
    map(({ auction, bids, recentEvents, loading, placingBid, actionLoading, error, liveMessage }) => ({
      auction,
      bids,
      recentEvents,
      loading,
      placingBid,
      actionLoading,
      error,
      liveMessage,
      bidCount: bids.length,
      nextMinimumBid: auction ? Number(auction.currentPrice) + Number(auction.minIncrement) : null,
      lastBidderId: bids[0]?.bidderId ?? null,
      canPlaceBid: auction?.status === 'RUNNING'
    }))
  );

  readonly bidForm = this.fb.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01)]]
  });

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntil(this.destroy$)).subscribe((params) => {
      const id = Number(params.get('id'));

      if (!Number.isFinite(id) || id <= 0) {
        this.error$.next('ID-ul licitatiei este invalid.');
        return;
      }

      this.auctionId = id;
      this.resetState();
      this.connectToAuction(id);
      this.loadSnapshot(id);
    });
  }

  ngOnDestroy(): void {
    this.liveSubscription?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  refreshSnapshot(): void {
    if (!this.auctionId) {
      return;
    }

    this.loadSnapshot(this.auctionId);
  }

  startAuction(): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    this.actionLoading$.next(true);
    this.error$.next(null);
    this.api
      .startAuction(auction.id)
      .pipe(finalize(() => this.actionLoading$.next(false)))
      .subscribe({
        next: (updatedAuction) => {
          this.auction$.next(updatedAuction);
          this.syncBidDefaultAmount();
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Pornirea licitatiei a esuat.');
        }
      });
  }

  closeAuction(): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    this.actionLoading$.next(true);
    this.error$.next(null);
    this.api
      .closeAuction(auction.id)
      .pipe(finalize(() => this.actionLoading$.next(false)))
      .subscribe({
        next: (updatedAuction) => {
          this.auction$.next(updatedAuction);
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Inchiderea licitatiei a esuat.');
        }
      });
  }

  placeBid(): void {
    const auction = this.auction$.value;

    if (!auction || this.bidForm.invalid) {
      this.bidForm.markAllAsTouched();
      return;
    }

    const { amount } = this.bidForm.getRawValue();
    this.placingBid$.next(true);
    this.error$.next(null);
    this.liveMessage$.next(null);

    this.api
      .placeBid(auction.id, { bidderId: 2, amount })
      .pipe(finalize(() => this.placingBid$.next(false)))
      .subscribe({
        next: () => {
          this.bidForm.reset({ amount }, { emitEvent: false });
          this.liveMessage$.next('Bid trimis. Astept confirmarea live.');
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Bid-ul nu a putut fi plasat.');
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

  eventSeverity(event: AuctionRealtimeEvent): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (event.type) {
      case 'BID_PLACED':
        return 'success';
      case 'AUCTION_EXTENDED':
        return 'warn';
      case 'AUCTION_CLOSED':
        return 'secondary';
      default:
        return 'info';
    }
  }

  trackEvent(index: number, event: AuctionRealtimeEvent): string {
    return `${event.type}-${event.occurredAt}-${index}`;
  }

  private connectToAuction(auctionId: number): void {
    this.recentEvents$.next([]);
    this.liveSubscription?.unsubscribe();
    this.liveSubscription = this.ws
      .watchAuction(auctionId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (event) => {
          this.recentEvents$.next([event, ...this.recentEvents$.value].slice(0, 10));
          this.handleRealtimeEvent(event);
        },
        error: () => {
          this.error$.next('Conexiunea live pentru licitatie a fost intrerupta.');
        }
      });
  }

  private handleRealtimeEvent(event: AuctionRealtimeEvent<AuctionBusinessEvent>): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    switch (event.type) {
      case 'BID_PLACED': {
        const payload = event.payload as BidPlacedEvent;
        const nextBid: Bid = {
          id: payload.bidId,
          auctionId: payload.auctionId,
          bidderId: payload.bidderId,
          amount: payload.amount,
          createdAt: event.occurredAt,
          auctionExtended: false,
          newEndTime: null
        };

        this.auction$.next({ ...auction, currentPrice: payload.currentPrice });
        this.bids$.next([nextBid, ...this.bids$.value.filter((bid) => bid.id !== nextBid.id)]);
        this.liveMessage$.next(`Bid nou primit pentru licitatia #${payload.auctionId}.`);
        this.syncBidDefaultAmount();
        break;
      }
      case 'AUCTION_EXTENDED': {
        const payload = event.payload as AuctionExtendedEvent;
        this.auction$.next({ ...auction, endTime: payload.newEndTime });
        this.bids$.next(
          this.bids$.value.map((bid, index) =>
            index === 0 ? { ...bid, auctionExtended: true, newEndTime: payload.newEndTime } : bid
          )
        );
        this.liveMessage$.next('Licitatia a fost extinsa automat.');
        break;
      }
      case 'AUCTION_CLOSED': {
        const payload = event.payload as AuctionClosedEvent;
        this.auction$.next({
          ...auction,
          currentPrice: payload.finalPrice,
          status: 'ENDED'
        });
        this.liveMessage$.next('Licitatia s-a incheiat.');
        break;
      }
    }
  }

  private syncBidDefaultAmount(): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    const nextAmount = Number(auction.currentPrice) + Number(auction.minIncrement);
    this.bidForm.patchValue({ amount: nextAmount }, { emitEvent: false });
  }

  private loadSnapshot(auctionId: number): void {
    this.loading$.next(true);
    this.error$.next(null);

    forkJoin({
      auction: this.api.getAuction(auctionId),
      bids: this.api.getBids(auctionId)
    })
      .pipe(finalize(() => this.loading$.next(false)))
      .subscribe({
        next: ({ auction, bids }) => {
          this.auction$.next(auction);
          this.bids$.next(bids);
          this.syncBidDefaultAmount();
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Nu am putut incarca licitatia.');
        }
      });
  }

  private resetState(): void {
    this.auction$.next(null);
    this.bids$.next([]);
    this.recentEvents$.next([]);
    this.loading$.next(true);
    this.placingBid$.next(false);
    this.actionLoading$.next(false);
    this.error$.next(null);
    this.liveMessage$.next(null);
  }
}
