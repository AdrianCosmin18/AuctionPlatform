package org.nedelcu.cosmin.auction.api.common.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuctionEventBroadcaster {

    private static final String AUCTION_TOPIC_PREFIX = "/topic/auctions/";

    private final SimpMessagingTemplate messagingTemplate;

    public void broadcastToAuction(Long auctionId, Object event) {
        messagingTemplate.convertAndSend(AUCTION_TOPIC_PREFIX + auctionId, event);
    }
}
