import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { AUCTION_CATEGORIES, AUTHENTICITY_STATUSES, ITEM_CONDITIONS, findCategoryByCode, findOptionLabel } from '../../core/constants/auction-taxonomy';
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
    TableModule,
    TagModule,
    ButtonModule,
    InputTextModule,
    ProgressSpinnerModule,
    SelectModule,
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
  readonly categoryFilterOptions = [{ code: 'ALL', label: 'Toate categoriile' }, ...AUCTION_CATEGORIES];
  statusFilter: 'ALL' | AuctionStatus = 'ALL';
  categoryFilter = 'ALL';
  searchTerm = '';
  loading = false;
  actionLoadingId: number | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadAuctions();
  }

  get totalAuctions(): number {
    return this.auctions.length;
  }

  get runningAuctions(): number {
    return this.auctions.filter((auction) => auction.status === 'RUNNING').length;
  }

  get draftAuctions(): number {
    return this.auctions.filter((auction) => auction.status === 'DRAFT').length;
  }

  get endedAuctions(): number {
    return this.auctions.filter((auction) => auction.status === 'ENDED').length;
  }

  get highestCurrentPrice(): number {
    return this.auctions.reduce((max, auction) => Math.max(max, Number(auction.currentPrice)), 0);
  }

  get featuredAuction(): Auction | null {
    const running = this.auctions
      .filter((auction) => auction.status === 'RUNNING' && auction.endTime)
      .sort((left, right) => new Date(left.endTime as string).getTime() - new Date(right.endTime as string).getTime());

    return running[0] ?? this.auctions[0] ?? null;
  }

  primaryImage(auction: Auction): string | null {
    return auction.images[0] ? this.resolveImageUrl(auction.images[0].imageUrl) : null;
  }

  categoryLabel(code: string | null | undefined): string {
    return findCategoryByCode(code)?.label ?? 'Necategorizat';
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

  get filteredAuctions(): Auction[] {
    const search = this.searchTerm.trim().toLowerCase();

    return this.auctions.filter((auction) => {
      const matchesStatus = this.statusFilter === 'ALL' || auction.status === this.statusFilter;
      const matchesCategory = this.categoryFilter === 'ALL' || auction.categoryCode === this.categoryFilter;
      const matchesSearch =
        !search ||
        auction.title.toLowerCase().includes(search) ||
        auction.description?.toLowerCase().includes(search) ||
        this.categoryLabel(auction.categoryCode).toLowerCase().includes(search) ||
        auction.creatorAuthor?.toLowerCase().includes(search);

      return matchesStatus && matchesCategory && matchesSearch;
    });
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
            return rightTime - leftTime;
          });
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Nu am putut incarca licitatiile.';
        }
      });
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
          this.errorMessage = error?.error?.detail ?? 'Pornirea licitatiei a esuat.';
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
          this.errorMessage = error?.error?.detail ?? 'Inchiderea licitatiei a esuat.';
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

  endTimeTone(auction: Auction): 'critical' | 'warning' | 'neutral' {
    if (!auction.endTime || auction.status !== 'RUNNING') {
      return 'neutral';
    }

    const millisUntilEnd = new Date(auction.endTime).getTime() - Date.now();

    if (millisUntilEnd <= 5 * 60 * 1000) {
      return 'critical';
    }

    if (millisUntilEnd <= 30 * 60 * 1000) {
      return 'warning';
    }

    return 'neutral';
  }

  trackAuction(index: number, auction: Auction): number {
    return auction.id;
  }

  setStatusFilter(status: 'ALL' | AuctionStatus): void {
    this.statusFilter = status;
  }

  clearFilters(): void {
    this.statusFilter = 'ALL';
    this.categoryFilter = 'ALL';
    this.searchTerm = '';
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
