import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { FileUploadModule } from 'primeng/fileupload';
import { GalleriaModule } from 'primeng/galleria';
import { InputNumberModule } from 'primeng/inputnumber';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { TextareaModule } from 'primeng/textarea';
import { AUTHENTICITY_STATUSES, AUCTION_CATEGORIES, ITEM_CONDITIONS, findCategoryByCode } from '../../core/constants/auction-taxonomy';
import { Auction } from '../../core/models/auction.model';
import { CreateAuctionRequest } from '../../core/models/create-auction-request.model';
import { environment } from '../../../environments/environment';

export interface AuctionFormSubmitEvent {
  payload: CreateAuctionRequest;
  files: File[];
}

interface PreviewImage {
  itemImageSrc: string;
  thumbnailImageSrc: string;
  alt: string;
}

@Component({
  selector: 'app-auction-form',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    ButtonModule,
    FileUploadModule,
    GalleriaModule,
    InputTextModule,
    InputNumberModule,
    SelectModule,
    TextareaModule
  ],
  templateUrl: './auction-form.component.html',
  styleUrl: './auction-form.component.scss'
})
export class AuctionFormComponent implements OnChanges, OnDestroy {
  private readonly fb = new FormBuilder();

  @Input() mode: 'create' | 'edit' = 'create';
  @Input() loading = false;
  @Input() initialAuction: Auction | null = null;
  @Input() submitLabel = 'Create auction';

  @Output() submitted = new EventEmitter<AuctionFormSubmitEvent>();

  selectedFiles: File[] = [];
  previewImages: PreviewImage[] = [];
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
    reservePrice: [null as number | null],
    buyNowPrice: [null as number | null],
    endTime: ['', [Validators.required]],
    antiSnipingWindowSec: [120],
    antiSnipingExtendSec: [30],
    createdBy: [1, [Validators.required, Validators.min(1)]]
  }, { validators: [this.reservePriceValidator(), this.buyNowPriceValidator()] });

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

  get existingImages(): PreviewImage[] {
    return (this.initialAuction?.images ?? []).map((image, index) => ({
      itemImageSrc: this.imageSrc(image.imageUrl),
      thumbnailImageSrc: this.imageSrc(image.imageUrl),
      alt: `${this.initialAuction?.title ?? 'Auction'} image ${index + 1}`
    }));
  }

  get remainingImageSlots(): number {
    return Math.max(0, 5 - this.existingImages.length - this.selectedFiles.length);
  }

  get isEditMode(): boolean {
    return this.mode === 'edit';
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['initialAuction'] && this.initialAuction) {
      this.auctionForm.reset(
        {
          title: this.initialAuction.title,
          description: this.initialAuction.description ?? '',
          categoryCode: this.initialAuction.categoryCode,
          subcategoryCode: this.initialAuction.subcategoryCode ?? '',
          creatorAuthor: this.initialAuction.creatorAuthor ?? '',
          estimatedYear: this.initialAuction.estimatedYear ?? 1900,
          languageCode: this.initialAuction.languageCode ?? 'Romanian',
          itemCondition: this.initialAuction.itemCondition ?? 'GOOD',
          authenticityStatus: this.initialAuction.authenticityStatus ?? 'UNVERIFIED',
          provenance: this.initialAuction.provenance ?? '',
          startPrice: this.initialAuction.startPrice,
          minIncrement: this.initialAuction.minIncrement,
          reservePrice: this.initialAuction.reservePrice,
          buyNowPrice: this.initialAuction.buyNowPrice,
          endTime: this.toDateTimeLocalValue(this.initialAuction.endTime),
          antiSnipingWindowSec: this.initialAuction.antiSnipingWindowSec ?? 120,
          antiSnipingExtendSec: this.initialAuction.antiSnipingExtendSec ?? 30,
          createdBy: this.initialAuction.createdBy
        },
        { emitEvent: false }
      );
    }
  }

  ngOnDestroy(): void {
    this.revokePreviewUrls();
  }

  submit(): void {
    if (this.auctionForm.invalid) {
      this.auctionForm.markAllAsTouched();
      return;
    }

    const raw = this.auctionForm.getRawValue();

    this.submitted.emit({
      payload: {
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
        reservePrice: raw.reservePrice,
        buyNowPrice: raw.buyNowPrice,
        endTime: new Date(raw.endTime).toISOString(),
        antiSnipingWindowSec: raw.antiSnipingWindowSec || null,
        antiSnipingExtendSec: raw.antiSnipingExtendSec || null,
        createdBy: raw.createdBy,
        imageUrls: []
      },
      files: [...this.selectedFiles]
    });
  }

  onFilesSelected(event: { files: File[] }): void {
    const incomingFiles = event.files ?? [];
    const nextFiles = [...this.selectedFiles];

    for (const file of incomingFiles) {
      if (this.existingImages.length + nextFiles.length >= 5) {
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

  private toDateTimeLocalValue(value: string | null): string {
    if (!value) {
      return '';
    }

    const date = new Date(value);
    const offsetMs = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offsetMs).toISOString().slice(0, 16);
  }

  private imageSrc(imageUrl: string): string {
    return imageUrl.startsWith('http://') || imageUrl.startsWith('https://')
      ? imageUrl
      : `${environment.wsBaseUrl}${imageUrl}`;
  }

  private reservePriceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startPrice = Number(control.get('startPrice')?.value);
      const reservePriceValue = control.get('reservePrice')?.value;

      if (reservePriceValue === null || reservePriceValue === undefined || reservePriceValue === '') {
        return null;
      }

      const reservePrice = Number(reservePriceValue);
      if (!Number.isFinite(startPrice) || !Number.isFinite(reservePrice)) {
        return null;
      }

      return reservePrice >= startPrice
        ? null
        : { reservePriceBelowStartPrice: true };
    };
  }

  private buyNowPriceValidator(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const startPrice = Number(control.get('startPrice')?.value);
      const reservePriceValue = control.get('reservePrice')?.value;
      const buyNowPriceValue = control.get('buyNowPrice')?.value;

      if (buyNowPriceValue === null || buyNowPriceValue === undefined || buyNowPriceValue === '') {
        return null;
      }

      const buyNowPrice = Number(buyNowPriceValue);
      const reservePrice = reservePriceValue === null || reservePriceValue === undefined || reservePriceValue === ''
        ? null
        : Number(reservePriceValue);

      if (!Number.isFinite(startPrice) || !Number.isFinite(buyNowPrice)) {
        return null;
      }

      if (buyNowPrice <= startPrice) {
        return { buyNowPriceBelowOrEqualStartPrice: true };
      }

      if (reservePrice !== null && Number.isFinite(reservePrice) && buyNowPrice < reservePrice) {
        return { buyNowPriceBelowReservePrice: true };
      }

      return null;
    };
  }
}
