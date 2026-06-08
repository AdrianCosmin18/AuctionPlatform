import { Injectable, OnDestroy, inject } from '@angular/core';
import { BehaviorSubject, Subscription, interval, startWith, switchMap } from 'rxjs';
import { NotificationApiService } from './notification-api.service';

@Injectable({
  providedIn: 'root'
})
export class NotificationStoreService implements OnDestroy {
  private readonly notificationApi = inject(NotificationApiService);
  private pollSubscription?: Subscription;

  readonly unreadCount$ = new BehaviorSubject<number>(0);

  startPolling(): void {
    if (this.pollSubscription) {
      return;
    }

    this.pollSubscription = interval(15000)
      .pipe(
        startWith(0),
        switchMap(() => this.notificationApi.getUnreadCount())
      )
      .subscribe({
        next: (response) => this.unreadCount$.next(response.unreadCount),
        error: () => this.unreadCount$.next(this.unreadCount$.value)
      });
  }

  refreshUnreadCount(): void {
    this.notificationApi.getUnreadCount().subscribe({
      next: (response) => this.unreadCount$.next(response.unreadCount)
    });
  }

  ngOnDestroy(): void {
    this.pollSubscription?.unsubscribe();
  }
}
