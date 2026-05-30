package org.nedelcu.cosmin.auction.worker.audit;

import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditService {

    private static final String SOURCE = "AUCTION_WORKER";

    private final AuditEventRepository auditEventRepository;

    @Transactional
    public void save(String eventType, Long aggregateId, String payload) {
        AuditEventEntity event = AuditEventEntity.builder()
                .eventType(eventType)
                .aggregateId(aggregateId)
                .payload(payload)
                .processedAt(OffsetDateTime.now())
                .source(SOURCE)
                .build();

        auditEventRepository.save(event);
    }
}
