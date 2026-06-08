package org.nedelcu.cosmin.auction.worker.auction;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionRepository extends JpaRepository<AuctionEntity, Long> {
}
