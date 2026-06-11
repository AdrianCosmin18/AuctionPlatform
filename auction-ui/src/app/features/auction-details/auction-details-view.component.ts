import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, Input, OnDestroy, OnInit } from '@angular/core';
import { DividerModule } from 'primeng/divider';
import { GalleriaModule } from 'primeng/galleria';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { AUTHENTICITY_STATUSES, ITEM_CONDITIONS, findCategoryByCode, findOptionLabel } from '../../core/constants/auction-taxonomy';
import { environment } from '../../../environments/environment';
import { Auction } from '../../core/models/auction.model';
import { AuctionStatus } from '../../core/models/auction-status.type';
import { Bid } from '../../core/models/bid.model';

@Component({
  selector: 'app-auction-details-view',
  standalone: true,
  imports: [CommonModule, DividerModule, GalleriaModule, TableModule, TagModule, MessageModule, ProgressSpinnerModule, CurrencyPipe, DatePipe],
  templateUrl: './auction-details-view.component.html',
  styleUrl: './auction-details-view.component.scss'
})
export class AuctionDetailsViewComponent implements OnInit, OnDestroy {
  @Input() auction: Auction | null = null;
  @Input() bids: Bid[] = [];
  @Input() loading = false;
  @Input() insideDialog = false;

  selectedImageIndex = 0;
  now = Date.now();
  private timerId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.timerId = setInterval(() => {
      this.now = Date.now();
    }, 1000);
  }

  ngOnDestroy(): void {
    if (this.timerId) {
      clearInterval(this.timerId);
      this.timerId = null;
    }
  }

  ngOnChanges(): void {
    this.selectedImageIndex = 0;
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

  suspensionLabel(auction: Auction | null): string | null {
    if (!auction?.suspensionReason) {
      return null;
    }

    return `Suspended by admin #${auction.suspendedBy ?? '-'}: ${auction.suspensionReason}`;
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

  imageSrc(imageUrl: string): string {
    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }

  selectImage(index: number): void {
    this.selectedImageIndex = index;
  }

  galleryResponsiveOptions = [
    {
      breakpoint: '1200px',
      numVisible: 5
    },
    {
      breakpoint: '992px',
      numVisible: 4
    },
    {
      breakpoint: '768px',
      numVisible: 3
    },
    {
      breakpoint: '576px',
      numVisible: 2
    }
  ];

  remainingTime(auction: Auction | null):
    | {
        days: number;
        hours: number;
        minutes: number;
        seconds: number;
        expired: boolean;
      }
    | null {
    if (!auction?.endTime) {
      return null;
    }

    const diffMs = new Date(auction.endTime).getTime() - this.now;
    const totalSeconds = Math.max(0, Math.floor(diffMs / 1000));

    return {
      days: Math.floor(totalSeconds / 86400),
      hours: Math.floor((totalSeconds % 86400) / 3600),
      minutes: Math.floor((totalSeconds % 3600) / 60),
      seconds: totalSeconds % 60,
      expired: totalSeconds === 0
    };
  }
}
