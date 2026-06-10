import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AnalyticsDashboard } from '../models/analytics-dashboard.model';

@Injectable({
  providedIn: 'root'
})
export class AnalyticsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/analytics`;

  getDashboard(): Observable<AnalyticsDashboard> {
    return this.http.get<AnalyticsDashboard>(`${this.baseUrl}/dashboard`);
  }
}
