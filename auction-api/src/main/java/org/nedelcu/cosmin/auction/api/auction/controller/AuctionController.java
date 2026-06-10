package org.nedelcu.cosmin.auction.api.auction.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.BidResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest;
import org.nedelcu.cosmin.auction.api.auction.dto.MyBidAuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.service.AuctionService;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {
    private static final long DEFAULT_USER_ID = 2L;

    private final AuctionService auctionService;

    @GetMapping
    public List<AuctionResponse> getAuctions(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return auctionService.findAll(resolveCurrentUserId(currentUserId));
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.findById(id, resolveCurrentUserId(currentUserId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuction(
            @Valid @RequestBody CreateAuctionRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.create(request, resolveCurrentUserId(currentUserId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuctionWithImages(
            @Valid @RequestPart("payload") CreateAuctionRequest request,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.createWithUploadedImages(request, images != null ? images : List.of(), resolveCurrentUserId(currentUserId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuctionResponse updateAuction(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateAuctionRequest request,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.update(id, request, resolveCurrentUserId(currentUserId));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuctionResponse updateAuctionWithImages(
            @PathVariable("id") Long id,
            @Valid @RequestPart("payload") CreateAuctionRequest request,
            @RequestPart(name = "images", required = false) List<MultipartFile> images,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.updateWithUploadedImages(id, request, images != null ? images : List.of(), resolveCurrentUserId(currentUserId));
    }

    @PostMapping("/{id}/start")
    public AuctionResponse startAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.start(id, resolveCurrentUserId(currentUserId));
    }

    @PostMapping("/{id}/close")
    public AuctionResponse closeAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.close(id, resolveCurrentUserId(currentUserId));
    }

    @PostMapping("/{id}/buy-now")
    public AuctionResponse buyNowAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.buyNow(id, resolveCurrentUserId(currentUserId));
    }

    @PostMapping("/{id}/watch")
    public AuctionResponse watchAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.watchAuction(id, resolveCurrentUserId(currentUserId));
    }

    @DeleteMapping("/{id}/watch")
    public AuctionResponse unwatchAuction(
            @PathVariable("id") Long id,
            @RequestHeader(name = "X-User-Id", required = false) Long currentUserId
    ) {
        return auctionService.unwatchAuction(id, resolveCurrentUserId(currentUserId));
    }

    @GetMapping("/me/watchlist")
    public List<AuctionResponse> getMyWatchlist(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return auctionService.findWatchlist(resolveCurrentUserId(currentUserId));
    }

    @GetMapping("/me/created")
    public List<AuctionResponse> getMyAuctions(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return auctionService.findCreatedByUser(resolveCurrentUserId(currentUserId));
    }

    @GetMapping("/me/bids")
    public List<MyBidAuctionResponse> getMyBids(@RequestHeader(name = "X-User-Id", required = false) Long currentUserId) {
        return auctionService.findBiddingActivity(resolveCurrentUserId(currentUserId));
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

    private Long resolveCurrentUserId(Long currentUserId) {
        return currentUserId != null ? currentUserId : DEFAULT_USER_ID;
    }
}
