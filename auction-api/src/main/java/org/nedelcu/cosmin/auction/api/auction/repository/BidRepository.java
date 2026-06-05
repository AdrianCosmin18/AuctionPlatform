package org.nedelcu.cosmin.auction.api.auction.repository;

import java.util.List;
import java.util.Optional;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<BidEntity, Long> {

    List<BidEntity> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    Optional<BidEntity> findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(Long auctionId);
}
