package org.nedelcu.cosmin.auction.api.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record CreateAuctionRequest(
        @NotBlank
        String title,

        String description,

        @NotBlank
        @Size(max = 80)
        String categoryCode,

        @Size(max = 80)
        String subcategoryCode,

        @Size(max = 255)
        String creatorAuthor,

        @Min(1000)
        Integer estimatedYear,

        @Size(max = 32)
        @Pattern(regexp = "^[A-Za-z][A-Za-z -]{0,31}$", message = "Language must contain letters, spaces, or hyphens only")
        String languageCode,

        @Size(max = 50)
        String itemCondition,

        @Size(max = 50)
        String authenticityStatus,

        @Size(max = 2000)
        String provenance,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal startPrice,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal minIncrement,

        @DecimalMin(value = "0.01")
        BigDecimal reservePrice,

        @DecimalMin(value = "0.01")
        BigDecimal buyNowPrice,

        @NotNull
        OffsetDateTime endTime,

        @Min(1)
        Integer antiSnipingWindowSec,

        @Min(1)
        Integer antiSnipingExtendSec,

        @Size(max = 5)
        List<@NotBlank @Pattern(regexp = "^https?://.+", message = "Image URL must start with http:// or https://") String> imageUrls
) {
}
