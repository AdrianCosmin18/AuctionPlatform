import { AsyncPipe, CommonModule } from '@angular/common';
import { Component, ViewChild, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { MenuItem } from 'primeng/api';
import { AvatarModule } from 'primeng/avatar';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { DialogModule } from 'primeng/dialog';
import { DividerModule } from 'primeng/divider';
import { InputTextModule } from 'primeng/inputtext';
import { Menu } from 'primeng/menu';
import { MenuModule } from 'primeng/menu';
import { MessageModule } from 'primeng/message';
import { PasswordModule } from 'primeng/password';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { AuthService } from './core/services/auth.service';
import { AuthenticatedUser } from './core/models/authenticated-user.model';
import { UserProfile } from './core/models/user-profile.model';
import { NotificationStoreService } from './core/services/notification-store.service';
import { ProfileApiService } from './core/services/profile-api.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterOutlet,
    RouterLink,
    ToolbarModule,
    ButtonModule,
    ToastModule,
    ConfirmDialogModule,
    AsyncPipe,
    AvatarModule,
    MenuModule,
    DialogModule,
    PasswordModule,
    ReactiveFormsModule,
    MessageModule,
    DividerModule,
    InputTextModule
  ],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  private readonly authService = inject(AuthService);
  private readonly notificationStore = inject(NotificationStoreService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly profileApiService = inject(ProfileApiService);

  @ViewChild('userMenu') userMenu?: Menu;

  readonly title = 'Auction Platform';
  readonly unreadCount$ = this.notificationStore.unreadCount$;
  readonly currentUser$ = this.authService.currentUser$;
  profileDialogVisible = false;
  passwordDialogVisible = false;
  profileLoading = false;
  profileSaving = false;
  profileError: string | null = null;
  profileSuccess: string | null = null;
  passwordSaving = false;
  passwordError: string | null = null;
  passwordSuccess: string | null = null;
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

  readonly passwordForm = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  constructor() {
    this.notificationStore.startPolling();
  }

  logout(): void {
    this.authService.logout();
  }

  userMenuItems(currentUser: AuthenticatedUser): MenuItem[] {
    const items: MenuItem[] = [
      {
        label: this.displayName(currentUser),
        disabled: true,
        styleClass: 'user-menu__headline'
      },
      {
        label: currentUser.email,
        disabled: true,
        styleClass: 'user-menu__subline'
      },
      {
        separator: true
      },
      {
        label: 'My Profile',
        icon: 'pi pi-user',
        command: () => this.openProfileDialog()
      },
      {
        label: 'Change Password',
        icon: 'pi pi-lock',
        command: () => this.openPasswordDialog()
      },
      {
        label: 'My Activity',
        icon: 'pi pi-briefcase',
        command: () => void this.router.navigate(['/my-auctions'])
      },
      {
        label: 'Notifications',
        icon: 'pi pi-bell',
        command: () => void this.router.navigate(['/notifications'])
      }
    ];

    if (currentUser.role === 'ADMIN') {
      items.push(
        {
          label: 'Dashboard',
          icon: 'pi pi-chart-bar',
          command: () => void this.router.navigate(['/dashboard'])
        },
        {
          label: 'Fraud Signals',
          icon: 'pi pi-shield',
          command: () => void this.router.navigate(['/fraud-signals'])
        }
      );
    }

    items.push(
      {
        separator: true
      },
      {
        label: 'Logout',
        icon: 'pi pi-sign-out',
        command: () => this.logout()
      }
    );

    return items;
  }

  avatarLabel(currentUser: AuthenticatedUser): string {
    const firstInitial = currentUser.firstName?.trim().charAt(0) ?? '';
    const lastInitial = currentUser.lastName?.trim().charAt(0) ?? '';
    const initials = `${firstInitial}${lastInitial}`.trim().toUpperCase();
    return initials || currentUser.email.charAt(0).toUpperCase();
  }

  displayName(currentUser: AuthenticatedUser): string {
    const fullName = `${currentUser.firstName ?? ''} ${currentUser.lastName ?? ''}`.trim();
    return fullName || 'My account';
  }

  openPasswordDialog(): void {
    this.passwordDialogVisible = true;
    this.passwordError = null;
    this.passwordSuccess = null;
    this.passwordForm.reset();
  }

  openProfileDialog(): void {
    this.profileDialogVisible = true;
    this.profileError = null;
    this.profileSuccess = null;
    this.loadProfile();
  }

  closeProfileDialog(): void {
    this.profileDialogVisible = false;
    this.profileError = null;
    this.profileSuccess = null;
  }

  closePasswordDialog(): void {
    this.passwordDialogVisible = false;
    this.passwordError = null;
    this.passwordSuccess = null;
    this.passwordForm.reset();
  }

  passwordMismatch(): boolean {
    return this.passwordForm.controls.newPassword.value !== this.passwordForm.controls.confirmPassword.value;
  }

  submitPasswordChange(): void {
    if (this.passwordForm.invalid || this.passwordMismatch()) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    this.passwordSaving = true;
    this.passwordError = null;
    this.passwordSuccess = null;

    this.profileApiService.changePassword(this.passwordForm.getRawValue())
      .pipe(finalize(() => (this.passwordSaving = false)))
      .subscribe({
        next: () => {
          this.passwordSuccess = 'Password changed successfully.';
          this.passwordForm.reset();
        },
        error: (error) => {
          this.passwordError = error?.error?.detail ?? 'Unable to change password.';
        }
      });
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
    this.profileLoading = true;
    this.profileApiService.getProfile()
      .pipe(finalize(() => (this.profileLoading = false)))
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

  private normalize(value: string): string | null {
    const trimmed = value.trim();
    return trimmed ? trimmed : null;
  }
}
