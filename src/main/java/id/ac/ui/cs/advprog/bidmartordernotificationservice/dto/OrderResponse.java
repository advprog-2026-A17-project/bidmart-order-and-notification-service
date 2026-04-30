package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        String id,
        String auctionId,
        String listingId,
        String sellerId,
        String buyerId,
        BigDecimal finalPrice,
        String shippingAddress,
        OrderStatus status,
        String shippingStatus,
        String trackingNumber,
        String carrier,
        Instant createdAt,
        Instant updatedAt
) {

    public static OrderResponse from(BidmartOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getAuctionId(),
                order.getListingId(),
                order.getSellerId(),
                order.getBuyerId(),
                order.getFinalPrice(),
                order.getShippingAddress(),
                order.getStatus(),
                shippingStatusFor(order.getStatus()),
                order.getTrackingNumber(),
                order.getCarrier(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private static String shippingStatusFor(OrderStatus status) {
        if (status == OrderStatus.CREATED) {
            return "PENDING";
        }
        return status.name();
    }
}
