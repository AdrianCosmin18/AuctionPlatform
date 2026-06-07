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
  endTime: string;
  antiSnipingWindowSec: number | null;
  antiSnipingExtendSec: number | null;
  createdBy: number;
  imageUrls: string[];
}
