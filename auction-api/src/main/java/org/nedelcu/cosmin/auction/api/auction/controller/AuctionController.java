package org.nedelcu.cosmin.auction.api.auction.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.BidResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.service.AuctionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionService auctionService;

    @GetMapping
    public List<AuctionResponse> getAuctions() {
        return auctionService.findAll();
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(@PathVariable("id") Long id) {
        return auctionService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        return auctionService.create(request);
    }

    @PostMapping("/{id}/start")
    public AuctionResponse startAuction(@PathVariable("id") Long id) {
        return auctionService.start(id);
    }

    @PostMapping("/{id}/close")
    public AuctionResponse closeAuction(@PathVariable("id") Long id) {
        return auctionService.close(id);
    }

    @PostMapping("/{id}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse placeBid(
            @PathVariable("id") Long id,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        return auctionService.placeBid(id, request);
    }

    @GetMapping("/{id}/bids")
    public List<BidResponse> getBids(@PathVariable("id") Long id) {
        return auctionService.findBids(id);
    }
}
