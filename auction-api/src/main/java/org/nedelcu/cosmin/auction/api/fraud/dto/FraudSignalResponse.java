package org.nedelcu.cosmin.auction.api.fraud.dto;

import java.time.OffsetDateTime;

public record FraudSignalResponse(
        FraudSignalType type,
        FraudSeverity severity,
        Long auctionId,
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
