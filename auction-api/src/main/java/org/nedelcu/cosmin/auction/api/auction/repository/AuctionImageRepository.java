package org.nedelcu.cosmin.auction.api.auction.repository;

import java.util.Collection;
import java.util.List;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionImageRepository extends JpaRepository<AuctionImageEntity, Long> {

    List<AuctionImageEntity> findByAuctionIdInOrderByAuctionIdAscDisplayOrderAsc(Collection<Long> auctionIds);

    List<AuctionImageEntity> findByAuctionIdOrderByDisplayOrderAsc(Long auctionId);
}
