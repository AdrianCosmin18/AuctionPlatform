package org.nedelcu.cosmin.auction.api.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;

public record AuctionResponse(
        Long id,
        String title,
        String description,
        BigDecimal startPrice,
        BigDecimal currentPrice,
        BigDecimal minIncrement,
        AuctionStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer antiSnipingWindowSec,
        Integer antiSnipingExtendSec,
        Long createdBy,
        Long version
) {
}
