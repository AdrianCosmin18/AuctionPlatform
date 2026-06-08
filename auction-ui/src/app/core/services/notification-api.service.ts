import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Notification } from '../models/notification.model';
import { UnreadNotificationsResponse } from '../models/unread-notifications-response.model';

@Injectable({
  providedIn: 'root'
})
export class NotificationApiService {
  private static readonly CURRENT_USER_ID = 2;
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/me/notifications`;
  private readonly userAwareOptions = {
    headers: {
      'X-User-Id': String(NotificationApiService.CURRENT_USER_ID)
    }
  };

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.baseUrl, this.userAwareOptions);
  }

  getUnreadCount(): Observable<UnreadNotificationsResponse> {
    return this.http.get<UnreadNotificationsResponse>(`${this.baseUrl}/unread-count`, this.userAwareOptions);
  }

  markAsRead(notificationId: number): Observable<Notification> {
    return this.http.post<Notification>(`${this.baseUrl}/${notificationId}/read`, {}, this.userAwareOptions);
  }

  markAllAsRead(): Observable<UnreadNotificationsResponse> {
    return this.http.post<UnreadNotificationsResponse>(`${this.baseUrl}/read-all`, {}, this.userAwareOptions);
  }
}
