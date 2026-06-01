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
  finalPrice: number;
  closedAt: string;
}

export type AuctionBusinessEvent = BidPlacedEvent | AuctionExtendedEvent | AuctionClosedEvent;
