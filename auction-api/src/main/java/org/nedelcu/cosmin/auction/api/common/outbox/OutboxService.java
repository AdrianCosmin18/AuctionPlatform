package org.nedelcu.cosmin.auction.api.common.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(String aggregateType, Long aggregateId, String eventType, Object payload) {
        try {
            OutboxEventEntity event = new OutboxEventEntity();
            event.setAggregateType(aggregateType);
            event.setAggregateId(aggregateId);
            event.setEventType(eventType);
            event.setPayload(objectMapper.writeValueAsString(payload));
            event.setStatus(OutboxEventStatus.NEW);
            event.setCreatedAt(OffsetDateTime.now());

            outboxEventRepository.save(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not serialize outbox event payload", ex);
        }
    }
}
