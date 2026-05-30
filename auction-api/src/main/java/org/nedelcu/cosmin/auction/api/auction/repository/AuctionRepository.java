package org.nedelcu.cosmin.auction.api.auction.repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.nedelcu.cosmin.auction.api.auction.entity.AuctionEntity;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuctionRepository extends JpaRepository<AuctionEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AuctionEntity a where a.id = :id")
    Optional<AuctionEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
           select a.id
           from AuctionEntity a
           where a.status = :status
             and a.endTime <= :now
           """)
    List<Long> findExpiredAuctionIds(
            @Param("status") AuctionStatus status,
            @Param("now") OffsetDateTime now
    );
}
