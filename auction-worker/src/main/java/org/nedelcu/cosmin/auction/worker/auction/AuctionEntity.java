package org.nedelcu.cosmin.auction.worker.auction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class AuctionEntity {

    @Id
    private Long id;

    @Column(name = "created_by")
    private Long createdBy;
}
