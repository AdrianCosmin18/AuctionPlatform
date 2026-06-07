package org.nedelcu.cosmin.auction.api.auction.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;

public record AuctionResponse(
        Long id,
        String title,
        String description,
        String categoryCode,
        String subcategoryCode,
        String creatorAuthor,
        Integer estimatedYear,
        String languageCode,
        String itemCondition,
        String authenticityStatus,
        String provenance,
        BigDecimal startPrice,
        BigDecimal currentPrice,
        BigDecimal minIncrement,
        AuctionStatus status,
        OffsetDateTime startTime,
        OffsetDateTime endTime,
        Integer antiSnipingWindowSec,
        Integer antiSnipingExtendSec,
        Long createdBy,
        Long winnerId,
        Long winningBidId,
        BigDecimal finalPrice,
        OffsetDateTime closedAt,
        AuctionCloseReason closedReason,
        List<AuctionImageResponse> images,
        Long version
) {
}
