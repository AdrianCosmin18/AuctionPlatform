export interface CreateAuctionRequest {
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
  minIncrement: number;
  reservePrice: number | null;
  buyNowPrice: number | null;
  endTime: string;
  antiSnipingWindowSec: number | null;
  antiSnipingExtendSec: number | null;
  imageUrls: string[];
}
