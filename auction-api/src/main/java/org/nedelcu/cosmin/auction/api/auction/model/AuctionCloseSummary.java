package org.nedelcu.cosmin.auction.api.auction.model;

import java.math.BigDecimal;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;

public record AuctionCloseSummary(
        Long winnerId,
        Long winningBidId,
        BigDecimal finalPrice,
        AuctionCloseReason closedReason,
        Boolean reserveMet
) {
}
