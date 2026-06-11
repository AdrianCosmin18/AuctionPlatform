package org.nedelcu.cosmin.auction.api.auction.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auth.CurrentUserService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auctions")
@RequiredArgsConstructor
public class AuctionController {
    private final AuctionService auctionService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public List<AuctionResponse> getAuctions() {
        return auctionService.findAll(currentUserService.getCurrentUserId());
    }

    @GetMapping("/{id}")
    public AuctionResponse getAuction(@PathVariable("id") Long id) {
        return auctionService.findById(id, currentUserService.getCurrentUserId());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuction(@Valid @RequestBody CreateAuctionRequest request) {
        return auctionService.create(request, currentUserService.getCurrentUserId());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse createAuctionWithImages(
            @Valid @RequestPart("payload") CreateAuctionRequest request,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        return auctionService.createWithUploadedImages(request, images != null ? images : List.of(), currentUserService.getCurrentUserId());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public AuctionResponse updateAuction(@PathVariable("id") Long id, @Valid @RequestBody CreateAuctionRequest request) {
        return auctionService.update(id, request, currentUserService.getCurrentUserId());
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AuctionResponse updateAuctionWithImages(
            @PathVariable("id") Long id,
            @Valid @RequestPart("payload") CreateAuctionRequest request,
            @RequestPart(name = "images", required = false) List<MultipartFile> images
    ) {
        return auctionService.updateWithUploadedImages(id, request, images != null ? images : List.of(), currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/start")
    public AuctionResponse startAuction(@PathVariable("id") Long id) {
        return auctionService.start(id, currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/close")
    public AuctionResponse closeAuction(@PathVariable("id") Long id) {
        return auctionService.close(id, currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/buy-now")
    public AuctionResponse buyNowAuction(@PathVariable("id") Long id) {
        return auctionService.buyNow(id, currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/watch")
    public AuctionResponse watchAuction(@PathVariable("id") Long id) {
        return auctionService.watchAuction(id, currentUserService.getCurrentUserId());
    }

    @DeleteMapping("/{id}/watch")
    public AuctionResponse unwatchAuction(@PathVariable("id") Long id) {
        return auctionService.unwatchAuction(id, currentUserService.getCurrentUserId());
    }

    @GetMapping("/me/watchlist")
    public List<AuctionResponse> getMyWatchlist() {
        return auctionService.findWatchlist(currentUserService.getCurrentUserId());
    }

    @GetMapping("/me/created")
    public List<AuctionResponse> getMyAuctions() {
        return auctionService.findCreatedByUser(currentUserService.getCurrentUserId());
    }

    @GetMapping("/me/bids")
    public List<MyBidAuctionResponse> getMyBids() {
        return auctionService.findBiddingActivity(currentUserService.getCurrentUserId());
    }

    @PostMapping("/{id}/bids")
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse placeBid(@PathVariable("id") Long id, @Valid @RequestBody PlaceBidRequest request) {
        return auctionService.placeBid(id, request, currentUserService.getCurrentUserId());
    }

    @GetMapping("/{id}/bids")
    public List<BidResponse> getBids(@PathVariable("id") Long id) {
        return auctionService.findBids(id);
    }
}
