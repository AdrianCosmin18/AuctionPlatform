package org.nedelcu.cosmin.auction.api.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BidResponse(
        Long id,
        Long auctionId,
        Long bidderId,
        BigDecimal amount,
        OffsetDateTime createdAt
) {
}
