import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { FileUploadModule } from 'primeng/fileupload';
import { GalleriaModule } from 'primeng/galleria';
import { IconFieldModule } from 'primeng/iconfield';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputIconModule } from 'primeng/inputicon';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { finalize } from 'rxjs';
import { AUTHENTICITY_STATUSES, AUCTION_CATEGORIES, ITEM_CONDITIONS, findCategoryByCode } from '../../core/constants/auction-taxonomy';
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
    FileUploadModule,
    GalleriaModule,
    IconFieldModule,
    InputTextModule,
    InputIconModule,
    InputNumberModule,
    MessageModule,
    ProgressSpinnerModule,
    SelectModule,
    TextareaModule
  ],
  templateUrl: './auction-create.page.html',
  styleUrl: './auction-create.page.scss'
})
export class AuctionCreatePageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly auctionApi = inject(AuctionApiService);
  private readonly router = inject(Router);

  loading = false;
  errorMessage: string | null = null;
  selectedFiles: File[] = [];
  previewImages: { itemImageSrc: string; thumbnailImageSrc: string; alt: string }[] = [];
  readonly categories = AUCTION_CATEGORIES;
  readonly itemConditions = ITEM_CONDITIONS;
  readonly authenticityStatuses = AUTHENTICITY_STATUSES;

  readonly auctionForm = this.fb.nonNullable.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    categoryCode: ['HISTORICAL_DOCUMENTS', [Validators.required]],
    subcategoryCode: ['OFFICIAL_ACTS'],
    creatorAuthor: [''],
    estimatedYear: [1900],
    languageCode: ['Romanian'],
    itemCondition: ['GOOD'],
    authenticityStatus: ['UNVERIFIED'],
    provenance: [''],
    startPrice: [100, [Validators.required, Validators.min(0.01)]],
    minIncrement: [10, [Validators.required, Validators.min(0.01)]],
    endTime: ['', [Validators.required]],
    antiSnipingWindowSec: [120],
    antiSnipingExtendSec: [30],
    createdBy: [1, [Validators.required, Validators.min(1)]]
  });

  constructor() {
    this.auctionForm.controls.categoryCode.valueChanges.subscribe(() => {
      const isCurrentSubcategoryValid = this.availableSubcategories.some(
        (subcategory) => subcategory.code === this.auctionForm.controls.subcategoryCode.value
      );

      if (!isCurrentSubcategoryValid) {
        this.auctionForm.controls.subcategoryCode.setValue(this.availableSubcategories[0]?.code ?? '');
      }
    });
  }

  get selectedCategory() {
    return findCategoryByCode(this.auctionForm.controls.categoryCode.value);
  }

  get availableSubcategories() {
    return this.selectedCategory?.subcategories ?? [];
  }

  ngOnDestroy(): void {
    this.revokePreviewUrls();
  }

  submit(): void {
    if (this.auctionForm.invalid) {
      this.auctionForm.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = null;

    const raw = this.auctionForm.getRawValue();

    this.auctionApi
      .createAuctionWithImages(
        {
          title: raw.title.trim(),
          description: raw.description.trim() || null,
          categoryCode: raw.categoryCode,
          subcategoryCode: raw.subcategoryCode || null,
          creatorAuthor: raw.creatorAuthor.trim() || null,
          estimatedYear: raw.estimatedYear || null,
          languageCode: raw.languageCode.trim() || null,
          itemCondition: raw.itemCondition || null,
          authenticityStatus: raw.authenticityStatus || null,
          provenance: raw.provenance.trim() || null,
          startPrice: raw.startPrice,
          minIncrement: raw.minIncrement,
          endTime: new Date(raw.endTime).toISOString(),
          antiSnipingWindowSec: raw.antiSnipingWindowSec || null,
          antiSnipingExtendSec: raw.antiSnipingExtendSec || null,
          createdBy: raw.createdBy,
          imageUrls: []
        },
        this.selectedFiles
      )
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

  onFilesSelected(event: { files: File[] }): void {
    const incomingFiles = event.files ?? [];
    const nextFiles = [...this.selectedFiles];

    for (const file of incomingFiles) {
      if (nextFiles.length >= 5) {
        break;
      }

      if (!file.type.startsWith('image/')) {
        continue;
      }

      nextFiles.push(file);
    }

    this.selectedFiles = nextFiles;
    this.syncPreviewImages();
  }

  onFileRemoved(event: { file: File }): void {
    this.selectedFiles = this.selectedFiles.filter((file) => file !== event.file);
    this.syncPreviewImages();
  }

  clearSelectedFiles(): void {
    this.selectedFiles = [];
    this.syncPreviewImages();
  }

  private syncPreviewImages(): void {
    this.revokePreviewUrls();
    this.previewImages = this.selectedFiles.map((file) => {
      const objectUrl = URL.createObjectURL(file);
      return {
        itemImageSrc: objectUrl,
        thumbnailImageSrc: objectUrl,
        alt: file.name
      };
    });
  }

  private revokePreviewUrls(): void {
    for (const preview of this.previewImages) {
      URL.revokeObjectURL(preview.itemImageSrc);
    }
  }
}
