import { CommonModule, CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { Auction } from '../../core/models/auction.model';
import { AuctionStatus } from '../../core/models/auction-status.type';
import { AuctionApiService } from '../../core/services/auction-api.service';

@Component({
  selector: 'app-auction-list-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    CardModule,
    TableModule,
    TagModule,
    ButtonModule,
    ProgressSpinnerModule,
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
  loading = false;
  actionLoadingId: number | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadAuctions();
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

  private replaceAuction(updated: Auction): void {
    this.auctions = this.auctions.map((auction) => (auction.id === updated.id ? updated : auction));
  }
}
