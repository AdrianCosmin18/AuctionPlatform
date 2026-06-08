import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { ToastModule } from 'primeng/toast';
import { ToolbarModule } from 'primeng/toolbar';
import { AsyncPipe } from '@angular/common';
import { NotificationStoreService } from './core/services/notification-store.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, ToolbarModule, ButtonModule, ToastModule, AsyncPipe],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  private readonly notificationStore = inject(NotificationStoreService);
  readonly title = 'Auction Platform';
  readonly unreadCount$ = this.notificationStore.unreadCount$;

  constructor() {
    this.notificationStore.startPolling();
  }
}
