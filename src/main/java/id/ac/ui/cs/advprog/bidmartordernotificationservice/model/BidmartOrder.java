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

    @Column(unique = true)
    private String sourceEventId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

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
        if (nextStatus == OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Buyer confirmation is required to confirm an order");
        }
        this.status = nextStatus;
        this.trackingNumber = trackingNumber;
        this.carrier = carrier;
        this.updatedAt = Instant.now();
    }

    public void confirmReceipt() {
        this.status = OrderStatus.CONFIRMED;
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

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
