export interface Bid {
  id: number;
  auctionId: number;
  bidderId: number;
  bidderDisplayName: string;
  amount: number;
  createdAt: string;
  auctionExtended: boolean;
  newEndTime: string | null;
}
