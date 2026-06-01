import { AuctionEventType } from './auction-event-type.type';
import { AuctionBusinessEvent } from './auction-business-events.model';

export interface AuctionRealtimeEvent<T = AuctionBusinessEvent> {
  type: AuctionEventType;
  payload: T;
  occurredAt: string;
}
