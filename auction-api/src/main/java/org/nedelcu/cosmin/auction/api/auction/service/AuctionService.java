package org.nedelcu.cosmin.auction.api.auction.service;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.api.auction.event.BidPlacedEvent;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.BidResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.nedelcu.cosmin.auction.api.common.exception.ResourceNotFoundException;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OutboxService outboxService;

    public List<AuctionResponse> findAll() {
        return auctionRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public AuctionResponse findById(Long id) {
        AuctionEntity auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        return toResponse(auction);
    }

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request) {
        OffsetDateTime now = OffsetDateTime.now();

        AuctionEntity auction = new AuctionEntity();
        auction.setTitle(request.title());
        auction.setDescription(request.description());
        auction.setStartPrice(request.startPrice());
        auction.setCurrentPrice(request.startPrice());
        auction.setMinIncrement(request.minIncrement());
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setStartTime(null);
        auction.setEndTime(request.endTime());
        auction.setAntiSnipingWindowSec(request.antiSnipingWindowSec() != null ? request.antiSnipingWindowSec() : 30);
        auction.setAntiSnipingExtendSec(request.antiSnipingExtendSec() != null ? request.antiSnipingExtendSec() : 30);
        auction.setCreatedBy(request.createdBy());
        auction.setCreatedAt(now);
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        return toResponse(savedAuction);
    }

    @Transactional
    public AuctionResponse start(Long id) {
        AuctionEntity auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new BusinessException("Only DRAFT auctions can be started");
        }

        OffsetDateTime now = OffsetDateTime.now();

        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) {
            throw new BusinessException("Auction end time must be in the future");
        }

        auction.setStatus(AuctionStatus.RUNNING);
        auction.setStartTime(now);
        auction.setUpdatedAt(now);

        return toResponse(auctionRepository.save(auction));
    }

    @Transactional
    public AuctionResponse close(Long id) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new BusinessException("Only RUNNING auctions can be closed");
        }

        OffsetDateTime now = OffsetDateTime.now();

        auction.setStatus(AuctionStatus.ENDED);
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        outboxService.saveEvent(
                "AUCTION",
                id,
                "AUCTION_CLOSED",
                new AuctionClosedEvent(
                        id,
                        savedAuction.getCurrentPrice(),
                        now
                )
        );
        return toResponse(savedAuction);
    }

    @Transactional
    public BidResponse placeBid(Long auctionId, PlaceBidRequest request) {
        AuctionEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        OffsetDateTime now = OffsetDateTime.now();

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new BusinessException("Only RUNNING auctions accept bids");
        }

        if (auction.getEndTime() == null || !auction.getEndTime().isAfter(now)) {
            throw new BusinessException("Auction has already ended");
        }

        BigDecimal minimumAcceptedAmount = auction.getCurrentPrice().add(auction.getMinIncrement());
        if (request.amount().compareTo(minimumAcceptedAmount) < 0) {
            throw new BusinessException("Bid amount must be at least " + minimumAcceptedAmount);
        }

        BidEntity bid = new BidEntity();
        bid.setAuctionId(auctionId);
        bid.setBidderId(request.bidderId());
        bid.setAmount(request.amount());
        bid.setCreatedAt(now);

        auction.setCurrentPrice(request.amount());
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.saveAndFlush(auction);
        BidEntity savedBid = bidRepository.save(bid);
        outboxService.saveEvent(
                "AUCTION",
                auctionId,
                "BID_PLACED",
                new BidPlacedEvent(
                        auctionId,
                        savedBid.getId(),
                        savedBid.getBidderId(),
                        savedBid.getAmount(),
                        savedAuction.getCurrentPrice(),
                        now
                )
        );

        return toBidResponse(savedBid);
    }

    public List<BidResponse> findBids(Long auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new ResourceNotFoundException("Auction not found: " + auctionId);
        }

        return bidRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId).stream()
                .map(this::toBidResponse)
                .toList();
    }

    private AuctionResponse toResponse(AuctionEntity auctionEntity) {
        return new AuctionResponse(
                auctionEntity.getId(),
                auctionEntity.getTitle(),
                auctionEntity.getDescription(),
                auctionEntity.getStartPrice(),
                auctionEntity.getCurrentPrice(),
                auctionEntity.getMinIncrement(),
                auctionEntity.getStatus(),
                auctionEntity.getStartTime(),
                auctionEntity.getEndTime(),
                auctionEntity.getAntiSnipingWindowSec(),
                auctionEntity.getAntiSnipingExtendSec(),
                auctionEntity.getCreatedBy(),
                auctionEntity.getVersion()
        );
    }

    private BidResponse toBidResponse(BidEntity bid) {
        return new BidResponse(
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidderId(),
                bid.getAmount(),
                bid.getCreatedAt()
        );
    }
}
