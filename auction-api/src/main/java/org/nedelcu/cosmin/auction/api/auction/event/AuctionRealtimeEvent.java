package org.nedelcu.cosmin.auction.api.auction.event;

import java.time.OffsetDateTime;

public record AuctionRealtimeEvent<T>(
        String type,
        T payload,
        OffsetDateTime occurredAt
) {
}
