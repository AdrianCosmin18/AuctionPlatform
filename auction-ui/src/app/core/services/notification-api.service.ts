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
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/me/notifications`;

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.baseUrl);
  }

  getUnreadCount(): Observable<UnreadNotificationsResponse> {
    return this.http.get<UnreadNotificationsResponse>(`${this.baseUrl}/unread-count`);
  }

  markAsRead(notificationId: number): Observable<Notification> {
    return this.http.post<Notification>(`${this.baseUrl}/${notificationId}/read`, {});
  }

  markAllAsRead(): Observable<UnreadNotificationsResponse> {
    return this.http.post<UnreadNotificationsResponse>(`${this.baseUrl}/read-all`, {});
  }
}
