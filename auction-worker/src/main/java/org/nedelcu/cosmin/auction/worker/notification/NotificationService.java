package org.nedelcu.cosmin.auction.worker.notification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.nedelcu.cosmin.auction.shared.notification.NotificationType;
import org.nedelcu.cosmin.auction.worker.auction.AuctionEntity;
import org.nedelcu.cosmin.auction.worker.auction.AuctionRepository;
import org.nedelcu.cosmin.auction.worker.bid.BidRepository;
import org.nedelcu.cosmin.auction.worker.watchlist.AuctionWatchlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm");

    private final NotificationRepository notificationRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionWatchlistRepository auctionWatchlistRepository;

    @Transactional
    public void handleBidPlaced(BidPlacedEvent payload) {
        auctionRepository.findById(payload.auctionId()).ifPresent(auction -> {
            if (auction.getCreatedBy() != null && !auction.getCreatedBy().equals(payload.bidderId())) {
                saveNotification(
                        auction.getCreatedBy(),
                        payload.auctionId(),
                        NotificationType.NEW_BID_ON_OWN_AUCTION,
                        "New bid on your auction",
                        "Auction #" + payload.auctionId() + " received a new bid of " + formatAmount(payload.amount()) + "."
                );
            }

            Set<Long> outbidUsers = new LinkedHashSet<>(bidRepository.findDistinctBidderIdsByAuctionId(payload.auctionId()));
            outbidUsers.remove(payload.bidderId());
            if (auction.getCreatedBy() != null) {
                outbidUsers.remove(auction.getCreatedBy());
            }

            for (Long userId : outbidUsers) {
                saveNotification(
                        userId,
                        payload.auctionId(),
                        NotificationType.OUTBID,
                        "You were outbid",
                        "Auction #" + payload.auctionId() + " now has a higher bid of " + formatAmount(payload.currentPrice()) + "."
                );
            }
        });
    }

    @Transactional
    public void handleAuctionExtended(AuctionExtendedEvent payload) {
        Set<Long> recipients = recipientsForAuction(payload.auctionId());

        for (Long userId : recipients) {
            saveNotification(
                    userId,
                    payload.auctionId(),
                    NotificationType.AUCTION_EXTENDED,
                    "Auction extended",
                    "Auction #" + payload.auctionId() + " was extended until " + DATE_TIME_FORMATTER.format(payload.newEndTime()) + "."
            );
        }
    }

    @Transactional
    public void handleAuctionClosed(AuctionClosedEvent payload) {
        AuctionEntity auction = auctionRepository.findById(payload.auctionId()).orElse(null);
        Set<Long> losingBidders = new LinkedHashSet<>(bidRepository.findDistinctBidderIdsByAuctionId(payload.auctionId()));

        if (payload.winnerId() != null) {
            saveNotification(
                    payload.winnerId(),
                    payload.auctionId(),
                    NotificationType.AUCTION_WON,
                    "You won the auction",
                    "You won auction #" + payload.auctionId() + " at " + formatAmount(payload.finalPrice()) + "."
            );
            losingBidders.remove(payload.winnerId());
        }

        for (Long userId : losingBidders) {
            saveNotification(
                    userId,
                    payload.auctionId(),
                    NotificationType.AUCTION_LOST,
                    "Auction closed",
                    "Auction #" + payload.auctionId() + " closed at " + formatAmount(payload.finalPrice()) + " and you did not win."
            );
        }

        if (auction != null && auction.getCreatedBy() != null) {
            saveNotification(
                    auction.getCreatedBy(),
                    payload.auctionId(),
                    NotificationType.AUCTION_CLOSED,
                    "Your auction has closed",
                    "Auction #" + payload.auctionId() + " closed at " + formatAmount(payload.finalPrice()) + "."
            );
        }

        Set<Long> watcherRecipients = new LinkedHashSet<>(auctionWatchlistRepository.findDistinctUserIdsByAuctionId(payload.auctionId()));
        if (payload.winnerId() != null) {
            watcherRecipients.remove(payload.winnerId());
        }
        watcherRecipients.removeAll(losingBidders);
        if (auction != null && auction.getCreatedBy() != null) {
            watcherRecipients.remove(auction.getCreatedBy());
        }

        for (Long userId : watcherRecipients) {
            saveNotification(
                    userId,
                    payload.auctionId(),
                    NotificationType.AUCTION_CLOSED,
                    "Watched auction closed",
                    "Auction #" + payload.auctionId() + " closed at " + formatAmount(payload.finalPrice()) + "."
            );
        }
    }

    private Set<Long> recipientsForAuction(Long auctionId) {
        Set<Long> recipients = new LinkedHashSet<>(auctionWatchlistRepository.findDistinctUserIdsByAuctionId(auctionId));
        recipients.addAll(bidRepository.findDistinctBidderIdsByAuctionId(auctionId));
        auctionRepository.findById(auctionId)
                .map(AuctionEntity::getCreatedBy)
                .ifPresent(recipients::add);
        return recipients;
    }

    private void saveNotification(Long userId, Long auctionId, NotificationType type, String title, String message) {
        if (userId == null) {
          return;
        }

        notificationRepository.save(NotificationEntity.builder()
                .userId(userId)
                .auctionId(auctionId)
                .type(type)
                .title(title)
                .message(message)
                .read(false)
                .createdAt(OffsetDateTime.now())
                .readAt(null)
                .build());
    }

    private String formatAmount(BigDecimal amount) {
        return amount != null ? amount.setScale(2, java.math.RoundingMode.HALF_UP) + " EUR" : "-";
    }
}
