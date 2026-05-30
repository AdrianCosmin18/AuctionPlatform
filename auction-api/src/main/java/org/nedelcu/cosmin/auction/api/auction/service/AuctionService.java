package org.nedelcu.cosmin.auction.api.auction.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.event.AuctionRealtimeEvent;
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
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxAggregateType;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxService;
import org.nedelcu.cosmin.auction.api.common.websocket.AuctionEventBroadcaster;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final OutboxService outboxService;
    private final AuctionEventBroadcaster auctionEventBroadcaster;

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
        return closeAuction(auction, now);
    }

    @Transactional
    public AuctionResponse closeExpiredAuction(Long id) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        OffsetDateTime now = OffsetDateTime.now();

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return toResponse(auction);
        }

        if (auction.getEndTime() == null || auction.getEndTime().isAfter(now)) {
            return toResponse(auction);
        }

        return closeAuction(auction, now);
    }

    @Transactional
    public BidResponse placeBid(Long auctionId, PlaceBidRequest request) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(auctionId)
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
        boolean auctionExtended = shouldExtendAuction(auction, now);
        if (auctionExtended) {
            auction.setEndTime(auction.getEndTime().plusSeconds(auction.getAntiSnipingExtendSec()));
        }
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.saveAndFlush(auction);
        BidEntity savedBid = bidRepository.save(bid);

        BidPlacedEvent bidPlacedEvent = new BidPlacedEvent(
                auctionId,
                savedBid.getId(),
                savedBid.getBidderId(),
                savedBid.getAmount(),
                savedAuction.getCurrentPrice(),
                now
        );
        publishAuctionEvent(auctionId, AuctionEventType.BID_PLACED, bidPlacedEvent, now);

        if (auctionExtended) {
            AuctionExtendedEvent auctionExtendedEvent = new AuctionExtendedEvent(
                    auctionId,
                    savedAuction.getEndTime(),
                    now
            );
            publishAuctionEvent(auctionId, AuctionEventType.AUCTION_EXTENDED, auctionExtendedEvent, now);
        }

        return new BidResponse(
                savedBid.getId(),
                savedBid.getAuctionId(),
                savedBid.getBidderId(),
                savedBid.getAmount(),
                savedBid.getCreatedAt(),
                auctionExtended,
                savedAuction.getEndTime()
        );
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
                bid.getCreatedAt(),
                false,
                null
        );
    }

    private boolean shouldExtendAuction(AuctionEntity auction, OffsetDateTime now) {
        if (auction.getEndTime() == null
                || auction.getAntiSnipingWindowSec() == null
                || auction.getAntiSnipingExtendSec() == null) {
            return false;
        }

        OffsetDateTime extensionThreshold = auction.getEndTime().minusSeconds(auction.getAntiSnipingWindowSec());
        return !now.isBefore(extensionThreshold);
    }

    private AuctionResponse closeAuction(AuctionEntity auction, OffsetDateTime now) {
        auction.setStatus(AuctionStatus.ENDED);
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        AuctionClosedEvent auctionClosedEvent = new AuctionClosedEvent(
                savedAuction.getId(),
                savedAuction.getCurrentPrice(),
                now
        );

        publishAuctionEvent(savedAuction.getId(), AuctionEventType.AUCTION_CLOSED, auctionClosedEvent, now);
        return toResponse(savedAuction);
    }

    private void publishAuctionEvent(
            Long auctionId,
            AuctionEventType eventType,
            Object payload,
            OffsetDateTime occurredAt
    ) {
        outboxService.saveEvent(
                OutboxAggregateType.AUCTION,
                auctionId,
                eventType,
                payload
        );
        auctionEventBroadcaster.broadcastToAuction(
                auctionId,
                new AuctionRealtimeEvent<>(
                        eventType.name(),
                        payload,
                        occurredAt
                )
        );
    }
}
