package org.nedelcu.cosmin.auction.api.common.outbox;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.nedelcu.cosmin.auction.api.common.messaging.RabbitMqConfig;
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
                rabbitTemplate.convertAndSend(
                        RabbitMqConfig.AUCTION_EXCHANGE,
                        RabbitMqConfig.AUCTION_ROUTING_KEY,
                        event.getPayload()
                );

                event.setStatus(OutboxEventStatus.PUBLISHED);
                event.setPublishedAt(OffsetDateTime.now());
            } catch (Exception ex) {
                event.setStatus(OutboxEventStatus.FAILED);
            }
        }
    }
}
