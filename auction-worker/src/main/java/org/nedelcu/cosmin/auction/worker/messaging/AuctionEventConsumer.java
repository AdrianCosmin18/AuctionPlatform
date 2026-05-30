package org.nedelcu.cosmin.auction.worker.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nedelcu.cosmin.auction.shared.event.AuctionClosedEvent;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventEnvelope;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.nedelcu.cosmin.auction.shared.event.AuctionExtendedEvent;
import org.nedelcu.cosmin.auction.shared.event.BidPlacedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionEventConsumer {

    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.AUCTION_EVENTS_QUEUE)
    public void consume(AuctionEventEnvelope event) throws Exception {
        log.info("Received auction event type={}", event.eventType());

        switch (AuctionEventType.valueOf(event.eventType())) {
            case BID_PLACED -> {
                BidPlacedEvent payload = objectMapper.readValue(event.payload(), BidPlacedEvent.class);
                log.info("Processing bid placed: auctionId={}, amount={}",
                        payload.auctionId(), payload.amount());
            }
            case AUCTION_EXTENDED -> {
                AuctionExtendedEvent payload = objectMapper.readValue(event.payload(), AuctionExtendedEvent.class);
                log.info("Processing auction extended: auctionId={}, newEndTime={}",
                        payload.auctionId(), payload.newEndTime());
            }
            case AUCTION_CLOSED -> {
                AuctionClosedEvent payload = objectMapper.readValue(event.payload(), AuctionClosedEvent.class);
                log.info("Processing auction closed: auctionId={}, finalPrice={}",
                        payload.auctionId(), payload.finalPrice());
            }
        }
    }
}
