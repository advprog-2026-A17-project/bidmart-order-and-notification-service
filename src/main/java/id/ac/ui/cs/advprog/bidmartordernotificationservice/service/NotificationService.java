package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> listForUser(String userId);

    NotificationResponse getForUser(String userId, String notificationId);

    NotificationResponse updateReadStatus(String userId, String notificationId, boolean read);

    NotificationResponse notifyOrderCreated(BidmartOrder order);

    NotificationResponse notifyBidPlaced(String userId, String auctionId, long amountCents, String eventId);

    NotificationResponse notifyOutbid(String userId, String auctionId, long amountCents, String eventId);

    NotificationResponse notifyAuctionWon(String userId, String auctionId, String eventId);

    NotificationResponse notifyOrderDisputed(BidmartOrder order);

    NotificationResponse notifyDisputeResolved(BidmartOrder order);

    NotificationResponse notifySellerPayoutReleased(BidmartOrder order, long amount);

    NotificationResponse notifyUserDisabled(String userId, String email, String dedupeKey);

    NotificationResponse notifyAuctionEnded(String userId, String auctionId, boolean sold, String eventId);
}
