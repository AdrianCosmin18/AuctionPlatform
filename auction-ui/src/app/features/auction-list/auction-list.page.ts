import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ChipModule } from 'primeng/chip';
import { IconFieldModule } from 'primeng/iconfield';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { PaginatorModule } from 'primeng/paginator';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectButtonModule } from 'primeng/selectbutton';
import { SelectModule } from 'primeng/select';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { AUTHENTICITY_STATUSES, AUCTION_CATEGORIES, ITEM_CONDITIONS, findCategoryByCode, findOptionLabel } from '../../core/constants/auction-taxonomy';
import { environment } from '../../../environments/environment';
import { Auction } from '../../core/models/auction.model';
import { AuctionStatus } from '../../core/models/auction-status.type';
import { AuctionApiService } from '../../core/services/auction-api.service';

@Component({
  selector: 'app-auction-list-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    CardModule,
    TagModule,
    ChipModule,
    ButtonModule,
    InputTextModule,
    IconFieldModule,
    InputIconModule,
    PaginatorModule,
    ProgressSpinnerModule,
    SelectModule,
    SelectButtonModule,
    MessageModule,
    CurrencyPipe,
    DatePipe
  ],
  templateUrl: './auction-list.page.html',
  styleUrl: './auction-list.page.scss'
})
export class AuctionListPageComponent implements OnInit {
  private readonly auctionApi = inject(AuctionApiService);

  auctions: Auction[] = [];
  readonly categories = AUCTION_CATEGORIES;
  readonly categoryFilterOptions = [{ code: 'ALL', label: 'All categories' }, ...AUCTION_CATEGORIES];
  readonly statusFilterOptions = [
    { label: 'All', value: 'ALL' },
    { label: 'Live', value: 'RUNNING' },
    { label: 'Upcoming', value: 'DRAFT' },
    { label: 'Closed', value: 'ENDED' }
  ];
  statusFilter: 'ALL' | AuctionStatus = 'ALL';
  categoryFilter = 'ALL';
  searchTerm = '';
  rows = 9;
  loading = false;
  actionLoadingId: number | null = null;
  watchLoadingId: number | null = null;
  errorMessage: string | null = null;
  first = 0;

  ngOnInit(): void {
    this.loadAuctions();
  }

  get totalAuctions(): number {
    return this.auctions.length;
  }

  get liveAuctions(): number {
    return this.auctions.filter((auction) => auction.status === 'RUNNING').length;
  }

  get curatedCategories(): number {
    return new Set(this.auctions.map((auction) => auction.categoryCode)).size;
  }

  get endingSoonCount(): number {
    return this.auctions.filter((auction) => this.endTimeTone(auction) !== 'neutral').length;
  }

  get featuredAuction(): Auction | null {
    const ranked = [...this.filteredAuctions].sort((left, right) => {
      const leftCurated = this.isCuratedAuction(left) ? 1 : 0;
      const rightCurated = this.isCuratedAuction(right) ? 1 : 0;
      const leftRunning = left.status === 'RUNNING' ? 1 : 0;
      const rightRunning = right.status === 'RUNNING' ? 1 : 0;
      const leftHasImage = left.images.length > 0 ? 1 : 0;
      const rightHasImage = right.images.length > 0 ? 1 : 0;

      if (leftCurated !== rightCurated) {
        return rightCurated - leftCurated;
      }

      if (leftRunning !== rightRunning) {
        return rightRunning - leftRunning;
      }

      if (leftHasImage !== rightHasImage) {
        return rightHasImage - leftHasImage;
      }

      return Number(right.currentPrice) - Number(left.currentPrice);
    });

    return ranked[0] ?? null;
  }

  get filteredAuctions(): Auction[] {
    const search = this.searchTerm.trim().toLowerCase();

    return this.auctions
      .filter((auction) => {
      const matchesStatus = this.statusFilter === 'ALL' || auction.status === this.statusFilter;
      const matchesCategory = this.categoryFilter === 'ALL' || auction.categoryCode === this.categoryFilter;
      const matchesSearch =
        !search ||
        auction.title.toLowerCase().includes(search) ||
        auction.description?.toLowerCase().includes(search) ||
        this.categoryLabel(auction.categoryCode).toLowerCase().includes(search) ||
        auction.creatorAuthor?.toLowerCase().includes(search);

        return matchesStatus && matchesCategory && matchesSearch;
      })
      .sort((left, right) => this.auctionRank(right) - this.auctionRank(left) || this.endTimeSort(left, right));
  }

