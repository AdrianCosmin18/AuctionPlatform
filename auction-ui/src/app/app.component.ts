import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { AsyncPipe } from '@angular/common';
import { AuthService } from './core/services/auth.service';
import { NotificationStoreService } from './core/services/notification-store.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, ToolbarModule, ButtonModule, ToastModule, ConfirmDialogModule, AsyncPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  private readonly authService = inject(AuthService);
  private readonly notificationStore = inject(NotificationStoreService);
  readonly title = 'Auction Platform';
  readonly unreadCount$ = this.notificationStore.unreadCount$;
  readonly currentUser$ = this.authService.currentUser$;

  constructor() {
    this.notificationStore.startPolling();
  }

  logout(): void {
    this.authService.logout();
  }
}
