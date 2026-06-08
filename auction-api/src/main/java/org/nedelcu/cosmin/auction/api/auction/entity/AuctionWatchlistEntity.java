package org.nedelcu.cosmin.auction.api.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auction_watchlist")
@Getter
@Setter
public class AuctionWatchlistEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_watchlist_seq_generator")
    @SequenceGenerator(name = "auction_watchlist_seq_generator", sequenceName = "auction_watchlist_seq", allocationSize = 1)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
