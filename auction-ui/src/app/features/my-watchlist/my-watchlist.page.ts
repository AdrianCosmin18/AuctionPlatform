import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChipModule } from 'primeng/chip';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { AUTHENTICITY_STATUSES, ITEM_CONDITIONS, findCategoryByCode, findOptionLabel } from '../../core/constants/auction-taxonomy';
import { environment } from '../../../environments/environment';
import { Auction } from '../../core/models/auction.model';
import { AuctionStatus } from '../../core/models/auction-status.type';
import { AuctionApiService } from '../../core/services/auction-api.service';

@Component({
  selector: 'app-my-watchlist-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, TagModule, ChipModule, ButtonModule, MessageModule, ProgressSpinnerModule, CurrencyPipe, DatePipe],
  templateUrl: './my-watchlist.page.html',
  styleUrl: './my-watchlist.page.scss'
})
export class MyWatchlistPageComponent implements OnInit {
  private readonly auctionApi = inject(AuctionApiService);

  watchlist: Auction[] = [];
  loading = false;
  actionLoadingId: number | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadWatchlist();
  }

  loadWatchlist(): void {
    this.loading = true;
    this.errorMessage = null;

    this.auctionApi
      .getMyWatchlist()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (watchlist) => {
          this.watchlist = watchlist;
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load your watchlist.';
        }
      });
  }

  unwatch(auction: Auction): void {
    this.actionLoadingId = auction.id;
    this.errorMessage = null;

    this.auctionApi
      .unwatchAuction(auction.id)
      .pipe(finalize(() => (this.actionLoadingId = null)))
      .subscribe({
        next: () => {
          this.watchlist = this.watchlist.filter((item) => item.id !== auction.id);
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to remove the auction from your watchlist.';
        }
      });
  }

  primaryImage(auction: Auction): string | null {
    return auction.images[0] ? this.resolveImageUrl(auction.images[0].imageUrl) : null;
  }

  categoryLabel(code: string | null | undefined): string {
    return findCategoryByCode(code)?.label ?? 'Uncategorized';
  }

  subcategoryLabel(auction: Auction): string | null {
    const category = findCategoryByCode(auction.categoryCode);
    return category?.subcategories.find((subcategory) => subcategory.code === auction.subcategoryCode)?.label ?? null;
  }

  itemConditionLabel(code: string | null | undefined): string | null {
    return findOptionLabel(ITEM_CONDITIONS, code);
  }

  authenticityLabel(code: string | null | undefined): string | null {
    return findOptionLabel(AUTHENTICITY_STATUSES, code);
  }

  statusSeverity(status: AuctionStatus): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (status) {
      case 'RUNNING':
        return 'success';
      case 'DRAFT':
        return 'warn';
      case 'SUSPENDED':
        return 'danger';
      case 'ENDED':
        return 'secondary';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'info';
    }
  }

  reserveLabel(auction: Auction): string | null {
    if (auction.reservePrice === null) {
      return null;
    }

    if (auction.reserveMet === true) {
      return 'Reserve met';
    }

    return auction.status === 'ENDED' ? 'Reserve not met' : 'Reserve pending';
  }

  reserveSeverity(auction: Auction): 'success' | 'warn' | 'secondary' {
    if (auction.reserveMet === true) {
      return 'success';
    }

    return auction.status === 'ENDED' ? 'secondary' : 'warn';
  }

  private resolveImageUrl(imageUrl: string): string {
    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }
}
