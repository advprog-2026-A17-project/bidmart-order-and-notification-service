package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;

public record UpdateOrderStatusRequest(
        OrderStatus status,
        String trackingNumber,
        String carrier
) {
}
