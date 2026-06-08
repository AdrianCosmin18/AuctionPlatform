package org.nedelcu.cosmin.auction.worker.watchlist;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionWatchlistRepository extends JpaRepository<AuctionWatchlistEntity, Long> {

    @Query("""
            select distinct w.userId
            from AuctionWatchlistEntity w
            where w.auctionId = :auctionId
            """)
    List<Long> findDistinctUserIdsByAuctionId(@Param("auctionId") Long auctionId);
}
