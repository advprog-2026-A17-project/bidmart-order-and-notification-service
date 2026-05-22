package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;

public final class OrderStates {

    private static final OrderState CREATED = new CreatedOrderState();
    private static final OrderState PACKED = new PackedOrderState();
    private static final OrderState SHIPPED = new ShippedOrderState();
    private static final OrderState DELIVERED = new DeliveredOrderState();
    private static final OrderState CONFIRMED = new ConfirmedOrderState();
    private static final OrderState DISPUTED = new DisputedOrderState();
    private static final OrderState REFUNDED = new RefundedOrderState();

    private OrderStates() {
    }

    public static OrderState forStatus(OrderStatus status) {
        return switch (status) {
            case CREATED -> CREATED;
            case PACKED -> PACKED;
            case SHIPPED -> SHIPPED;
            case DELIVERED -> DELIVERED;
            case CONFIRMED -> CONFIRMED;
            case DISPUTED -> DISPUTED;
            case REFUNDED -> REFUNDED;
        };
    }

    private static final class CreatedOrderState implements OrderState {
        @Override
        public void updateShipping(BidmartOrder order, OrderStatus nextStatus, String trackingNumber, String carrier) {
            order.updateShipping(nextStatus, trackingNumber, carrier);
        }
    }

    private static final class PackedOrderState implements OrderState {
        @Override
        public void updateShipping(BidmartOrder order, OrderStatus nextStatus, String trackingNumber, String carrier) {
            order.updateShipping(nextStatus, trackingNumber, carrier);
        }
    }

    private static class ShippedOrderState implements OrderState {
        @Override
        public void confirmReceipt(BidmartOrder order) {
            order.confirmReceipt();
        }

        @Override
        public void openDispute(BidmartOrder order, String reason, String details) {
            order.openDispute(reason, details);
        }
    }

    private static final class DeliveredOrderState extends ShippedOrderState {
    }

    private static final class ConfirmedOrderState implements OrderState {
        @Override
        public void updateShipping(BidmartOrder order, OrderStatus nextStatus, String trackingNumber, String carrier) {
            order.updateShipping(nextStatus, trackingNumber, carrier);
        }
    }

    private static final class DisputedOrderState implements OrderState {
        @Override
        public void resolveDispute(BidmartOrder order, DisputeWinner winner, String resolvedBy) {
            order.resolveDispute(winner, resolvedBy);
        }
    }

    private static final class RefundedOrderState implements OrderState {
    }
}
