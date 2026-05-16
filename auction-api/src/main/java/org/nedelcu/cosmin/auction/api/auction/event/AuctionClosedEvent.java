package org.nedelcu.cosmin.auction.api.auction.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AuctionClosedEvent(
        Long auctionId,
        BigDecimal finalPrice,
        OffsetDateTime closedAt
) {
}
