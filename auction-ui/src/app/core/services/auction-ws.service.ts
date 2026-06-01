import { Injectable } from '@angular/core';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuctionRealtimeEvent } from '../models/auction-realtime-event.model';

@Injectable({
  providedIn: 'root'
})
export class AuctionWsService {
  watchAuction(auctionId: number): Observable<AuctionRealtimeEvent> {
    return new Observable<AuctionRealtimeEvent>((observer) => {
      let subscription: StompSubscription | undefined;

      const client = new Client({
        reconnectDelay: 5000,
        webSocketFactory: () => new SockJS(`${environment.wsBaseUrl}/ws`) as WebSocket
      });

      client.onConnect = () => {
        subscription = client.subscribe(`/topic/auctions/${auctionId}`, (message: IMessage) => {
          observer.next(JSON.parse(message.body) as AuctionRealtimeEvent);
        });
      };

      client.onStompError = (frame) => {
        observer.error(new Error(frame.headers['message'] ?? 'WebSocket broker error'));
      };

      client.onWebSocketError = () => {
        observer.error(new Error('WebSocket connection error'));
      };

      void client.activate();

      return () => {
        subscription?.unsubscribe();
        void client.deactivate();
      };
    });
  }
}
