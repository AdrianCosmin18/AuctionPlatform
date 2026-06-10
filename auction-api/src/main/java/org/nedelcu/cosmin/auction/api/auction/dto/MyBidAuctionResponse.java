package org.nedelcu.cosmin.auction.api.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record MyBidAuctionResponse(
        AuctionResponse auction,
        long totalBids,
        BigDecimal highestBidAmount,
        BigDecimal latestBidAmount,
        OffsetDateTime latestBidAt,
        boolean leading,
        boolean won
) {
}
