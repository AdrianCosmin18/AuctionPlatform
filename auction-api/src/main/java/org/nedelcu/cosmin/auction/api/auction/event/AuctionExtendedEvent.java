package org.nedelcu.cosmin.auction.api.auction.event;

import java.time.OffsetDateTime;

public record AuctionExtendedEvent(
        Long auctionId,
        OffsetDateTime newEndTime,
        OffsetDateTime occurredAt
) {
}
