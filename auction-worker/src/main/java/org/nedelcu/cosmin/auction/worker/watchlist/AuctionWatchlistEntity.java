package org.nedelcu.cosmin.auction.worker.watchlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auction_watchlist")
@Getter
@Setter
public class AuctionWatchlistEntity {

    @Id
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "auction_id")
    private Long auctionId;
}
