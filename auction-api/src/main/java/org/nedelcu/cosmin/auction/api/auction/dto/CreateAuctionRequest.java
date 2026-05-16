package org.nedelcu.cosmin.auction.api.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CreateAuctionRequest(
        @NotBlank
        String title,

        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal startPrice,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal minIncrement,

        @NotNull
        OffsetDateTime endTime,

        @Min(1)
        Integer antiSnipingWindowSec,

        @Min(1)
        Integer antiSnipingExtendSec,

        @NotNull
        Long createdBy
) {
}
