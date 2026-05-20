package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.ForbiddenOrderActionException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderAccessPolicyTest {

    private final OrderAccessPolicy policy = new OrderAccessPolicy();

    @Test
    void requireParticipantAllowsSellerAndBuyer() {
        BidmartOrder order = BidmartOrder.create(
                "auction-1",
                "listing-1",
                "seller-1",
                "buyer-1",
                new BigDecimal("100"),
                "Address",
                null
        );

        assertDoesNotThrow(() -> policy.requireParticipant(order, "seller-1"));
        assertDoesNotThrow(() -> policy.requireParticipant(order, "buyer-1"));
    }

    @Test
    void requireParticipantRejectsOutsider() {
        BidmartOrder order = BidmartOrder.create(
                "auction-2",
                "listing-2",
                "seller-2",
                "buyer-2",
                new BigDecimal("200"),
                "Address",
                null
        );

        assertThrows(ForbiddenOrderActionException.class, () -> policy.requireParticipant(order, "stranger-2"));
    }

    @Test
    void requireSellerRejectsBuyer() {
        assertThrows(ForbiddenOrderActionException.class, () -> policy.requireSeller("buyer-3", "seller-3"));
    }
}
