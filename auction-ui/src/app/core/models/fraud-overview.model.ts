export type FraudSeverity = 'LOW' | 'MEDIUM' | 'HIGH';
export type FraudSignalType = 'BURST_BIDDING' | 'SELLER_BIDDER_CONCENTRATION';
import { AuctionStatus } from './auction-status.type';

export interface FraudSignal {
  type: FraudSignalType;
  severity: FraudSeverity;
  auctionId: number | null;
  auctionStatus: AuctionStatus | null;
  sellerId: number | null;
  bidderId: number | null;
  relatedBidCount: number;
  relatedAuctionCount: number;
  windowSeconds: number;
  title: string;
  details: string;
  firstSeenAt: string;
  lastSeenAt: string;
}

export interface FraudOverview {
  totalSignals: number;
  highSeveritySignals: number;
  mediumSeveritySignals: number;
  lowSeveritySignals: number;
  signals: FraudSignal[];
}
