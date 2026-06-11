package org.nedelcu.cosmin.auction.api.fraud.dto;

import java.time.OffsetDateTime;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;

public record FraudSignalResponse(
        FraudSignalType type,
        FraudSeverity severity,
        Long auctionId,
        AuctionStatus auctionStatus,
        Long sellerId,
        Long bidderId,
        long relatedBidCount,
        long relatedAuctionCount,
        int windowSeconds,
        String title,
        String details,
        OffsetDateTime firstSeenAt,
        OffsetDateTime lastSeenAt
) {
}
