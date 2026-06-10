package org.nedelcu.cosmin.auction.api.auction.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.auction.event.AuctionRealtimeEvent;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionImageResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.AuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.BidResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.CreateAuctionRequest;
import org.nedelcu.cosmin.auction.api.auction.dto.MyBidAuctionResponse;
import org.nedelcu.cosmin.auction.api.auction.dto.PlaceBidRequest;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionImageEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionWatchlistEntity;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionCloseSummary;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionDomainRules;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionImageRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionWatchlistRepository;
import org.nedelcu.cosmin.auction.api.auction.repository.BidRepository;
import org.nedelcu.cosmin.auction.api.common.exception.BusinessException;
import org.nedelcu.cosmin.auction.api.common.exception.ResourceNotFoundException;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxAggregateType;
import org.nedelcu.cosmin.auction.api.common.outbox.OutboxService;
import org.nedelcu.cosmin.auction.api.common.websocket.AuctionEventBroadcaster;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final AuctionWatchlistRepository auctionWatchlistRepository;
    private final BidRepository bidRepository;
    private final OutboxService outboxService;
    private final AuctionEventBroadcaster auctionEventBroadcaster;
    private final AuctionMediaStorageService auctionMediaStorageService;

    public List<AuctionResponse> findAll() {
        return findAll(null);
    }

    public List<AuctionResponse> findAll(Long currentUserId) {
        List<AuctionEntity> auctions = auctionRepository.findAll();
        Map<Long, List<AuctionImageResponse>> imagesByAuctionId = loadImagesByAuctionId(
                auctions.stream().map(AuctionEntity::getId).toList()
        );
        Map<Long, Long> watcherCountsByAuctionId = loadWatcherCountsByAuctionId(
                auctions.stream().map(AuctionEntity::getId).toList()
        );
        List<Long> watchedAuctionIds = loadWatchedAuctionIds(
                currentUserId,
                auctions.stream().map(AuctionEntity::getId).toList()
        );

        return auctions.stream()
                .map(auction -> toResponse(
                        auction,
                        imagesByAuctionId.getOrDefault(auction.getId(), List.of()),
                        watcherCountsByAuctionId.getOrDefault(auction.getId(), 0L),
                        watchedAuctionIds.contains(auction.getId())
                ))
                .toList();
    }

    public AuctionResponse findById(Long id) {
        return findById(id, null);
    }

    public AuctionResponse findById(Long id, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        boolean watchedByCurrentUser = currentUserId != null
                && auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, auction.getId());

        return toResponse(
                auction,
                loadImages(auction.getId()),
                watcherCountForAuction(auction.getId()),
                watchedByCurrentUser
        );
    }

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request) {
        return create(request, (Long) null);
    }

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request, Long currentUserId) {
        return createWithImageUrls(request, request.imageUrls(), currentUserId);
    }

    @Transactional
    public AuctionResponse createWithUploadedImages(CreateAuctionRequest request, List<MultipartFile> imageFiles) {
        return createWithUploadedImages(request, imageFiles, null);
    }

    @Transactional
    public AuctionResponse createWithUploadedImages(CreateAuctionRequest request, List<MultipartFile> imageFiles, Long currentUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        validateUpsertRequest(request);

        AuctionEntity auction = new AuctionEntity();
        applyAuctionDraftFields(auction, request, now);
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setStartTime(null);
        auction.setWinnerId(null);
        auction.setWinningBidId(null);
        auction.setFinalPrice(null);
        auction.setReserveMet(computeReserveMet(request.startPrice(), request.reservePrice()));
        auction.setClosedAt(null);
        auction.setClosedReason(null);
        auction.setCreatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        List<String> storedPaths = List.of();
        try {
            storedPaths = auctionMediaStorageService.storeAuctionImages(savedAuction.getId(), imageFiles);
            saveAuctionImages(savedAuction.getId(), storedPaths, 0);
        } catch (RuntimeException ex) {
            auctionMediaStorageService.deleteStoredImages(storedPaths);
            throw ex;
        }

        return toResponse(savedAuction, loadImages(savedAuction.getId()), 0L, false);
    }

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request, List<String> imageUrls) {
        return createWithImageUrls(request, imageUrls, null);
    }

    @Transactional
    public AuctionResponse createWithImageUrls(CreateAuctionRequest request, List<String> imageUrls, Long currentUserId) {
        OffsetDateTime now = OffsetDateTime.now();
        validateUpsertRequest(request);

        AuctionEntity auction = new AuctionEntity();
        applyAuctionDraftFields(auction, request, now);
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setStartTime(null);
        auction.setWinnerId(null);
        auction.setWinningBidId(null);
        auction.setFinalPrice(null);
        auction.setReserveMet(computeReserveMet(request.startPrice(), request.reservePrice()));
        auction.setClosedAt(null);
        auction.setClosedReason(null);
        auction.setCreatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        saveAuctionImages(savedAuction.getId(), imageUrls, 0);

        return toResponse(savedAuction, loadImages(savedAuction.getId()), 0L, false);
    }

    @Transactional
    public AuctionResponse update(Long id, CreateAuctionRequest request) {
        return update(id, request, null);
    }

    @Transactional
    public AuctionResponse update(Long id, CreateAuctionRequest request, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        ensureDraftAuction(auction);
        validateUpsertRequest(request);
        applyAuctionDraftFields(auction, request, OffsetDateTime.now());

        AuctionEntity savedAuction = auctionRepository.save(auction);
        int existingImageCount = loadImages(savedAuction.getId()).size();
        saveAuctionImages(savedAuction.getId(), request.imageUrls(), existingImageCount);

        return toResponse(
                savedAuction,
                loadImages(savedAuction.getId()),
                watcherCountForAuction(savedAuction.getId()),
                currentUserId != null && auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, savedAuction.getId())
        );
    }

    @Transactional
    public AuctionResponse updateWithUploadedImages(Long id, CreateAuctionRequest request, List<MultipartFile> imageFiles) {
        return updateWithUploadedImages(id, request, imageFiles, null);
    }

    @Transactional
    public AuctionResponse updateWithUploadedImages(Long id, CreateAuctionRequest request, List<MultipartFile> imageFiles, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        ensureDraftAuction(auction);
        validateUpsertRequest(request);
        applyAuctionDraftFields(auction, request, OffsetDateTime.now());

        AuctionEntity savedAuction = auctionRepository.save(auction);
        List<AuctionImageResponse> existingImages = loadImages(savedAuction.getId());
        saveAuctionImages(savedAuction.getId(), request.imageUrls(), existingImages.size());

        List<String> storedPaths = List.of();
        try {
            storedPaths = auctionMediaStorageService.storeAuctionImages(savedAuction.getId(), imageFiles);
            saveAuctionImages(savedAuction.getId(), storedPaths, existingImages.size() + countNonBlankUrls(request.imageUrls()));
        } catch (RuntimeException ex) {
            auctionMediaStorageService.deleteStoredImages(storedPaths);
            throw ex;
        }

        return toResponse(
                savedAuction,
                loadImages(savedAuction.getId()),
                watcherCountForAuction(savedAuction.getId()),
                currentUserId != null && auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, savedAuction.getId())
        );
    }

    @Transactional
    public AuctionResponse start(Long id) {
        return start(id, null);
    }

    @Transactional
    public AuctionResponse start(Long id, Long currentUserId) {
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

        AuctionEntity savedAuction = auctionRepository.save(auction);
        return toResponse(
                savedAuction,
                loadImages(savedAuction.getId()),
                watcherCountForAuction(savedAuction.getId()),
                currentUserId != null && auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, savedAuction.getId())
        );
    }

    @Transactional
    public AuctionResponse close(Long id) {
        return close(id, null);
    }

    @Transactional
    public AuctionResponse close(Long id, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new BusinessException("Only RUNNING auctions can be closed");
        }

        OffsetDateTime now = OffsetDateTime.now();
        return closeAuction(auction, now, AuctionCloseReason.MANUAL, currentUserId);
    }

    @Transactional
    public AuctionResponse closeExpiredAuction(Long id) {
        AuctionEntity auction = auctionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        OffsetDateTime now = OffsetDateTime.now();

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return toResponse(
                    auction,
                    loadImages(auction.getId()),
                    watcherCountForAuction(auction.getId()),
                    false
            );
        }

        if (auction.getEndTime() == null || auction.getEndTime().isAfter(now)) {
            return toResponse(
                    auction,
                    loadImages(auction.getId()),
                    watcherCountForAuction(auction.getId()),
                    false
            );
        }

        return closeAuction(auction, now, AuctionCloseReason.EXPIRED, null);
    }

    @Transactional
    public AuctionResponse watchAuction(Long auctionId, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, auctionId)) {
            throw new BusinessException("The auction is already in the watchlist");
        }

        AuctionWatchlistEntity watch = new AuctionWatchlistEntity();
        watch.setUserId(currentUserId);
        watch.setAuctionId(auctionId);
        watch.setCreatedAt(OffsetDateTime.now());
        auctionWatchlistRepository.save(watch);

        return toResponse(auction, loadImages(auction.getId()), watcherCountForAuction(auction.getId()), true);
    }

    @Transactional
    public AuctionResponse unwatchAuction(Long auctionId, Long currentUserId) {
        AuctionEntity auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        AuctionWatchlistEntity watch = auctionWatchlistRepository.findByUserIdAndAuctionId(currentUserId, auctionId)
                .orElseThrow(() -> new BusinessException("The auction is not in the watchlist"));
        auctionWatchlistRepository.delete(watch);

        return toResponse(auction, loadImages(auction.getId()), watcherCountForAuction(auction.getId()), false);
    }

    public List<AuctionResponse> findWatchlist(Long currentUserId) {
        List<AuctionWatchlistEntity> watchEntries = auctionWatchlistRepository.findByUserIdOrderByCreatedAtDesc(currentUserId);
        List<Long> auctionIds = watchEntries.stream().map(AuctionWatchlistEntity::getAuctionId).distinct().toList();

        if (auctionIds.isEmpty()) {
            return List.of();
        }

        Map<Long, AuctionEntity> auctionsById = auctionRepository.findAllById(auctionIds).stream()
                .collect(java.util.stream.Collectors.toMap(AuctionEntity::getId, auction -> auction));
        Map<Long, List<AuctionImageResponse>> imagesByAuctionId = loadImagesByAuctionId(auctionIds);
        Map<Long, Long> watcherCountsByAuctionId = loadWatcherCountsByAuctionId(auctionIds);

        return auctionIds.stream()
                .map(auctionsById::get)
                .filter(java.util.Objects::nonNull)
                .map(auction -> toResponse(
                        auction,
                        imagesByAuctionId.getOrDefault(auction.getId(), List.of()),
                        watcherCountsByAuctionId.getOrDefault(auction.getId(), 0L),
                        true
                ))
                .toList();
    }

    public List<AuctionResponse> findCreatedByUser(Long currentUserId) {
        List<AuctionEntity> auctions = auctionRepository.findByCreatedByOrderByCreatedAtDesc(currentUserId);
        List<Long> auctionIds = auctions.stream().map(AuctionEntity::getId).toList();
        Map<Long, List<AuctionImageResponse>> imagesByAuctionId = loadImagesByAuctionId(auctionIds);
        Map<Long, Long> watcherCountsByAuctionId = loadWatcherCountsByAuctionId(auctionIds);
        List<Long> watchedAuctionIds = loadWatchedAuctionIds(currentUserId, auctionIds);

        return auctions.stream()
                .map(auction -> toResponse(
                        auction,
                        imagesByAuctionId.getOrDefault(auction.getId(), List.of()),
                        watcherCountsByAuctionId.getOrDefault(auction.getId(), 0L),
                        watchedAuctionIds.contains(auction.getId())
                ))
                .toList();
    }

    public List<MyBidAuctionResponse> findBiddingActivity(Long currentUserId) {
        List<BidEntity> bids = bidRepository.findByBidderIdOrderByCreatedAtDesc(currentUserId);
        if (bids.isEmpty()) {
            return List.of();
        }

        Map<Long, List<BidEntity>> bidsByAuctionId = new LinkedHashMap<>();
        for (BidEntity bid : bids) {
            bidsByAuctionId.computeIfAbsent(bid.getAuctionId(), ignored -> new ArrayList<>()).add(bid);
        }

        List<Long> auctionIds = new ArrayList<>(bidsByAuctionId.keySet());
        Map<Long, AuctionEntity> auctionsById = auctionRepository.findAllById(auctionIds).stream()
                .collect(java.util.stream.Collectors.toMap(AuctionEntity::getId, auction -> auction));
        Map<Long, List<AuctionImageResponse>> imagesByAuctionId = loadImagesByAuctionId(auctionIds);
        Map<Long, Long> watcherCountsByAuctionId = loadWatcherCountsByAuctionId(auctionIds);
        List<Long> watchedAuctionIds = loadWatchedAuctionIds(currentUserId, auctionIds);
        Map<Long, BidEntity> topBidsByAuctionId = loadTopBidsByAuctionId(auctionIds);

        List<MyBidAuctionResponse> responses = new ArrayList<>();
        for (Map.Entry<Long, List<BidEntity>> entry : bidsByAuctionId.entrySet()) {
            AuctionEntity auction = auctionsById.get(entry.getKey());
            if (auction == null) {
                continue;
            }

            List<BidEntity> auctionBids = entry.getValue();
            BidEntity latestBid = auctionBids.get(0);
            BigDecimal highestBidAmount = auctionBids.stream()
                    .map(BidEntity::getAmount)
                    .max(BigDecimal::compareTo)
                    .orElse(latestBid.getAmount());
            BidEntity topBid = topBidsByAuctionId.get(auction.getId());
            boolean won = auction.getWinnerId() != null && auction.getWinnerId().equals(currentUserId);
            boolean leading = !won
                    && auction.getStatus() == AuctionStatus.RUNNING
                    && topBid != null
                    && currentUserId.equals(topBid.getBidderId());

            responses.add(new MyBidAuctionResponse(
                    toResponse(
                            auction,
                            imagesByAuctionId.getOrDefault(auction.getId(), List.of()),
                            watcherCountsByAuctionId.getOrDefault(auction.getId(), 0L),
                            watchedAuctionIds.contains(auction.getId())
                    ),
                    auctionBids.size(),
                    highestBidAmount,
                    latestBid.getAmount(),
                    latestBid.getCreatedAt(),
                    leading,
                    won
            ));
        }

        return responses;
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
        auction.setReserveMet(computeReserveMet(request.amount(), auction.getReservePrice()));
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

    private AuctionResponse toResponse(
            AuctionEntity auctionEntity,
            List<AuctionImageResponse> images,
            long watchersCount,
            boolean watchedByCurrentUser
    ) {
        return new AuctionResponse(
                auctionEntity.getId(),
                auctionEntity.getTitle(),
                auctionEntity.getDescription(),
                auctionEntity.getCategoryCode(),
                auctionEntity.getSubcategoryCode(),
                auctionEntity.getCreatorAuthor(),
                auctionEntity.getEstimatedYear(),
                auctionEntity.getLanguageCode(),
                auctionEntity.getItemCondition(),
                auctionEntity.getAuthenticityStatus(),
                auctionEntity.getProvenance(),
                auctionEntity.getStartPrice(),
                auctionEntity.getCurrentPrice(),
                auctionEntity.getMinIncrement(),
                auctionEntity.getReservePrice(),
                auctionEntity.getReserveMet(),
                auctionEntity.getStatus(),
                auctionEntity.getStartTime(),
                auctionEntity.getEndTime(),
                auctionEntity.getAntiSnipingWindowSec(),
                auctionEntity.getAntiSnipingExtendSec(),
                auctionEntity.getCreatedBy(),
                auctionEntity.getWinnerId(),
                auctionEntity.getWinningBidId(),
                auctionEntity.getFinalPrice(),
                auctionEntity.getClosedAt(),
                auctionEntity.getClosedReason(),
                images,
                watchersCount,
                watchedByCurrentUser,
                auctionEntity.getVersion()
        );
    }

    private AuctionImageResponse toImageResponse(AuctionImageEntity image) {
        return new AuctionImageResponse(
                image.getId(),
                image.getImageUrl(),
                image.getDisplayOrder()
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

    private AuctionResponse closeAuction(
            AuctionEntity auction,
            OffsetDateTime now,
            AuctionCloseReason closedReason,
            Long currentUserId
    ) {
        AuctionCloseSummary closeSummary = resolveCloseSummary(auction, closedReason);

        auction.setStatus(AuctionStatus.ENDED);
        auction.setWinnerId(closeSummary.winnerId());
        auction.setWinningBidId(closeSummary.winningBidId());
        auction.setFinalPrice(closeSummary.finalPrice());
        auction.setReserveMet(closeSummary.reserveMet());
        auction.setClosedAt(now);
        auction.setClosedReason(closeSummary.closedReason());
        auction.setUpdatedAt(now);

        AuctionEntity savedAuction = auctionRepository.save(auction);
        AuctionClosedEvent auctionClosedEvent = new AuctionClosedEvent(
                savedAuction.getId(),
                savedAuction.getWinnerId(),
                savedAuction.getWinningBidId(),
                savedAuction.getFinalPrice(),
                savedAuction.getReserveMet(),
                savedAuction.getClosedReason(),
                now
        );

        publishAuctionEvent(savedAuction.getId(), AuctionEventType.AUCTION_CLOSED, auctionClosedEvent, now);
        return toResponse(
                savedAuction,
                loadImages(savedAuction.getId()),
                watcherCountForAuction(savedAuction.getId()),
                currentUserId != null && auctionWatchlistRepository.existsByUserIdAndAuctionId(currentUserId, savedAuction.getId())
        );
    }

    private AuctionCloseSummary resolveCloseSummary(AuctionEntity auction, AuctionCloseReason closedReason) {
        BidEntity winningBid = bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auction.getId())
                .orElse(null);
        Boolean reserveMet = computeReserveMet(auction.getCurrentPrice(), auction.getReservePrice());

        if (winningBid == null) {
            return new AuctionCloseSummary(
                    null,
                    null,
                    auction.getCurrentPrice(),
                    closedReason,
                    reserveMet
            );
        }

        if (Boolean.FALSE.equals(reserveMet)) {
            return new AuctionCloseSummary(
                    null,
                    null,
                    winningBid.getAmount(),
                    closedReason,
                    false
            );
        }

        return new AuctionCloseSummary(
                winningBid.getBidderId(),
                winningBid.getId(),
                winningBid.getAmount(),
                closedReason,
                reserveMet
        );
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

    private void saveAuctionImages(Long auctionId, List<String> imageUrls, int startOrder) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<AuctionImageEntity> images = new ArrayList<>(imageUrls.size());
        int displayOrder = startOrder;

        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.isBlank()) {
                continue;
            }

            AuctionImageEntity image = new AuctionImageEntity();
            image.setAuctionId(auctionId);
            image.setImageUrl(imageUrl.trim());
            image.setDisplayOrder(displayOrder++);
            images.add(image);
        }

        if (!images.isEmpty()) {
            auctionImageRepository.saveAll(images);
        }
    }

    private void validateUpsertRequest(CreateAuctionRequest request) {
        if (!AuctionDomainRules.isValidCategory(request.categoryCode())) {
            throw new BusinessException("Unsupported category code: " + request.categoryCode());
        }

        String subcategoryCode = trimToNull(request.subcategoryCode());
        if (subcategoryCode != null && !AuctionDomainRules.isValidSubcategory(request.categoryCode(), subcategoryCode)) {
            throw new BusinessException("Unsupported subcategory for category " + request.categoryCode() + ": " + subcategoryCode);
        }

        BigDecimal reservePrice = request.reservePrice();
        if (reservePrice != null && reservePrice.compareTo(request.startPrice()) < 0) {
            throw new BusinessException("Reserve price must be greater than or equal to the opening price");
        }

        String itemCondition = trimToNull(request.itemCondition());
        if (itemCondition != null && !AuctionDomainRules.isValidCondition(itemCondition)) {
            throw new BusinessException("Unsupported item condition: " + itemCondition);
        }

        String authenticityStatus = trimToNull(request.authenticityStatus());
        if (authenticityStatus != null && !AuctionDomainRules.isValidAuthenticityStatus(authenticityStatus)) {
            throw new BusinessException("Unsupported authenticity status: " + authenticityStatus);
        }
    }

    private void ensureDraftAuction(AuctionEntity auction) {
        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new BusinessException("Only DRAFT auctions can be edited");
        }
    }

    private void applyAuctionDraftFields(AuctionEntity auction, CreateAuctionRequest request, OffsetDateTime now) {
        auction.setTitle(request.title().trim());
        auction.setDescription(trimToNull(request.description()));
        auction.setCategoryCode(request.categoryCode());
        auction.setSubcategoryCode(trimToNull(request.subcategoryCode()));
        auction.setCreatorAuthor(trimToNull(request.creatorAuthor()));
        auction.setEstimatedYear(request.estimatedYear());
        auction.setLanguageCode(trimToNull(request.languageCode()));
        auction.setItemCondition(trimToNull(request.itemCondition()));
        auction.setAuthenticityStatus(trimToNull(request.authenticityStatus()));
        auction.setProvenance(trimToNull(request.provenance()));
        auction.setStartPrice(request.startPrice());
        auction.setCurrentPrice(request.startPrice());
        auction.setMinIncrement(request.minIncrement());
        auction.setReservePrice(request.reservePrice());
        auction.setReserveMet(computeReserveMet(request.startPrice(), request.reservePrice()));
        auction.setEndTime(request.endTime());
        auction.setAntiSnipingWindowSec(request.antiSnipingWindowSec() != null ? request.antiSnipingWindowSec() : 30);
        auction.setAntiSnipingExtendSec(request.antiSnipingExtendSec() != null ? request.antiSnipingExtendSec() : 30);
        auction.setCreatedBy(request.createdBy());
        auction.setUpdatedAt(now);
    }

    private int countNonBlankUrls(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return 0;
        }

        int count = 0;
        for (String imageUrl : imageUrls) {
            if (imageUrl != null && !imageUrl.isBlank()) {
                count++;
            }
        }

        return count;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Boolean computeReserveMet(BigDecimal currentPrice, BigDecimal reservePrice) {
        if (reservePrice == null) {
            return null;
        }

        return currentPrice != null && currentPrice.compareTo(reservePrice) >= 0;
    }

    private Map<Long, List<AuctionImageResponse>> loadImagesByAuctionId(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<AuctionImageResponse>> imagesByAuctionId = new LinkedHashMap<>();
        for (AuctionImageEntity image : auctionImageRepository.findByAuctionIdInOrderByAuctionIdAscDisplayOrderAsc(auctionIds)) {
            imagesByAuctionId
                    .computeIfAbsent(image.getAuctionId(), ignored -> new ArrayList<>())
                    .add(toImageResponse(image));
        }

        return imagesByAuctionId;
    }

    private List<AuctionImageResponse> loadImages(Long auctionId) {
        return auctionImageRepository.findByAuctionIdOrderByDisplayOrderAsc(auctionId).stream()
                .map(this::toImageResponse)
                .toList();
    }

    private Map<Long, Long> loadWatcherCountsByAuctionId(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return auctionWatchlistRepository.findWatcherCountsByAuctionIds(auctionIds).stream()
                .collect(java.util.stream.Collectors.toMap(
                        AuctionWatchlistRepository.AuctionWatchlistCountView::getAuctionId,
                        AuctionWatchlistRepository.AuctionWatchlistCountView::getWatcherCount
                ));
    }

    private List<Long> loadWatchedAuctionIds(Long currentUserId, List<Long> auctionIds) {
        if (currentUserId == null || auctionIds.isEmpty()) {
            return List.of();
        }

        return auctionWatchlistRepository.findWatchedAuctionIdsByUserIdAndAuctionIds(currentUserId, auctionIds);
    }

    private long watcherCountForAuction(Long auctionId) {
        return loadWatcherCountsByAuctionId(List.of(auctionId)).getOrDefault(auctionId, 0L);
    }

    private Map<Long, BidEntity> loadTopBidsByAuctionId(List<Long> auctionIds) {
        if (auctionIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, BidEntity> topBidsByAuctionId = new LinkedHashMap<>();
        for (Long auctionId : new HashSet<>(auctionIds)) {
            bidRepository.findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(auctionId)
                    .ifPresent(bid -> topBidsByAuctionId.put(auctionId, bid));
        }

        return topBidsByAuctionId;
    }
}
