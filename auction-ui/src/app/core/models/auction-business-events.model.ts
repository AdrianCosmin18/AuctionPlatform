import { AuctionCloseReason } from './auction-close-reason.type';

export interface BidPlacedEvent {
  auctionId: number;
  bidId: number;
  bidderId: number;
  amount: number;
  currentPrice: number;
  occurredAt: string;
}

export interface AuctionExtendedEvent {
  auctionId: number;
  newEndTime: string;
  occurredAt: string;
}

export interface AuctionClosedEvent {
  auctionId: number;
  winnerId: number | null;
  winningBidId: number | null;
  finalPrice: number;
  reserveMet: boolean | null;
  closedReason: AuctionCloseReason;
  closedAt: string;
}

export type AuctionBusinessEvent = BidPlacedEvent | AuctionExtendedEvent | AuctionClosedEvent;
