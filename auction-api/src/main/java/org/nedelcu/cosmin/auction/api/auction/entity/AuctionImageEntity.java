package org.nedelcu.cosmin.auction.api.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auction_images")
@Getter
@Setter
public class AuctionImageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auction_images_seq_generator")
    @SequenceGenerator(name = "auction_images_seq_generator", sequenceName = "auction_images_seq", allocationSize = 1)
    private Long id;

    @Column(name = "auction_id", nullable = false)
    private Long auctionId;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
