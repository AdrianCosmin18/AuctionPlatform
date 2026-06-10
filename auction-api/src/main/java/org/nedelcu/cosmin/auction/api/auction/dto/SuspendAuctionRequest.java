package org.nedelcu.cosmin.auction.api.auction.dto;

import jakarta.validation.constraints.NotBlank;

public record SuspendAuctionRequest(
        @NotBlank(message = "Suspension reason is required")
        String reason
) {
}