  get pagedAuctions(): Auction[] {
    return this.filteredAuctions.slice(this.first, this.first + this.rows);
  }

  loadAuctions(): void {
    this.loading = true;
    this.errorMessage = null;

    this.auctionApi
      .getAuctions()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (auctions) => {
          this.auctions = [...auctions].sort((left, right) => {
            const leftTime = left.endTime ? new Date(left.endTime).getTime() : 0;
            const rightTime = right.endTime ? new Date(right.endTime).getTime() : 0;
            return leftTime - rightTime;
          });
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load auctions.';
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
      case 'ENDED':
        return 'secondary';
      case 'CANCELLED':
        return 'danger';
      default:
        return 'info';
    }
  }

  statusLabel(status: AuctionStatus): string {
    switch (status) {
      case 'RUNNING':
        return 'Live';
      case 'DRAFT':
        return 'Upcoming';
      case 'ENDED':
        return 'Closed';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  endTimeTone(auction: Auction): 'critical' | 'warning' | 'neutral' {
    if (!auction.endTime || auction.status !== 'RUNNING') {
      return 'neutral';
    }

    const millisUntilEnd = new Date(auction.endTime).getTime() - Date.now();

    if (millisUntilEnd <= 5 * 60 * 1000) {
      return 'critical';
    }

    if (millisUntilEnd <= 60 * 60 * 1000) {
      return 'warning';
    }

    return 'neutral';
  }

  startAuction(auction: Auction): void {
    this.actionLoadingId = auction.id;
    this.errorMessage = null;

    this.auctionApi
      .startAuction(auction.id)
      .pipe(finalize(() => (this.actionLoadingId = null)))
      .subscribe({
        next: (updated) => this.replaceAuction(updated),
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to start the auction.';
        }
      });
  }

  closeAuction(auction: Auction): void {
    this.actionLoadingId = auction.id;
    this.errorMessage = null;

    this.auctionApi
      .closeAuction(auction.id)
      .pipe(finalize(() => (this.actionLoadingId = null)))
      .subscribe({
        next: (updated) => this.replaceAuction(updated),
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to close the auction.';
        }
      });
  }

  toggleWatch(auction: Auction): void {
    this.watchLoadingId = auction.id;
    this.errorMessage = null;

    const request$ = auction.watchedByCurrentUser
      ? this.auctionApi.unwatchAuction(auction.id)
      : this.auctionApi.watchAuction(auction.id);

    request$
      .pipe(finalize(() => (this.watchLoadingId = null)))
      .subscribe({
        next: (updated) => this.replaceAuction(updated),
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to update the watchlist.';
        }
      });
  }

  clearFilters(): void {
    this.statusFilter = 'ALL';
    this.categoryFilter = 'ALL';
    this.searchTerm = '';
    this.first = 0;
  }

  trackAuction(index: number, auction: Auction): number {
    return auction.id;
  }

  onFilterChange(): void {
    this.first = 0;
  }

  onPageChange(event: { first?: number | null }): void {
    this.first = event.first ?? 0;
  }

  private isCuratedAuction(auction: Auction): boolean {
    return !!auction.categoryCode;
  }

  private auctionRank(auction: Auction): number {
    let rank = 0;

    if (this.isCuratedAuction(auction)) {
      rank += 100;
    }

    if (auction.images.length > 0) {
      rank += 25;
    }

    if (auction.status === 'RUNNING') {
      rank += 20;
    } else if (auction.status === 'DRAFT') {
      rank += 10;
    }

    return rank + Math.min(Number(auction.currentPrice) / 100, 20);
  }

  private endTimeSort(left: Auction, right: Auction): number {
    const leftTime = left.endTime ? new Date(left.endTime).getTime() : Number.MAX_SAFE_INTEGER;
    const rightTime = right.endTime ? new Date(right.endTime).getTime() : Number.MAX_SAFE_INTEGER;
    return leftTime - rightTime;
  }

  private replaceAuction(updated: Auction): void {
    this.auctions = this.auctions.map((auction) => (auction.id === updated.id ? updated : auction));
  }

  private resolveImageUrl(imageUrl: string): string {
    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }
}
