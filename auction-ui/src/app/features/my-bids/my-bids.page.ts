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
import { AuctionStatus } from '../../core/models/auction-status.type';
import { MyBidAuction } from '../../core/models/my-bid-auction.model';
import { AuctionApiService } from '../../core/services/auction-api.service';
import { MyActivityHeaderComponent } from '../my-activity/my-activity-header.component';

@Component({
  selector: 'app-my-bids-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, TagModule, ChipModule, ButtonModule, MessageModule, ProgressSpinnerModule, CurrencyPipe, DatePipe, MyActivityHeaderComponent],
  templateUrl: './my-bids.page.html',
  styleUrl: './my-bids.page.scss'
})
export class MyBidsPageComponent implements OnInit {
  private readonly auctionApi = inject(AuctionApiService);

  bids: MyBidAuction[] = [];
  loading = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadBids();
  }

  loadBids(): void {
    this.loading = true;
    this.errorMessage = null;

    this.auctionApi
      .getMyBids()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (bids) => {
          this.bids = bids;
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load your bidding activity.';
        }
      });
  }

  primaryImage(imageUrl: string | undefined): string | null {
    if (!imageUrl) {
      return null;
    }

    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }

  categoryLabel(code: string | null | undefined): string {
    return findCategoryByCode(code)?.label ?? 'Uncategorized';
  }

  subcategoryLabel(categoryCode: string | null | undefined, subcategoryCode: string | null | undefined): string | null {
    const category = findCategoryByCode(categoryCode);
    return category?.subcategories.find((subcategory) => subcategory.code === subcategoryCode)?.label ?? null;
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

  bidStateSeverity(bid: MyBidAuction): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    if (bid.auction.reservePrice !== null && bid.auction.status === 'ENDED' && bid.auction.reserveMet === false) {
      return 'secondary';
    }

    if (bid.auction.status === 'SUSPENDED') {
      return 'danger';
    }

    if (bid.won) {
      return 'success';
    }

    if (bid.leading) {
      return 'info';
    }

    if (bid.auction.status === 'ENDED') {
      return 'secondary';
    }

    return 'warn';
  }

  bidStateLabel(bid: MyBidAuction): string {
    if (bid.auction.reservePrice !== null && bid.auction.status === 'ENDED' && bid.auction.reserveMet === false) {
      return 'Reserve not met';
    }

    if (bid.auction.status === 'SUSPENDED') {
      return 'Suspended by admin';
    }

    if (bid.won) {
      return 'Won';
    }

    if (bid.leading) {
      return 'Leading';
    }

    if (bid.auction.status === 'ENDED') {
      return 'Outbid / Lost';
    }

    return 'Watching contest';
  }

  reserveLabel(bid: MyBidAuction): string | null {
    if (bid.auction.reservePrice === null) {
      return null;
    }

    if (bid.auction.reserveMet === true) {
      return 'Reserve met';
    }

    return bid.auction.status === 'ENDED' ? 'Reserve not met' : 'Reserve pending';
  }

  reserveSeverity(bid: MyBidAuction): 'success' | 'warn' | 'secondary' {
    if (bid.auction.reserveMet === true) {
      return 'success';
    }

    return bid.auction.status === 'ENDED' ? 'secondary' : 'warn';
  }
}
