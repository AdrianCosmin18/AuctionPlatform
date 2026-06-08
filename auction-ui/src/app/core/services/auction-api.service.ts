import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Auction } from '../models/auction.model';
import { Bid } from '../models/bid.model';
import { CreateAuctionRequest } from '../models/create-auction-request.model';
import { PlaceBidRequest } from '../models/place-bid-request.model';

@Injectable({
  providedIn: 'root'
})
export class AuctionApiService {
  private static readonly CURRENT_USER_ID = 2;
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auctions`;
  private readonly userAwareOptions = {
    headers: {
      'X-User-Id': String(AuctionApiService.CURRENT_USER_ID)
    }
  };

  getAuctions(): Observable<Auction[]> {
    return this.http.get<Auction[]>(this.baseUrl, this.userAwareOptions);
  }

  getAuction(id: number): Observable<Auction> {
    return this.http.get<Auction>(`${this.baseUrl}/${id}`, this.userAwareOptions);
  }

  createAuction(request: CreateAuctionRequest): Observable<Auction> {
    return this.http.post<Auction>(this.baseUrl, request, this.userAwareOptions);
  }

  createAuctionWithImages(request: CreateAuctionRequest, files: File[]): Observable<Auction> {
    const formData = new FormData();
    formData.append('payload', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    for (const file of files) {
      formData.append('images', file, file.name);
    }

    return this.http.post<Auction>(this.baseUrl, formData, this.userAwareOptions);
  }

  updateAuction(id: number, request: CreateAuctionRequest): Observable<Auction> {
    return this.http.put<Auction>(`${this.baseUrl}/${id}`, request, this.userAwareOptions);
  }

  updateAuctionWithImages(id: number, request: CreateAuctionRequest, files: File[]): Observable<Auction> {
    const formData = new FormData();
    formData.append('payload', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    for (const file of files) {
      formData.append('images', file, file.name);
    }

    return this.http.put<Auction>(`${this.baseUrl}/${id}`, formData, this.userAwareOptions);
  }

  startAuction(id: number): Observable<Auction> {
    return this.http.post<Auction>(`${this.baseUrl}/${id}/start`, {}, this.userAwareOptions);
  }

  closeAuction(id: number): Observable<Auction> {
    return this.http.post<Auction>(`${this.baseUrl}/${id}/close`, {}, this.userAwareOptions);
  }

  watchAuction(id: number): Observable<Auction> {
    return this.http.post<Auction>(`${this.baseUrl}/${id}/watch`, {}, this.userAwareOptions);
  }

  unwatchAuction(id: number): Observable<Auction> {
    return this.http.delete<Auction>(`${this.baseUrl}/${id}/watch`, this.userAwareOptions);
  }

  getMyWatchlist(): Observable<Auction[]> {
    return this.http.get<Auction[]>(`${this.baseUrl}/me/watchlist`, this.userAwareOptions);
  }

  getBids(auctionId: number): Observable<Bid[]> {
    return this.http.get<Bid[]>(`${this.baseUrl}/${auctionId}/bids`);
  }

  placeBid(auctionId: number, request: PlaceBidRequest): Observable<Bid> {
    return this.http.post<Bid>(`${this.baseUrl}/${auctionId}/bids`, request);
  }
}
