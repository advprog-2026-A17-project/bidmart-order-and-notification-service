package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner;

public final class NotificationTemplateFactory {

    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String BID_PLACED = "BID_PLACED";
    public static final String OUTBID = "OUTBID";
    public static final String AUCTION_WON = "AUCTION_WON";
    public static final String AUCTION_ENDED = "AUCTION_ENDED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String ORDER_DISPUTED = "ORDER_DISPUTED";
    public static final String ORDER_DISPUTE_RESOLVED = "ORDER_DISPUTE_RESOLVED";
    public static final String WALLET_PAYOUT_RELEASED = "WALLET_PAYOUT_RELEASED";

    private NotificationTemplateFactory() {
    }

    public static NotificationEnvelope orderCreated(BidmartOrder order) {
        return new NotificationEnvelope(
                ORDER_CREATED,
                "Order created",
                "Your winning auction has been converted into an order.",
                sourceEventId(order, ORDER_CREATED)
        );
    }

    public static NotificationEnvelope bidPlaced(String auctionId, long amountCents, String eventId) {
        return new NotificationEnvelope(
                BID_PLACED,
                "Bid placed",
                "Your bid of " + formatIdrFromCents(amountCents) + " was placed on auction " + auctionId + ".",
                sourceEventId(eventId, BID_PLACED)
        );
    }

    public static NotificationEnvelope outbid(String auctionId, long amountCents, String eventId) {
        return new NotificationEnvelope(
                OUTBID,
                "Outbid",
                "A higher bid of " + formatIdrFromCents(amountCents) + " was placed on auction " + auctionId + ".",
                sourceEventId(eventId, OUTBID)
        );
    }

    public static NotificationEnvelope auctionWon(String auctionId, String eventId) {
        return new NotificationEnvelope(AUCTION_WON, "Auction won", "You won auction " + auctionId + ".", sourceEventId(eventId, AUCTION_WON));
    }

    public static NotificationEnvelope orderDisputed(BidmartOrder order) {
        return new NotificationEnvelope(
                ORDER_DISPUTED,
                "Order disputed",
                "Buyer opened a dispute for order " + order.getId() + ".",
                sourceEventId(order, ORDER_DISPUTED)
        );
    }

    public static NotificationEnvelope disputeResolvedForBuyer(BidmartOrder order) {
        String message = order.getDisputeWinner() == DisputeWinner.BUYER
                ? "Your dispute was resolved in your favor."
                : "Your dispute was resolved in favor of the seller.";
        return new NotificationEnvelope(
                ORDER_DISPUTE_RESOLVED,
                "Dispute resolved",
                message,
                sourceEventId(order, ORDER_DISPUTE_RESOLVED + ":buyer")
        );
    }

    public static NotificationEnvelope disputeResolvedForSeller(BidmartOrder order) {
        return new NotificationEnvelope(
                ORDER_DISPUTE_RESOLVED,
                "Dispute resolved",
                "Dispute for order " + order.getId() + " has been resolved.",
                sourceEventId(order, ORDER_DISPUTE_RESOLVED + ":seller")
        );
    }

    public static NotificationEnvelope sellerPayoutReleased(BidmartOrder order) {
        return new NotificationEnvelope(
                WALLET_PAYOUT_RELEASED,
                "Payout released",
                "Payout for order " + order.getId() + " has been released to your active balance.",
                sourceEventId(order, WALLET_PAYOUT_RELEASED)
        );
    }

    public static NotificationEnvelope userDisabled(String userId, String email, String dedupeKey) {
        String message = email == null || email.isBlank()
                ? "Your account has been disabled by an administrator."
                : "Your account (" + email + ") has been disabled by an administrator.";
        return new NotificationEnvelope(
                USER_DISABLED,
                "Account disabled",
                message,
                dedupeKey.isBlank() ? userId + ":" + USER_DISABLED : dedupeKey
        );
    }

    public static NotificationEnvelope auctionEnded(String auctionId, boolean sold, String eventId) {
        String message = sold
                ? "Auction " + auctionId + " ended with a winner."
                : "Auction " + auctionId + " ended without a winner.";
        return new NotificationEnvelope(AUCTION_ENDED, "Auction ended", message, sourceEventId(eventId, AUCTION_ENDED));
    }

    private static String sourceEventId(BidmartOrder order, String type) {
        String source = order.getSourceEventId() == null ? order.getId() : order.getSourceEventId();
        return source + ":" + type;
    }

    private static String sourceEventId(String eventId, String type) {
        return eventId + ":" + type;
    }

    public static String formatIdrFromCents(long amountCents) {
        long major = amountCents / 100;
        long minor = Math.abs(amountCents % 100);
        return String.format("IDR %d.%02d", major, minor);
    }
}
