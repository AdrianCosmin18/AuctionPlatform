import { AuctionCloseReason } from './auction-close-reason.type';

export interface AuctionStartedEvent {
  auctionId: number;
  startedAt: string;
}

export interface BidPlacedEvent {
  auctionId: number;
  bidId: number;
  bidderId: number;
  bidderDisplayName: string;
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

export interface AuctionSuspendedEvent {
  auctionId: number;
  suspendedBy: number;
  reason: string;
  suspendedAt: string;
}

export type AuctionBusinessEvent =
  | AuctionStartedEvent
  | BidPlacedEvent
  | AuctionExtendedEvent
  | AuctionClosedEvent
  | AuctionSuspendedEvent;
