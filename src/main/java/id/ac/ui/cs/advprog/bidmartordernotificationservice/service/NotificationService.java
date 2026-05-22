package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.exception.NotificationNotFoundException;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartOrder;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class NotificationService {

    private static final String ORDER_CREATED = "ORDER_CREATED";
    private static final String BID_PLACED = "BID_PLACED";
    private static final String OUTBID = "OUTBID";
    private static final String AUCTION_WON = "AUCTION_WON";
    private static final String AUCTION_ENDED = "AUCTION_ENDED";
    private static final String USER_DISABLED = "USER_DISABLED";
    private static final String ORDER_DISPUTED = "ORDER_DISPUTED";
    private static final String ORDER_DISPUTE_RESOLVED = "ORDER_DISPUTE_RESOLVED";
    private static final String WALLET_PAYOUT_RELEASED = "WALLET_PAYOUT_RELEASED";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPreferenceService notificationPreferenceService;
    private final ExternalNotificationDispatcher externalNotificationDispatcher;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate,
            NotificationPreferenceService notificationPreferenceService,
            ExternalNotificationDispatcher externalNotificationDispatcher
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
        this.notificationPreferenceService = notificationPreferenceService;
        this.externalNotificationDispatcher = externalNotificationDispatcher;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getForUser(String userId, String notificationId) {
        BidmartNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        return NotificationResponse.from(notification);
    }

    @Transactional
    public NotificationResponse updateReadStatus(String userId, String notificationId, boolean read) {
        BidmartNotification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if (read) {
            notification.markAsRead();
        } else {
            notification.markAsUnread();
        }
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public NotificationResponse notifyOrderCreated(BidmartOrder order) {
        String sourceEventId = sourceEventId(order, ORDER_CREATED);
        return createAndPublish(
                order.getBuyerId(),
                ORDER_CREATED,
                "Order created",
                "Your winning auction has been converted into an order.",
                sourceEventId
        );
    }

    @Transactional
    public NotificationResponse notifyBidPlaced(
            String userId,
            String auctionId,
            long amountCents,
            String eventId
    ) {
        return createAndPublish(
                userId,
                BID_PLACED,
                "Bid placed",
                "Your bid of " + formatIdrFromCents(amountCents) + " was placed on auction " + auctionId + ".",
                sourceEventId(eventId, BID_PLACED)
        );
    }

    @Transactional
    public NotificationResponse notifyOutbid(
            String userId,
            String auctionId,
            long amountCents,
            String eventId
    ) {
        return createAndPublish(
                userId,
                OUTBID,
                "Outbid",
                "A higher bid of " + formatIdrFromCents(amountCents) + " was placed on auction " + auctionId + ".",
                sourceEventId(eventId, OUTBID)
        );
    }

    @Transactional
    public NotificationResponse notifyAuctionWon(String userId, String auctionId, String eventId) {
        return createAndPublish(
                userId,
                AUCTION_WON,
                "Auction won",
                "You won auction " + auctionId + ".",
                sourceEventId(eventId, AUCTION_WON)
        );
    }

    @Transactional
    public NotificationResponse notifyOrderDisputed(BidmartOrder order) {
        return createAndPublish(
                order.getSellerId(),
                ORDER_DISPUTED,
                "Order disputed",
                "Buyer opened a dispute for order " + order.getId() + ".",
                sourceEventId(order, ORDER_DISPUTED)
        );
    }

    @Transactional
    public NotificationResponse notifyDisputeResolved(BidmartOrder order) {
        String buyerMessage = order.getDisputeWinner() == id.ac.ui.cs.advprog.bidmartordernotificationservice.model.DisputeWinner.BUYER
                ? "Your dispute was resolved in your favor."
                : "Your dispute was resolved in favor of the seller.";
        createAndPublish(
                order.getBuyerId(),
                ORDER_DISPUTE_RESOLVED,
                "Dispute resolved",
                buyerMessage,
                sourceEventId(order, ORDER_DISPUTE_RESOLVED + ":buyer")
        );
        return createAndPublish(
                order.getSellerId(),
                ORDER_DISPUTE_RESOLVED,
                "Dispute resolved",
                "Dispute for order " + order.getId() + " has been resolved.",
                sourceEventId(order, ORDER_DISPUTE_RESOLVED + ":seller")
        );
    }

    @Transactional
    public NotificationResponse notifySellerPayoutReleased(BidmartOrder order, long amount) {
        return createAndPublish(
                order.getSellerId(),
                WALLET_PAYOUT_RELEASED,
                "Payout released",
                "Payout for order " + order.getId() + " has been released to your active balance.",
                sourceEventId(order, WALLET_PAYOUT_RELEASED)
        );
    }

    @Transactional
    public NotificationResponse notifyUserDisabled(String userId, String email, String dedupeKey) {
        String message = email == null || email.isBlank()
                ? "Your account has been disabled by an administrator."
                : "Your account (" + email + ") has been disabled by an administrator.";
        return createAndPublish(
                userId,
                USER_DISABLED,
                "Account disabled",
                message,
                dedupeKey.isBlank() ? userId + ":" + USER_DISABLED : dedupeKey
        );
    }

    @Transactional
    public NotificationResponse notifyAuctionEnded(
            String userId,
            String auctionId,
            boolean sold,
            String eventId
    ) {
        String message = sold
                ? "Auction " + auctionId + " ended with a winner."
                : "Auction " + auctionId + " ended without a winner.";
        return createAndPublish(
                userId,
                AUCTION_ENDED,
                "Auction ended",
                message,
                sourceEventId(eventId, AUCTION_ENDED)
        );
    }

    private NotificationResponse createAndPublish(
            String userId,
            String type,
            String title,
            String message,
            String sourceEventId
    ) {
        BidmartNotification notification = notificationRepository
                .findByUserIdAndTypeAndSourceEventId(userId, type, sourceEventId)
                .orElseGet(() -> notificationRepository.save(BidmartNotification.create(
                        userId,
                        type,
                        title,
                        message,
                        sourceEventId
                )));
        NotificationResponse response = NotificationResponse.from(notification);
        var preferences = notificationPreferenceService.findOrCreate(userId);
        if (preferences.isInAppEnabled()) {
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", response);
        }
        if (preferences.isEmailEnabled()) {
            externalNotificationDispatcher.sendEmail(userId, title, message);
        }
        if (preferences.isPushEnabled()) {
            externalNotificationDispatcher.sendPush(userId, title, message);
        }
        return response;
    }

    private String sourceEventId(BidmartOrder order, String type) {
        String source = order.getSourceEventId() == null ? order.getId() : order.getSourceEventId();
        return source + ":" + type;
    }

    private String sourceEventId(String eventId, String type) {
        return eventId + ":" + type;
    }

    static String formatIdrFromCents(long amountCents) {
        long major = amountCents / 100;
        long minor = Math.abs(amountCents % 100);
        return String.format("IDR %d.%02d", major, minor);
    }
}
