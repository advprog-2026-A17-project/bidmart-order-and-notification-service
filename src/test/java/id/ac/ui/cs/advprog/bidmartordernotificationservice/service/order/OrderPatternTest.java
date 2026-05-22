package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.order;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.OrderStatus;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification.NotificationTemplateFactory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPatternTest {

    @Test
    void orderStateAllowsPackedToShippedAndRejectsCreatedConfirmation() {
        BidmartOrder order = BidmartOrder.create(
                "auction-1",
                "listing-1",
                "seller-1",
                "buyer-1",
                new BigDecimal("10000"),
                "Address",
                null
        );

        assertThrows(IllegalArgumentException.class,
                () -> OrderStates.forStatus(order.getStatus()).confirmReceipt(order));

        OrderStates.forStatus(order.getStatus()).updateShipping(order, OrderStatus.PACKED, "TRK-1", "JNE");
        OrderStates.forStatus(order.getStatus()).updateShipping(order, OrderStatus.SHIPPED, "TRK-2", "JNE");

        assertEquals(OrderStatus.SHIPPED, order.getStatus());
    }

    @Test
    void notificationTemplatePreservesBidPlacedMessageShape() {
        var envelope = NotificationTemplateFactory.bidPlaced("auction-2", 1250000L, "evt-1");

        assertEquals("BID_PLACED", envelope.type());
        assertEquals("evt-1:BID_PLACED", envelope.sourceEventId());
        assertEquals("Your bid of IDR 12500.00 was placed on auction auction-2.", envelope.message());
    }
}
