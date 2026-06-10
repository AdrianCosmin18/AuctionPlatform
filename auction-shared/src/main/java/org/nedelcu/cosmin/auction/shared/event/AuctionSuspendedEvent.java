package org.nedelcu.cosmin.auction.shared.event;

import java.time.OffsetDateTime;

public record AuctionSuspendedEvent(
        Long auctionId,
        Long suspendedBy,
        String reason,
        OffsetDateTime suspendedAt
) {
}
