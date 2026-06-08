import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { finalize } from 'rxjs';
import { Auction } from '../../core/models/auction.model';
import { AuctionApiService } from '../../core/services/auction-api.service';
import { AuctionFormComponent, AuctionFormSubmitEvent } from '../auction-form/auction-form.component';

@Component({
  selector: 'app-auction-edit-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, MessageModule, ProgressSpinnerModule, AuctionFormComponent],
  templateUrl: './auction-edit.page.html',
  styleUrl: './auction-edit.page.scss'
})
export class AuctionEditPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly auctionApi = inject(AuctionApiService);
  private readonly router = inject(Router);

  auction: Auction | null = null;
  loading = true;
  saving = false;
  errorMessage: string | null = null;

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    if (!Number.isFinite(id) || id <= 0) {
      this.loading = false;
      this.errorMessage = 'The auction id is invalid.';
      return;
    }

    this.auctionApi
      .getAuction(id)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (auction) => {
          if (auction.status !== 'DRAFT') {
            this.errorMessage = 'Only DRAFT auctions can be edited.';
            this.auction = auction;
            return;
          }

          this.auction = auction;
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = error.error?.detail ?? 'Unable to load the auction for editing.';
        }
      });
  }

  submit(event: AuctionFormSubmitEvent): void {
    if (!this.auction) {
      return;
    }

    this.saving = true;
    this.errorMessage = null;

    this.auctionApi
      .updateAuctionWithImages(this.auction.id, event.payload, event.files)
      .pipe(finalize(() => (this.saving = false)))
      .subscribe({
        next: (auction) => {
          void this.router.navigate(['/auctions', auction.id]);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = error.error?.detail ?? 'Unable to update the auction.';
        }
      });
  }
}
