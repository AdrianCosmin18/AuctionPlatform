package org.nedelcu.cosmin.auction.worker.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuctionEventConsumer {

    @RabbitListener(queues = RabbitMqConfig.AUCTION_EVENTS_QUEUE)
    public void consume(String message) {
        log.info("Received auction event: {}", message);
    }
}
