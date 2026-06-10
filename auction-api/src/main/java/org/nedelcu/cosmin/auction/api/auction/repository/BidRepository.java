package org.nedelcu.cosmin.auction.api.auction.repository;

import java.util.List;
import java.util.Optional;
import org.nedelcu.cosmin.auction.api.auction.entity.BidEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BidRepository extends JpaRepository<BidEntity, Long> {

    interface CategoryMetricView {
        String getCategoryCode();

        long getMetricCount();
    }

    List<BidEntity> findByAuctionIdOrderByCreatedAtDesc(Long auctionId);

    List<BidEntity> findByBidderIdOrderByCreatedAtDesc(Long bidderId);

    Optional<BidEntity> findTopByAuctionIdOrderByAmountDescCreatedAtAscIdAsc(Long auctionId);

    @Query("""
           select a.categoryCode as categoryCode, count(b.id) as metricCount
           from BidEntity b
           join AuctionEntity a on a.id = b.auctionId
           group by a.categoryCode
           order by count(b.id) desc, a.categoryCode asc
           """)
    List<CategoryMetricView> countBidsByCategory();
}
