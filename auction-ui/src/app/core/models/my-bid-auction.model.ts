import { Auction } from './auction.model';

export interface MyBidAuction {
  auction: Auction;
  totalBids: number;
  highestBidAmount: number;
  latestBidAmount: number;
  latestBidAt: string;
  leading: boolean;
  won: boolean;
}
