package id.ac.ui.cs.advprog.bidmartordernotificationservice.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BidmartOrderTest {

    private static BidmartOrder newOrder() {
        return BidmartOrder.create(
                "auction-1",
                "listing-1",
                "seller-1",
                "buyer-1",
                new BigDecimal("12500"),
                "Jl. Test 123",
                "evt-1"
        );
    }

    @Test
    void orderLifecycleShouldSupportShippingConfirmationAndPayoutRelease() {
        BidmartOrder order = newOrder();

        order.updateShipping(OrderStatus.PACKED, "TRK-1", "JNE");
        assertEquals(OrderStatus.PACKED, order.getStatus());
        assertEquals("TRK-1", order.getTrackingNumber());
        assertEquals("JNE", order.getCarrier());

        order.updateShipping(OrderStatus.SHIPPED, "TRK-1", "JNE");
        assertEquals(OrderStatus.SHIPPED, order.getStatus());

        order.confirmReceipt();
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertNotNull(order.getConfirmedAt());

        assertDoesNotThrow(order::markPayoutReleased);
        assertNotNull(order.getPayoutReleasedAt());
        assertNotNull(order.getUpdatedAt());
    }

    @Test
    void updateShippingShouldRejectInvalidTransitionsAndStatus() {
        BidmartOrder order = newOrder();

        assertThrows(IllegalArgumentException.class, () -> order.updateShipping(null, "T", "C"));
        assertThrows(IllegalArgumentException.class, () -> order.updateShipping(OrderStatus.CREATED, "T", "C"));

        // Cannot jump directly to SHIPPED from CREATED
        assertThrows(IllegalArgumentException.class, () -> order.updateShipping(OrderStatus.SHIPPED, "T", "C"));

        // After confirmed, shipping update is forbidden
        order.updateShipping(OrderStatus.PACKED, "T", "C");
        order.updateShipping(OrderStatus.SHIPPED, "T", "C");
        order.confirmReceipt();
        assertThrows(IllegalArgumentException.class, () -> order.updateShipping(OrderStatus.PACKED, "T2", "C2"));
    }

    @Test
    void updateShippingShouldRejectPackedWhenAlreadyPacked() {
        BidmartOrder order = newOrder();
        order.updateShipping(OrderStatus.PACKED, "TRK-1", "JNE");

        assertThrows(IllegalArgumentException.class, () -> order.updateShipping(OrderStatus.PACKED, "TRK-2", "JNE"));
    }

    @Test
    void confirmReceiptShouldRequireShippedStatus() {
        BidmartOrder order = newOrder();
        assertThrows(IllegalArgumentException.class, order::confirmReceipt);
    }

    @Test
    void openDisputeShouldRequireShippedStatus() {
        BidmartOrder order = newOrder();
        assertThrows(IllegalArgumentException.class, () -> order.openDispute("Missing package", "Details"));
    }

    @Test
    void openDisputeShouldMoveOrderToDisputed() {
        BidmartOrder order = newOrder();
        order.updateShipping(OrderStatus.PACKED, "TRK", "JNE");
        order.updateShipping(OrderStatus.SHIPPED, "TRK", "JNE");

        order.openDispute("Never received", "Still waiting");

        assertEquals(OrderStatus.DISPUTED, order.getStatus());
        assertEquals("Never received", order.getDisputeReason());
        assertNotNull(order.getDisputeOpenedAt());
    }
}

