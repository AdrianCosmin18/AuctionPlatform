package org.nedelcu.cosmin.auction.api.auction.dto;

public record AuctionImageResponse(
        Long id,
        String imageUrl,
        Integer displayOrder
) {
}
