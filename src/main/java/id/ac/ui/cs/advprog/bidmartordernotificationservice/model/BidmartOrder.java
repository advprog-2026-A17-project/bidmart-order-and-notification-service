package id.ac.ui.cs.advprog.bidmartordernotificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class BidmartOrder {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String auctionId;

    @Column(nullable = false)
    private String listingId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String buyerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal finalPrice;

    @Column(nullable = false)
    private String shippingAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column
    private String trackingNumber;

    @Column
    private String carrier;

    @Column
    private Instant confirmedAt;

    @Column
    private Instant payoutReleasedAt;

    @Column(unique = true)
    private String sourceEventId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private String disputeReason;

    @Column
    private String disputeDetails;

    @Column
    private String sellerDisputeResponse;

    @Enumerated(EnumType.STRING)
    @Column
    private DisputeWinner disputeWinner;

    @Column
    private Instant disputeOpenedAt;

    @Column
    private Instant disputeResolvedAt;

    @Column
    private String disputeResolvedBy;

    @Version
    private long version;

    public static BidmartOrder create(
            String auctionId,
            String listingId,
            String sellerId,
            String buyerId,
            BigDecimal finalPrice,
            String shippingAddress,
            String sourceEventId
    ) {
        Instant now = Instant.now();
        BidmartOrder order = new BidmartOrder();
        order.id = UUID.randomUUID().toString();
        order.auctionId = auctionId;
        order.listingId = listingId;
        order.sellerId = sellerId;
        order.buyerId = buyerId;
        order.finalPrice = finalPrice;
        order.shippingAddress = shippingAddress;
        order.status = OrderStatus.CREATED;
        order.sourceEventId = sourceEventId;
        order.createdAt = now;
        order.updatedAt = now;
        return order;
    }

    public void updateShipping(OrderStatus nextStatus, String trackingNumber, String carrier) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Shipping status is required");
        }

        if (nextStatus != OrderStatus.PACKED && nextStatus != OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("Seller can only set PACKED or SHIPPED status");
        }

        if (this.status == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Cannot update shipping after order confirmation");
        }

        if (nextStatus == OrderStatus.PACKED && this.status != OrderStatus.CREATED) {
            throw new IllegalArgumentException("Order must be CREATED before it can be PACKED");
        }

        if (nextStatus == OrderStatus.SHIPPED && this.status != OrderStatus.PACKED) {
            throw new IllegalArgumentException("Order must be PACKED before it can be SHIPPED");
        }

        this.status = nextStatus;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.updatedAt = Instant.now();
    }

    public void confirmReceipt() {
        if (this.status != OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("Order must be shipped before confirmation");
        }
        this.status = OrderStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void openDispute(String reason, String details) {
        if (this.status != OrderStatus.SHIPPED && this.status != OrderStatus.DELIVERED) {
            throw new IllegalArgumentException("Disputes can only be opened for shipped orders");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Dispute reason is required");
        }
        this.status = OrderStatus.DISPUTED;
        this.disputeReason = reason.trim();
        this.disputeDetails = details == null ? null : details.trim();
        this.disputeOpenedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void resolveDispute(DisputeWinner winner, String resolvedBy) {
        if (this.status != OrderStatus.DISPUTED) {
            throw new IllegalArgumentException("Order is not in dispute");
        }
        if (winner == null) {
            throw new IllegalArgumentException("Dispute winner is required");
        }
        this.disputeWinner = winner;
        this.disputeResolvedAt = Instant.now();
        this.disputeResolvedBy = resolvedBy;
        if (winner == DisputeWinner.BUYER) {
            this.status = OrderStatus.REFUNDED;
        } else {
            this.status = OrderStatus.CONFIRMED;
            this.confirmedAt = Instant.now();
        }
        this.updatedAt = Instant.now();
    }

    public void markPayoutReleased() {
        this.payoutReleasedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getListingId() {
        return listingId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public String getCarrier() {
        return carrier;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public Instant getPayoutReleasedAt() {
        return payoutReleasedAt;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getDisputeReason() {
        return disputeReason;
    }

    public String getDisputeDetails() {
        return disputeDetails;
    }

    public String getSellerDisputeResponse() {
        return sellerDisputeResponse;
    }

    public DisputeWinner getDisputeWinner() {
        return disputeWinner;
    }

    public Instant getDisputeOpenedAt() {
        return disputeOpenedAt;
    }

    public Instant getDisputeResolvedAt() {
        return disputeResolvedAt;
    }

    public String getDisputeResolvedBy() {
        return disputeResolvedBy;
    }
}
