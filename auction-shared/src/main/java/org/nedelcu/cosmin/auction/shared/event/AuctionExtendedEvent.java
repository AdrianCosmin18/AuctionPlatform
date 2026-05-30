package org.nedelcu.cosmin.auction.shared.event;

import java.time.OffsetDateTime;

public record AuctionExtendedEvent(
        Long auctionId,
        OffsetDateTime newEndTime,
        OffsetDateTime occurredAt
) {
}
