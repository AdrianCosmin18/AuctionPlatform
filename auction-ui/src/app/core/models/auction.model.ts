import { AuctionCloseReason } from './auction-close-reason.type';
import { AuctionImage } from './auction-image.model';
import { AuctionStatus } from './auction-status.type';

export interface Auction {
  id: number;
  title: string;
  description: string | null;
  categoryCode: string;
  subcategoryCode: string | null;
  creatorAuthor: string | null;
  estimatedYear: number | null;
  languageCode: string | null;
  itemCondition: string | null;
  authenticityStatus: string | null;
  provenance: string | null;
  startPrice: number;
  currentPrice: number;
  minIncrement: number;
  reservePrice: number | null;
  reserveMet: boolean | null;
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
  watchersCount: number;
  watchedByCurrentUser: boolean;
  version: number;
}
