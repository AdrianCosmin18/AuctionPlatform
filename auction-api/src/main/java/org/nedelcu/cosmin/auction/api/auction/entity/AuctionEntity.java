package org.nedelcu.cosmin.auction.api.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.shared.event.AuctionCloseReason;

@Entity
@Table(name = "auctions")
@Getter
@Setter
public class AuctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "auctions_seq_generator")
    @SequenceGenerator(name = "auctions_seq_generator", sequenceName = "auctions_seq", allocationSize = 1)
    private Long id;

    private String title;

    private String description;

    @Column(name = "category_code")
    private String categoryCode;

    @Column(name = "subcategory_code")
    private String subcategoryCode;

    @Column(name = "creator_author")
    private String creatorAuthor;

    @Column(name = "estimated_year")
    private Integer estimatedYear;

    @Column(name = "language_code")
    private String languageCode;

    @Column(name = "item_condition")
    private String itemCondition;

    @Column(name = "authenticity_status")
    private String authenticityStatus;

    private String provenance;

    @Column(name = "start_price")
    private BigDecimal startPrice;

    @Column(name = "current_price")
    private BigDecimal currentPrice;

    @Column(name = "min_increment")
    private BigDecimal minIncrement;

    @Enumerated(EnumType.STRING)
    private AuctionStatus status;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "anti_sniping_window_sec")
    private Integer antiSnipingWindowSec;

    @Column(name = "anti_sniping_extend_sec")
    private Integer antiSnipingExtendSec;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "winner_id")
    private Long winnerId;

    @Column(name = "winning_bid_id")
    private Long winningBidId;

    @Column(name = "final_price")
    private BigDecimal finalPrice;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_reason")
    private AuctionCloseReason closedReason;

    @Version
    private Long version;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
