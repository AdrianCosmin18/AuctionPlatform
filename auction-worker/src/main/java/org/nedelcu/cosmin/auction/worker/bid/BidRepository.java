package org.nedelcu.cosmin.auction.worker.bid;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BidRepository extends JpaRepository<BidEntity, Long> {

    @Query("""
            select distinct b.bidderId
            from BidEntity b
            where b.auctionId = :auctionId
            """)
    List<Long> findDistinctBidderIdsByAuctionId(@Param("auctionId") Long auctionId);
}
