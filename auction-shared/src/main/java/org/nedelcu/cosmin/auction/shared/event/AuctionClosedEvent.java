package org.nedelcu.cosmin.auction.shared.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AuctionClosedEvent(
        Long auctionId,
        BigDecimal finalPrice,
        OffsetDateTime closedAt
) {
}
