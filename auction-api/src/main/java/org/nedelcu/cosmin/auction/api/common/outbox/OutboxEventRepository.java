package org.nedelcu.cosmin.auction.api.common.outbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long> {
}
