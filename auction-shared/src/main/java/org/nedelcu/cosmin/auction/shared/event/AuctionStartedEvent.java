package org.nedelcu.cosmin.auction.shared.event;

import java.time.OffsetDateTime;

public record AuctionStartedEvent(
        Long auctionId,
        OffsetDateTime startedAt
) {
}
