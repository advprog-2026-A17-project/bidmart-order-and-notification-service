package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;

public interface OrderState {

    default void updateShipping(BidmartOrder order, OrderStatus nextStatus, String trackingNumber, String carrier) {
        throw new IllegalArgumentException("Seller can only set PACKED or SHIPPED status");
    }

    default void confirmReceipt(BidmartOrder order) {
        throw new IllegalArgumentException("Order must be shipped before confirmation");
    }

    default void openDispute(BidmartOrder order, String reason, String details) {
        throw new IllegalArgumentException("Disputes can only be opened for shipped orders");
    }

    default void resolveDispute(BidmartOrder order, DisputeWinner winner, String resolvedBy) {
        throw new IllegalArgumentException("Order is not in dispute");
    }
}
