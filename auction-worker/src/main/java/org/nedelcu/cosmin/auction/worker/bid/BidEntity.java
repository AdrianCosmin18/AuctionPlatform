package org.nedelcu.cosmin.auction.worker.bid;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bids")
@Getter
@Setter
public class BidEntity {

    @Id
    private Long id;

    @Column(name = "auction_id")
    private Long auctionId;

    @Column(name = "bidder_id")
    private Long bidderId;
}
