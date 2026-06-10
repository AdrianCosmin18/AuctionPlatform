package org.nedelcu.cosmin.auction.api.auction.repository;

import java.util.List;
import java.util.Optional;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionWatchlistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionWatchlistRepository extends JpaRepository<AuctionWatchlistEntity, Long> {

    interface CategoryMetricView {
        String getCategoryCode();

        long getMetricCount();
    }

    boolean existsByUserIdAndAuctionId(Long userId, Long auctionId);

    Optional<AuctionWatchlistEntity> findByUserIdAndAuctionId(Long userId, Long auctionId);

    List<AuctionWatchlistEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
            select w.auctionId as auctionId, count(w.id) as watcherCount
            from AuctionWatchlistEntity w
            where w.auctionId in :auctionIds
            group by w.auctionId
            """)
    List<AuctionWatchlistCountView> findWatcherCountsByAuctionIds(@Param("auctionIds") List<Long> auctionIds);

    @Query("""
            select w.auctionId
            from AuctionWatchlistEntity w
            where w.userId = :userId and w.auctionId in :auctionIds
            """)
    List<Long> findWatchedAuctionIdsByUserIdAndAuctionIds(@Param("userId") Long userId, @Param("auctionIds") List<Long> auctionIds);

    @Query("""
            select a.categoryCode as categoryCode, count(w.id) as metricCount
            from AuctionWatchlistEntity w
            join AuctionEntity a on a.id = w.auctionId
            group by a.categoryCode
            order by count(w.id) desc, a.categoryCode asc
            """)
    List<CategoryMetricView> countWatchlistEntriesByCategory();

    interface AuctionWatchlistCountView {
        Long getAuctionId();

        long getWatcherCount();
    }
}
