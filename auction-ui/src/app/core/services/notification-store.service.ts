import { Injectable, OnDestroy, inject } from '@angular/core';
import { MessageService } from 'primeng/api';
import { BehaviorSubject, Subscription, interval, startWith, switchMap } from 'rxjs';
import { Notification } from '../models/notification.model';
import { AuthService } from './auth.service';
import { NotificationApiService } from './notification-api.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationStoreService implements OnDestroy {
  private readonly notificationApi = inject(NotificationApiService);
  private readonly messageService = inject(MessageService);
  private readonly authService = inject(AuthService);
  private pollSubscription?: Subscription;
  private authSubscription?: Subscription;
  private readonly announcedWinningNotificationIds = new Set<number>();
  private initializedWinningNotifications = false;

  readonly unreadCount$ = new BehaviorSubject<number>(0);

  startPolling(): void {
    if (this.authSubscription) {
      return;
    }

    this.authSubscription = this.authService.isAuthenticated$.subscribe((isAuthenticated) => {
      if (isAuthenticated) {
        this.beginPolling();
        return;
      }

      this.pollSubscription?.unsubscribe();
      this.pollSubscription = undefined;
      this.unreadCount$.next(0);
      this.announcedWinningNotificationIds.clear();
      this.initializedWinningNotifications = false;
    });
  }

  refreshUnreadCount(): void {
    this.notificationApi.getUnreadCount().subscribe({
      next: (response) => this.unreadCount$.next(response.unreadCount)
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
    this.authSubscription?.unsubscribe();
  }

  private beginPolling(): void {
    if (this.pollSubscription) {
      return;
    }

    this.pollSubscription = interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.notificationApi.getNotifications())
      )
      .subscribe({
        next: (notifications) => {
          this.unreadCount$.next(notifications.filter((notification) => !notification.read).length);
          this.announceWinningNotifications(notifications);
        },
        error: () => this.unreadCount$.next(this.unreadCount$.value)
      });
  }

  private announceWinningNotifications(notifications: Notification[]): void {
    const winningNotifications = notifications
      .filter((notification) => notification.type === 'AUCTION_WON')
      .sort((left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime());

    if (!this.initializedWinningNotifications) {
      winningNotifications.forEach((notification) => this.announcedWinningNotificationIds.add(notification.id));
      this.initializedWinningNotifications = true;
      return;
    }

    for (const notification of winningNotifications) {
      if (this.announcedWinningNotificationIds.has(notification.id)) {
        continue;
      }

      this.announcedWinningNotificationIds.add(notification.id);
      this.messageService.add({
        key: 'global-notifications',
        severity: 'success',
        summary: 'Auction won',
        detail: notification.message,
        life: 9000
      });
    }
  }
}
