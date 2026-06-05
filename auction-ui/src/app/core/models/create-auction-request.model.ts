export interface CreateAuctionRequest {
  title: string;
  description: string | null;
  startPrice: number;
  minIncrement: number;
  endTime: string;
  antiSnipingWindowSec: number | null;
  antiSnipingExtendSec: number | null;
  createdBy: number;
  imageUrls: string[];
}
