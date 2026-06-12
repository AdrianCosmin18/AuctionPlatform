import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { InputTextModule } from 'primeng/inputtext';
import { MessageModule } from 'primeng/message';
import { finalize } from 'rxjs';
import { UserProfile } from '../../core/models/user-profile.model';
import { ProfileApiService } from '../../core/services/profile-api.service';

@Component({
  selector: 'app-profile-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    CardModule,
    ButtonModule,
    InputTextModule,
    MessageModule
  ],
  templateUrl: './profile.page.html',
  styleUrl: './profile.page.scss'
})
export class ProfilePageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profileApiService = inject(ProfileApiService);

  loading = true;
  profileSaving = false;
  profileError: string | null = null;
  profileSuccess: string | null = null;
  private initialProfileSnapshot: string | null = null;

  readonly profileForm = this.fb.nonNullable.group({
    email: [{ value: '', disabled: true }],
    firstName: ['', [Validators.maxLength(100)]],
    lastName: ['', [Validators.maxLength(100)]],
    phone: ['', [Validators.maxLength(50)]],
    country: ['', [Validators.maxLength(100)]],
    city: ['', [Validators.maxLength(100)]],
    addressLine1: ['', [Validators.maxLength(255)]],
    addressLine2: ['', [Validators.maxLength(255)]],
    postalCode: ['', [Validators.maxLength(50)]]
  });

  ngOnInit(): void {
    this.loadProfile();
  }

  saveProfile(): void {
    if (this.profileForm.invalid || !this.hasProfileChanges()) {
      this.profileForm.markAllAsTouched();
      return;
    }

    this.profileSaving = true;
    this.profileError = null;
    this.profileSuccess = null;

    this.profileApiService.updateProfile({
      firstName: this.normalize(this.profileForm.controls.firstName.value),
      lastName: this.normalize(this.profileForm.controls.lastName.value),
      phone: this.normalize(this.profileForm.controls.phone.value),
      country: this.normalize(this.profileForm.controls.country.value),
      city: this.normalize(this.profileForm.controls.city.value),
      addressLine1: this.normalize(this.profileForm.controls.addressLine1.value),
      addressLine2: this.normalize(this.profileForm.controls.addressLine2.value),
      postalCode: this.normalize(this.profileForm.controls.postalCode.value)
    })
      .pipe(finalize(() => (this.profileSaving = false)))
      .subscribe({
        next: (profile) => {
          this.patchProfile(profile);
          this.profileSuccess = 'Profile updated successfully.';
        },
        error: (error) => {
          this.profileError = error?.error?.detail ?? 'Unable to update profile.';
        }
      });
  }

  hasProfileChanges(): boolean {
    return this.initialProfileSnapshot !== this.profileSnapshot(this.currentProfileValue());
  }

  private loadProfile(): void {
    this.loading = true;
    this.profileApiService.getProfile()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (profile) => this.patchProfile(profile),
        error: (error) => {
          this.profileError = error?.error?.detail ?? 'Unable to load profile.';
        }
      });
  }

  private patchProfile(profile: UserProfile): void {
    this.profileForm.patchValue({
      email: profile.email,
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      phone: profile.phone ?? '',
      country: profile.country ?? '',
      city: profile.city ?? '',
      addressLine1: profile.addressLine1 ?? '',
      addressLine2: profile.addressLine2 ?? '',
      postalCode: profile.postalCode ?? ''
    });
    this.initialProfileSnapshot = this.profileSnapshot({
      firstName: profile.firstName ?? '',
      lastName: profile.lastName ?? '',
      phone: profile.phone ?? '',
      country: profile.country ?? '',
      city: profile.city ?? '',
      addressLine1: profile.addressLine1 ?? '',
      addressLine2: profile.addressLine2 ?? '',
      postalCode: profile.postalCode ?? ''
    });
    this.profileForm.markAsPristine();
  }

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }

  private currentProfileValue() {
    return {
      firstName: this.profileForm.controls.firstName.value,
      lastName: this.profileForm.controls.lastName.value,
      phone: this.profileForm.controls.phone.value,
      country: this.profileForm.controls.country.value,
      city: this.profileForm.controls.city.value,
      addressLine1: this.profileForm.controls.addressLine1.value,
      addressLine2: this.profileForm.controls.addressLine2.value,
      postalCode: this.profileForm.controls.postalCode.value
    };
  }

  private profileSnapshot(value: {
    firstName: string;
    lastName: string;
    phone: string;
    country: string;
    city: string;
    addressLine1: string;
    addressLine2: string;
    postalCode: string;
  }): string {
    return JSON.stringify({
      firstName: this.normalize(value.firstName),
      lastName: this.normalize(value.lastName),
      phone: this.normalize(value.phone),
      country: this.normalize(value.country),
      city: this.normalize(value.city),
      addressLine1: this.normalize(value.addressLine1),
      addressLine2: this.normalize(value.addressLine2),
      postalCode: this.normalize(value.postalCode)
    });
  }
}
