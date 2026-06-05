package org.nedelcu.cosmin.auction.shared.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AuctionClosedEvent(
        Long auctionId,
        Long winnerId,
        Long winningBidId,
        BigDecimal finalPrice,
        AuctionCloseReason closedReason,
        OffsetDateTime closedAt
) {
}
