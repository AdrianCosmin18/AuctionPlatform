package org.nedelcu.cosmin.auction.api.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotNull
        Long bidderId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal amount
) {
}
