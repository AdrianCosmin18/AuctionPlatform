import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DividerModule } from 'primeng/divider';
import { InputNumberModule } from 'primeng/inputnumber';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { BehaviorSubject, Subject, Subscription, combineLatest, finalize, forkJoin, map, takeUntil, timer } from 'rxjs';
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
    ToastModule,
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
  private readonly messageService = inject(MessageService);
  private readonly destroy$ = new Subject<void>();
  private auctionId: number | null = null;
  private liveSubscription?: Subscription;
  private readonly suppressedRealtimeToasts = new Map<string, number>();
  private readonly currencyFormatter = new Intl.NumberFormat('ro-RO', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });

  readonly auction$ = new BehaviorSubject<Auction | null>(null);
  readonly bids$ = new BehaviorSubject<Bid[]>([]);
  readonly recentEvents$ = new BehaviorSubject<AuctionRealtimeEvent<AuctionBusinessEvent>[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(true);
  readonly placingBid$ = new BehaviorSubject<boolean>(false);
  readonly actionLoading$ = new BehaviorSubject<boolean>(false);
  readonly error$ = new BehaviorSubject<string | null>(null);
  readonly liveMessage$ = new BehaviorSubject<string | null>(null);
  readonly remainingTime$ = combineLatest([this.auction$, timer(0, 1000)]).pipe(
    map(([auction]) => {
      if (!auction?.endTime) {
        return null;
      }

      const diffMs = new Date(auction.endTime).getTime() - Date.now();
      const totalSeconds = Math.max(0, Math.floor(diffMs / 1000));

      return {
        totalSeconds,
        days: Math.floor(totalSeconds / 86400),
        hours: Math.floor((totalSeconds % 86400) / 3600),
        minutes: Math.floor((totalSeconds % 3600) / 60),
        seconds: totalSeconds % 60,
        expired: totalSeconds === 0
      };
    })
  );
  readonly vm$ = combineLatest({
    auction: this.auction$,
    bids: this.bids$,
    recentEvents: this.recentEvents$,
    loading: this.loading$,
    placingBid: this.placingBid$,
    actionLoading: this.actionLoading$,
    error: this.error$,
    liveMessage: this.liveMessage$,
    remainingTime: this.remainingTime$
  }).pipe(
    map(({ auction, bids, recentEvents, loading, placingBid, actionLoading, error, liveMessage, remainingTime }) => ({
      auction,
      bids,
      recentEvents,
      loading,
      placingBid,
      actionLoading,
      error,
      liveMessage,
      remainingTime,
      bidCount: bids.length,
      nextMinimumBid: auction ? Number(auction.currentPrice) + Number(auction.minIncrement) : null,
      lastBidderId: bids[0]?.bidderId ?? null,
      canPlaceBid: auction?.status === 'RUNNING' && !remainingTime?.expired
    }))
  );

  readonly bidForm = this.fb.nonNullable.group({
    amount: [0, [Validators.required, Validators.min(0.01), this.minimumBidValidator()]]
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
          this.suppressRealtimeToast('AUCTION_EXTENDED');
          this.showToast('success', 'Licitație pornită', 'Licitația acceptă acum bid-uri.');
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
          this.suppressRealtimeToast('AUCTION_CLOSED');
          this.showToast('info', 'Licitație închisă', 'Licitația a fost închisă manual.');
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Inchiderea licitatiei a esuat.');
        }
      });
  }

  placeBid(): void {
    const auction = this.auction$.value;
    const remainingTime = this.remainingTimeSnapshot();

    if (!auction || this.bidForm.invalid || auction.status !== 'RUNNING' || remainingTime?.expired) {
      this.bidForm.markAllAsTouched();
      return;
    }

    const { amount } = this.bidForm.getRawValue();
    const minimumAllowed = Number(auction.currentPrice) + Number(auction.minIncrement);

    if (amount < minimumAllowed) {
      this.error$.next(`Bid-ul minim permis este ${minimumAllowed.toFixed(2)} EUR.`);
      return;
    }

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
          this.bidForm.controls.amount.updateValueAndValidity({ emitEvent: false });
        },
        error: (error: HttpErrorResponse) => {
          this.error$.next(this.bidErrorMessage(error));
        }
      });
  }

  minimumBid(): number | null {
    const auction = this.auction$.value;

    return auction ? Number(auction.currentPrice) + Number(auction.minIncrement) : null;
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
        this.maybeShowRealtimeToast(
          'BID_PLACED',
          'success',
          'Bid nou',
          `Bid nou: ${this.formatAmount(payload.amount)}`
        );
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
        this.maybeShowRealtimeToast('AUCTION_EXTENDED', 'warn', 'Licitație extinsă', 'Licitația a fost extinsă.');
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
        this.maybeShowRealtimeToast('AUCTION_CLOSED', 'info', 'Licitație închisă', 'Licitația s-a închis.');
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
    this.bidForm.controls.amount.updateValueAndValidity({ emitEvent: false });
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

  private remainingTimeSnapshot():
    | {
        totalSeconds: number;
        days: number;
        hours: number;
        minutes: number;
        seconds: number;
        expired: boolean;
      }
    | null {
    const auction = this.auction$.value;

    if (!auction?.endTime) {
      return null;
    }

    const diffMs = new Date(auction.endTime).getTime() - Date.now();
    const totalSeconds = Math.max(0, Math.floor(diffMs / 1000));

    return {
      totalSeconds,
      days: Math.floor(totalSeconds / 86400),
      hours: Math.floor((totalSeconds % 86400) / 3600),
      minutes: Math.floor((totalSeconds % 3600) / 60),
      seconds: totalSeconds % 60,
      expired: totalSeconds === 0
    };
  }

  private minimumBidValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const auction = this.auction$.value;
      const amount = Number(control.value);

      if (!auction || !Number.isFinite(amount)) {
        return null;
      }

      const minimumAllowed = Number(auction.currentPrice) + Number(auction.minIncrement);

      return amount >= minimumAllowed
        ? null
        : {
            minimumBid: {
              requiredMinimum: minimumAllowed,
              actual: amount
            }
          };
    };
  }

  private bidErrorMessage(error: HttpErrorResponse): string {
    const detail = error.error?.detail;

    if (error.status === 409) {
      return detail ?? 'Bid-ul a intrat in conflict cu o actualizare concurenta. Reincarca si incearca din nou.';
    }

    if (error.status === 400) {
      return detail ?? 'Bid-ul este invalid sau licitatia nu mai accepta oferte.';
    }

    return detail ?? 'Bid-ul nu a putut fi plasat.';
  }

  private showToast(
    severity: 'success' | 'info' | 'warn' | 'error',
    summary: string,
    detail: string
  ): void {
    this.messageService.add({
      severity,
      summary,
      detail
    });
  }

  private maybeShowRealtimeToast(
    eventType: 'BID_PLACED' | 'AUCTION_EXTENDED' | 'AUCTION_CLOSED',
    severity: 'success' | 'info' | 'warn' | 'error',
    summary: string,
    detail: string
  ): void {
    const suppressedUntil = this.suppressedRealtimeToasts.get(eventType) ?? 0;

    if (suppressedUntil > Date.now()) {
      this.suppressedRealtimeToasts.delete(eventType);
      return;
    }

    this.showToast(severity, summary, detail);
  }

  private suppressRealtimeToast(eventType: 'BID_PLACED' | 'AUCTION_EXTENDED' | 'AUCTION_CLOSED'): void {
    this.suppressedRealtimeToasts.set(eventType, Date.now() + 5000);
  }

  private formatAmount(amount: number): string {
    return this.currencyFormatter.format(amount);
  }
}
