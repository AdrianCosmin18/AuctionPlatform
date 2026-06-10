import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, inject } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ConfirmationService, MessageService } from 'primeng/api';
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
import { AUTHENTICITY_STATUSES, ITEM_CONDITIONS, findCategoryByCode, findOptionLabel } from '../../core/constants/auction-taxonomy';
import { environment } from '../../../environments/environment';
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
  private static readonly CURRENT_BIDDER_ID = 2;
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AuctionApiService);
  private readonly ws = inject(AuctionWsService);
  private readonly fb = inject(FormBuilder);
  private readonly confirmationService = inject(ConfirmationService);
  private readonly messageService = inject(MessageService);
  private readonly destroy$ = new Subject<void>();
  private readonly currentUserId = this.api.getCurrentUserId();
  private auctionId: number | null = null;
  private liveSubscription?: Subscription;
  private liveMessageTimer: ReturnType<typeof setTimeout> | null = null;
  private readonly suppressedRealtimeToasts = new Map<string, number>();
  private readonly currencyFormatter = new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2
  });
  selectedImageIndex = 0;

  readonly auction$ = new BehaviorSubject<Auction | null>(null);
  readonly bids$ = new BehaviorSubject<Bid[]>([]);
  readonly recentEvents$ = new BehaviorSubject<AuctionRealtimeEvent<AuctionBusinessEvent>[]>([]);
  readonly loading$ = new BehaviorSubject<boolean>(true);
  readonly placingBid$ = new BehaviorSubject<boolean>(false);
  readonly actionLoading$ = new BehaviorSubject<boolean>(false);
  readonly watchLoading$ = new BehaviorSubject<boolean>(false);
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
    watchLoading: this.watchLoading$,
    error: this.error$,
    liveMessage: this.liveMessage$,
    remainingTime: this.remainingTime$
  }).pipe(
    map(({ auction, bids, recentEvents, loading, placingBid, actionLoading, watchLoading, error, liveMessage, remainingTime }) => ({
      auction,
      bids,
      recentEvents,
      loading,
      placingBid,
      actionLoading,
      watchLoading,
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
        this.error$.next('The auction id is invalid.');
        return;
      }

      this.auctionId = id;
      this.selectedImageIndex = 0;
      this.resetState();
      this.connectToAuction(id);
      this.loadSnapshot(id);
    });
  }

  ngOnDestroy(): void {
    this.liveSubscription?.unsubscribe();
    this.clearLiveMessageTimer();
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
          this.showToast('success', 'Auction started', 'The lot is now live for bidding.');
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Unable to start the auction.');
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
          this.showToast('info', 'Auction closed', 'The auction was closed manually.');
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Unable to close the auction.');
        }
      });
  }

  buyNowAuction(): void {
    const auction = this.auction$.value;

    if (!auction || !this.canBuyNow(auction)) {
      return;
    }

    this.confirmationService.confirm({
      header: 'Confirm Buy Now',
      message: `This will immediately close the auction and purchase the item for ${this.formatAmount(auction.buyNowPrice!)}. Do you want to continue?`,
      icon: 'pi pi-exclamation-triangle',
      rejectLabel: 'Cancel',
      acceptLabel: 'Confirm purchase',
      acceptButtonStyleClass: 'p-button-warning',
      accept: () => {
        this.actionLoading$.next(true);
        this.error$.next(null);
        this.api
          .buyNowAuction(auction.id)
          .pipe(finalize(() => this.actionLoading$.next(false)))
          .subscribe({
            next: (updatedAuction) => {
              this.auction$.next(updatedAuction);
              this.suppressRealtimeToast('AUCTION_CLOSED');
              this.showToast('success', 'Buy Now completed', 'The auction was closed instantly at the Buy Now price.');
            },
            error: (error) => {
              this.error$.next(error?.error?.detail ?? 'Unable to complete Buy Now.');
            }
          });
      }
    });
  }

  toggleWatch(): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    this.watchLoading$.next(true);
    this.error$.next(null);

    const request$ = auction.watchedByCurrentUser
      ? this.api.unwatchAuction(auction.id)
      : this.api.watchAuction(auction.id);

    request$
      .pipe(finalize(() => this.watchLoading$.next(false)))
      .subscribe({
        next: (updatedAuction) => {
          this.auction$.next(updatedAuction);
          this.showToast(
            'info',
            updatedAuction.watchedByCurrentUser ? 'Added to watchlist' : 'Removed from watchlist',
            updatedAuction.watchedByCurrentUser
              ? 'You will now see this lot in My Watchlist.'
              : 'The lot was removed from My Watchlist.'
          );
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Unable to update the watchlist.');
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
      this.error$.next(`The minimum allowed bid is ${minimumAllowed.toFixed(2)} EUR.`);
      return;
    }

    this.placingBid$.next(true);
    this.error$.next(null);
    this.setLiveMessage(null);

    this.api
      .placeBid(auction.id, { bidderId: AuctionDetailsPageComponent.CURRENT_BIDDER_ID, amount })
      .pipe(finalize(() => this.placingBid$.next(false)))
      .subscribe({
        next: (createdBid) => {
          this.bidForm.reset({ amount }, { emitEvent: false });
          this.applyAcceptedBid(createdBid);
          this.suppressRealtimeToast('BID_PLACED');
          this.setLiveMessage(`Bid accepted at ${this.formatAmount(createdBid.amount)}. Syncing auction activity...`, 4000);
          this.loadSnapshot(auction.id);
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
        return `Bid #${payload.bidId} at ${payload.amount}`;
      }
      case 'AUCTION_EXTENDED': {
        const payload = event.payload as AuctionExtendedEvent;
        return `New end time: ${payload.newEndTime}`;
      }
      case 'AUCTION_CLOSED': {
        const payload = event.payload as AuctionClosedEvent;
        return payload.winnerId
          ? `Final price: ${payload.finalPrice} · Winner #${payload.winnerId}`
          : payload.reserveMet === false
            ? `Final price: ${payload.finalPrice} · Reserve not met`
            : `Final price: ${payload.finalPrice} · No winner`;
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

  closeReasonLabel(reason: string | null | undefined): string {
    switch (reason) {
      case 'EXPIRED':
        return 'Expired automatically';
      case 'MANUAL':
        return 'Closed manually';
      case 'BUY_NOW':
        return 'Closed by Buy Now';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return '-';
    }
  }

  reserveStatusLabel(auction: Auction | null): string | null {
    if (!auction || auction.reservePrice === null) {
      return null;
    }

    if (auction.reserveMet === true) {
      return 'Reserve met';
    }

    return auction.status === 'ENDED' ? 'Reserve not met' : 'Reserve pending';
  }

  reserveStatusSeverity(auction: Auction | null): 'success' | 'warn' | 'secondary' | 'info' {
    if (!auction || auction.reservePrice === null) {
      return 'info';
    }

    if (auction.reserveMet === true) {
      return 'success';
    }

    return auction.status === 'ENDED' ? 'secondary' : 'warn';
  }

  canBuyNow(auction: Auction | null): boolean {
    return (
      !!auction &&
      auction.status === 'RUNNING' &&
      auction.buyNowPrice !== null &&
      auction.currentPrice < auction.buyNowPrice &&
      auction.createdBy !== this.currentUserId
    );
  }

  buyNowLabel(auction: Auction | null): string {
    if (!auction?.buyNowPrice) {
      return 'Buy Now';
    }

    return `Buy now for ${this.formatAmount(auction.buyNowPrice)}`;
  }

  selectImage(index: number): void {
    this.selectedImageIndex = index;
  }

  imageSrc(imageUrl: string): string {
    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }

  categoryLabel(code: string | null | undefined): string {
    return findCategoryByCode(code)?.label ?? 'Uncategorized';
  }

  subcategoryLabel(auction: Auction | null): string | null {
    if (!auction) {
      return null;
    }

    return findCategoryByCode(auction.categoryCode)?.subcategories.find((subcategory) => subcategory.code === auction.subcategoryCode)?.label ?? null;
  }

  itemConditionLabel(code: string | null | undefined): string | null {
    return findOptionLabel(ITEM_CONDITIONS, code);
  }

  authenticityLabel(code: string | null | undefined): string | null {
    return findOptionLabel(AUTHENTICITY_STATUSES, code);
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
          this.error$.next('The live connection for this auction was interrupted.');
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
        this.setLiveMessage(
          payload.bidderId === AuctionDetailsPageComponent.CURRENT_BIDDER_ID
            ? `Your bid is now live at ${this.formatAmount(payload.amount)}.`
            : `A competing bid moved the price to ${this.formatAmount(payload.currentPrice)}.`,
          4000
        );
        this.maybeShowRealtimeToast('BID_PLACED', 'success', 'New bid', `New bid: ${this.formatAmount(payload.amount)}`);
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
        this.setLiveMessage('The auction was extended automatically.', 5000);
        this.maybeShowRealtimeToast('AUCTION_EXTENDED', 'warn', 'Auction extended', 'The auction was extended.');
        break;
      }
      case 'AUCTION_CLOSED': {
        const payload = event.payload as AuctionClosedEvent;
        this.auction$.next({
          ...auction,
          currentPrice: payload.finalPrice,
          status: 'ENDED',
          winnerId: payload.winnerId,
          winningBidId: payload.winningBidId,
          finalPrice: payload.finalPrice,
          reserveMet: payload.reserveMet,
          closedAt: payload.closedAt,
          closedReason: payload.closedReason
        });
        this.setLiveMessage(payload.reserveMet === false ? 'The auction ended without meeting the reserve price.' : 'The auction has ended.', 5000);
        this.maybeShowRealtimeToast('AUCTION_CLOSED', 'info', 'Auction closed', 'The auction has been closed.');
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
          this.selectedImageIndex = 0;
          this.syncBidDefaultAmount();
        },
        error: (error) => {
          this.error$.next(error?.error?.detail ?? 'Unable to load the auction.');
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
    this.watchLoading$.next(false);
    this.error$.next(null);
    this.setLiveMessage(null);
  }

  private applyAcceptedBid(createdBid: Bid): void {
    const auction = this.auction$.value;

    if (!auction) {
      return;
    }

    this.auction$.next({
      ...auction,
      currentPrice: createdBid.amount
    });
    this.bids$.next([createdBid, ...this.bids$.value.filter((bid) => bid.id !== createdBid.id)]);
    this.syncBidDefaultAmount();
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
      return detail ?? 'The bid conflicted with a concurrent update. Refresh and try again.';
    }

    if (error.status === 400) {
      return detail ?? 'The bid is invalid or the auction is no longer accepting offers.';
    }

    return detail ?? 'The bid could not be placed.';
  }

  private showToast(
    severity: 'success' | 'info' | 'warn' | 'error',
    summary: string,
    detail: string
  ): void {
    this.messageService.add({
      key: 'auction-details',
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

  private setLiveMessage(message: string | null, autoClearMs?: number): void {
    this.clearLiveMessageTimer();
    this.liveMessage$.next(message);

    if (message && autoClearMs) {
      this.liveMessageTimer = setTimeout(() => {
        this.liveMessage$.next(null);
        this.liveMessageTimer = null;
      }, autoClearMs);
    }
  }

  private clearLiveMessageTimer(): void {
    if (this.liveMessageTimer) {
      clearTimeout(this.liveMessageTimer);
      this.liveMessageTimer = null;
    }
  }

  private formatAmount(amount: number): string {
    return this.currencyFormatter.format(amount);
  }
}
