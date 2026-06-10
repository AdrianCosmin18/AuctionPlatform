package org.nedelcu.cosmin.auction.api.auction.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.SuspendAuctionRequest;
import org.nedelcu.cosmin.auction.api.auction.service.AuctionService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auctions")
@RequiredArgsConstructor
public class AdminAuctionController {
    private static final long DEFAULT_USER_ID = 2L;

    private final AuctionService auctionService;

    @PostMapping("/{id}/suspend")
    public AuctionResponse suspendAuction(
            @PathVariable("id") Long id,
            @Valid @RequestBody SuspendAuctionRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.suspend(id, request.reason(), resolveCurrentUserId(currentUserId));
    }

    private Long resolveCurrentUserId(Long currentUserId) {
        return currentUserId != null ? currentUserId : DEFAULT_USER_ID;
    }
}
