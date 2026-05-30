package org.nedelcu.cosmin.auction.api.auction.scheduler;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.service.AuctionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    @Scheduled(fixedDelay = 5000)
    public void closeExpiredAuctions() {
        OffsetDateTime now = OffsetDateTime.now();
        var expiredAuctionIds = auctionRepository.findExpiredAuctionIds(AuctionStatus.RUNNING, now);

        for (Long auctionId : expiredAuctionIds) {
            try {
                auctionService.closeExpiredAuction(auctionId);
                log.info("Auto-closed expired auction id={}", auctionId);
            } catch (Exception ex) {
                log.error("Failed to auto-close auction id={}", auctionId, ex);
            }
        }
    }
}
