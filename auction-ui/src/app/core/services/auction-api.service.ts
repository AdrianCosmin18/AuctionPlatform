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
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/auctions`;

  getAuctions(): Observable<Auction[]> {
    return this.http.get<Auction[]>(this.baseUrl);
  }

  getAuction(id: number): Observable<Auction> {
    return this.http.get<Auction>(`${this.baseUrl}/${id}`);
  }

  createAuction(request: CreateAuctionRequest): Observable<Auction> {
    return this.http.post<Auction>(this.baseUrl, request);
  }

  createAuctionWithImages(request: CreateAuctionRequest, files: File[]): Observable<Auction> {
    const formData = new FormData();
    formData.append('payload', new Blob([JSON.stringify(request)], { type: 'application/json' }));

    for (const file of files) {
      formData.append('images', file, file.name);
    }

    return this.http.post<Auction>(this.baseUrl, formData);
  }

  startAuction(id: number): Observable<Auction> {
    return this.http.post<Auction>(`${this.baseUrl}/${id}/start`, {});
  }

  closeAuction(id: number): Observable<Auction> {
    return this.http.post<Auction>(`${this.baseUrl}/${id}/close`, {});
  }

  getBids(auctionId: number): Observable<Bid[]> {
    return this.http.get<Bid[]>(`${this.baseUrl}/${auctionId}/bids`);
  }

  placeBid(auctionId: number, request: PlaceBidRequest): Observable<Bid> {
    return this.http.post<Bid>(`${this.baseUrl}/${auctionId}/bids`, request);
  }
}
