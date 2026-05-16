package org.nedelcu.cosmin.auction.api.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "bids")
@Getter
@Setter
public class BidEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bids_seq_generator")
    @SequenceGenerator(name = "bids_seq_generator", sequenceName = "bids_seq", allocationSize = 1)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
