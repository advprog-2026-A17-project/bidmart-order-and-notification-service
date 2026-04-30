package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.ForbiddenOrderActionException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import org.springframework.stereotype.Component;

@Component
public class OrderAccessPolicy {

    public void requireSeller(String userId, String sellerId) {
        requireActor(userId, sellerId, "Only the seller can perform this order action");
    }

    public void requireSeller(BidmartOrder order, String userId) {
        requireActor(userId, order.getSellerId(), "Only the seller can perform this order action");
    }

    public void requireBuyer(BidmartOrder order, String userId) {
        requireActor(userId, order.getBuyerId(), "Only the buyer can perform this order action");
    }

    public void requireParticipant(BidmartOrder order, String userId) {
        if (!userId.equals(order.getSellerId()) && !userId.equals(order.getBuyerId())) {
            throw new ForbiddenOrderActionException("Only seller or buyer can view this order");
        }
    }

    private void requireActor(String actualUserId, String requiredUserId, String message) {
        if (!requiredUserId.equals(actualUserId)) {
            throw new ForbiddenOrderActionException(message);
        }
    }
}
