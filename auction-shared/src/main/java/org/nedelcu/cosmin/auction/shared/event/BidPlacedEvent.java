package org.nedelcu.cosmin.auction.shared.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidPlacedEvent(
        Long auctionId,
        Long bidId,
        Long bidderId,
        BigDecimal amount,
        BigDecimal currentPrice,
        OffsetDateTime occurredAt
) {
}
