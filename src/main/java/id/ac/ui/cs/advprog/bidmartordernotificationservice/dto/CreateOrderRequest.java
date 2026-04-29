package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import java.math.BigDecimal;

public record CreateOrderRequest(
        String auctionId,
        String listingId,
        String sellerId,
        String buyerId,
        BigDecimal finalPrice,
        String shippingAddress
) {
}
