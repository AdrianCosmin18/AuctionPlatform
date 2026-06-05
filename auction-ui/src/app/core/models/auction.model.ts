import { AuctionCloseReason } from './auction-close-reason.type';
import { AuctionImage } from './auction-image.model';
import { AuctionStatus } from './auction-status.type';

export interface Auction {
  id: number;
  title: string;
  description: string | null;
  startPrice: number;
  currentPrice: number;
  minIncrement: number;
  status: AuctionStatus;
  startTime: string | null;
  endTime: string | null;
  antiSnipingWindowSec: number | null;
  antiSnipingExtendSec: number | null;
  createdBy: number;
  winnerId: number | null;
  winningBidId: number | null;
  finalPrice: number | null;
  closedAt: string | null;
  closedReason: AuctionCloseReason | null;
  images: AuctionImage[];
  version: number;
}
