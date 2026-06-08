import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { finalize } from 'rxjs';
import { AuctionApiService } from '../../core/services/auction-api.service';
import { AuctionFormComponent, AuctionFormSubmitEvent } from '../auction-form/auction-form.component';

@Component({
  selector: 'app-auction-create-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, MessageModule, AuctionFormComponent],
  templateUrl: './auction-create.page.html',
  styleUrl: './auction-create.page.scss'
})
export class AuctionCreatePageComponent {
  private readonly auctionApi = inject(AuctionApiService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage: string | null = null;

  submit(event: AuctionFormSubmitEvent): void {
    this.loading = true;
    this.errorMessage = null;

    this.auctionApi
      .createAuctionWithImages(event.payload, event.files)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (auction) => {
          void this.router.navigate(['/auctions', auction.id]);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = error.error?.detail ?? 'Unable to create the auction.';
        }
      });
  }
}
