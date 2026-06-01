import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { finalize } from 'rxjs';
import { AuctionApiService } from '../../core/services/auction-api.service';

@Component({
  selector: 'app-auction-create-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    InputNumberModule,
    MessageModule,
    ProgressSpinnerModule
  ],
  templateUrl: './auction-create.page.html',
  styleUrl: './auction-create.page.scss'
})
export class AuctionCreatePageComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auctionApi = inject(AuctionApiService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage: string | null = null;

  readonly auctionForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    startPrice: [100, [Validators.required, Validators.min(0.01)]],
    minIncrement: [10, [Validators.required, Validators.min(0.01)]],
    endTime: ['', [Validators.required]],
    antiSnipingWindowSec: [120],
    antiSnipingExtendSec: [30],
    createdBy: [1, [Validators.required, Validators.min(1)]]
  });

  submit(): void {
    if (this.auctionForm.invalid) {
      this.auctionForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const raw = this.auctionForm.getRawValue();

    this.auctionApi
      .createAuction({
        title: raw.title.trim(),
        description: raw.description.trim() || null,
        startPrice: raw.startPrice,
        minIncrement: raw.minIncrement,
        endTime: new Date(raw.endTime).toISOString(),
        antiSnipingWindowSec: raw.antiSnipingWindowSec || null,
        antiSnipingExtendSec: raw.antiSnipingExtendSec || null,
        createdBy: raw.createdBy
      })
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (auction) => {
          void this.router.navigate(['/auctions', auction.id]);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage = error.error?.detail ?? 'Nu am putut crea licitatia.';
        }
      });
  }
}
