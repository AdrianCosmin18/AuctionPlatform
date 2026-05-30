package org.nedelcu.cosmin.auction.api.common.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.common.messaging.RabbitMqConfig;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventEnvelope;
import org.nedelcu.cosmin.auction.shared.event.AuctionEventType;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OutboxPublisher outboxPublisher;

    @Captor
    private ArgumentCaptor<AuctionEventEnvelope> envelopeCaptor;

    @Test
    void publishPendingEventsPublishesEnvelopeAndMarksEventPublished() {
        OutboxEventEntity event = newEvent();
        when(outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));

        outboxPublisher.publishPendingEvents();

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMqConfig.AUCTION_EXCHANGE),
                eq(RabbitMqConfig.AUCTION_ROUTING_KEY),
                envelopeCaptor.capture()
        );

        AuctionEventEnvelope envelope = envelopeCaptor.getValue();
        assertThat(envelope.eventType()).isEqualTo(AuctionEventType.BID_PLACED.name());
        assertThat(envelope.payload()).isEqualTo("{\"auctionId\":10}");
        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
        assertThat(event.getLastError()).isNull();
    }

    @Test
    void publishPendingEventsKeepsEventNewUntilRetryLimitIsReached() {
        OutboxEventEntity event = newEvent();
        when(outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("broker down")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(AuctionEventEnvelope.class));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastError()).isEqualTo("broker down");
        assertThat(event.getPublishedAt()).isNull();
    }

    @Test
    void publishPendingEventsMarksEventFailedAfterThirdAttempt() {
        OutboxEventEntity event = newEvent();
        event.setRetryCount(2);

        when(outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus.NEW))
                .thenReturn(List.of(event));
        doThrow(new AmqpException("still down")).when(rabbitTemplate)
                .convertAndSend(any(String.class), any(String.class), any(AuctionEventEnvelope.class));

        outboxPublisher.publishPendingEvents();

        assertThat(event.getStatus()).isEqualTo(OutboxEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(3);
        assertThat(event.getLastError()).isEqualTo("still down");
    }

    private OutboxEventEntity newEvent() {
        OutboxEventEntity event = new OutboxEventEntity();
        event.setId(1L);
        event.setAggregateType(OutboxAggregateType.AUCTION);
        event.setAggregateId(10L);
        event.setEventType(AuctionEventType.BID_PLACED);
        event.setPayload("{\"auctionId\":10}");
        event.setStatus(OutboxEventStatus.NEW);
        event.setRetryCount(0);
        event.setCreatedAt(OffsetDateTime.now());
        return event;
    }
}
