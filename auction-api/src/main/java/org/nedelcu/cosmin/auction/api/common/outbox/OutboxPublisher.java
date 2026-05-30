package org.nedelcu.cosmin.auction.api.common.outbox;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.common.messaging.RabbitMqConfig;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventEnvelope;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        var events = outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW);

        for (OutboxEventEntity event : events) {
            try {
                AuctionEventEnvelope envelope = new AuctionEventEnvelope(
                        event.getEventType().name(),
                        event.getPayload()
                );

                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.AUCTION_EXCHANGE,
                        RabbitMqConfig.AUCTION_ROUTING_KEY,
                        envelope
                );

                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(OffsetDateTime.now());
                event.setLastError(null);
            } catch (Exception ex) {
                int retries = event.getRetryCount() == null ? 1 : event.getRetryCount() + 1;
                event.setRetryCount(retries);
                event.setLastError(ex.getMessage());
                event.setPublishedAt(null);

                if (retries >= 3) {
                    event.setStatus(OutboxEventStatus.FAILED);
                } else {
                    event.setStatus(OutboxEventStatus.NEW);
                }
            }
        }
    }
}
