import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { FraudOverview } from '../models/fraud-overview.model';

@Injectable({
  providedIn: 'root'
})
export class FraudApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/fraud`;

  getSignals(): Observable<FraudOverview> {
    return this.http.get<FraudOverview>(`${this.baseUrl}/signals`);
  }
}
