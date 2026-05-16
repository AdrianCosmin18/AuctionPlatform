package org.nedelcu.cosmin.auction.api.common.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {

    List<OutboxEventEntity> findTop20ByStatusOrderByCreatedAtAsc(OutboxEventStatus status);
}
