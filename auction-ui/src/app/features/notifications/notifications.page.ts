import { CommonModule, DatePipe } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { MessageModule } from 'primeng/message';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { TagModule } from 'primeng/tag';
import { finalize } from 'rxjs';
import { Notification } from '../../core/models/notification.model';
import { NotificationType } from '../../core/models/notification-type.type';
import { NotificationApiService } from '../../core/services/notification-api.service';
import { NotificationStoreService } from '../../core/services/notification-store.service';

@Component({
  selector: 'app-notifications-page',
  standalone: true,
  imports: [CommonModule, RouterLink, CardModule, ButtonModule, MessageModule, ProgressSpinnerModule, TagModule, DatePipe],
  templateUrl: './notifications.page.html',
  styleUrl: './notifications.page.scss'
})
export class NotificationsPageComponent implements OnInit {
  private readonly notificationApi = inject(NotificationApiService);
  private readonly notificationStore = inject(NotificationStoreService);

  notifications: Notification[] = [];
  loading = false;
  actionLoadingId: number | 'ALL' | null = null;
  errorMessage: string | null = null;

  ngOnInit(): void {
    this.loadNotifications();
  }

  get unreadCount(): number {
    return this.notifications.filter((notification) => !notification.read).length;
  }

  loadNotifications(): void {
    this.loading = true;
    this.errorMessage = null;

    this.notificationApi
      .getNotifications()
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: (notifications) => {
          this.notifications = notifications;
          this.notificationStore.refreshUnreadCount();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to load notifications.';
        }
      });
  }

  markAsRead(notification: Notification): void {
    if (notification.read) {
      return;
    }

    this.actionLoadingId = notification.id;
    this.notificationApi
      .markAsRead(notification.id)
      .pipe(finalize(() => (this.actionLoadingId = null)))
      .subscribe({
        next: (updated) => {
          this.notifications = this.notifications.map((item) => (item.id === updated.id ? updated : item));
          this.notificationStore.refreshUnreadCount();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to mark the notification as read.';
        }
      });
  }

  markAllAsRead(): void {
    if (this.unreadCount === 0) {
      return;
    }

    this.actionLoadingId = 'ALL';
    this.notificationApi
      .markAllAsRead()
      .pipe(finalize(() => (this.actionLoadingId = null)))
      .subscribe({
        next: () => {
          this.notifications = this.notifications.map((notification) => ({
            ...notification,
            read: true,
            readAt: notification.readAt ?? new Date().toISOString()
          }));
          this.notificationStore.refreshUnreadCount();
        },
        error: (error) => {
          this.errorMessage = error?.error?.detail ?? 'Unable to mark all notifications as read.';
        }
      });
  }

  notificationSeverity(type: NotificationType): 'success' | 'info' | 'warn' | 'danger' | 'secondary' | 'contrast' {
    switch (type) {
      case 'AUCTION_WON':
        return 'success';
      case 'OUTBID':
        return 'danger';
      case 'AUCTION_EXTENDED':
        return 'warn';
      case 'NEW_BID_ON_OWN_AUCTION':
        return 'info';
      default:
        return 'secondary';
    }
  }
}
