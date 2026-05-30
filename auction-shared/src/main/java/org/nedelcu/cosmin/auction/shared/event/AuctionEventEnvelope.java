package org.nedelcu.cosmin.auction.shared.event;

public record AuctionEventEnvelope(
        String eventType,
        String payload
) {
}
