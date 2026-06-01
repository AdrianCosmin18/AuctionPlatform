export interface Bid {
  id: number;
  auctionId: number;
  bidderId: number;
  amount: number;
  createdAt: string;
  auctionExtended: boolean;
  newEndTime: string | null;
}
