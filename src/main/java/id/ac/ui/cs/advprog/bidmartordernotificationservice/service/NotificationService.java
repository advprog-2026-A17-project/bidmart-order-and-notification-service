package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
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

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(
            NotificationRepository notificationRepository,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listForUser(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationResponse::from)
                .toList();
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
            BigDecimal amountCents,
            String eventId
    ) {
        return createAndPublish(
                userId,
                BID_PLACED,
                "Bid placed",
                "Your bid of " + formatCents(amountCents) + " was placed on auction " + auctionId + ".",
                sourceEventId(eventId, BID_PLACED)
        );
    }

    @Transactional
    public NotificationResponse notifyOutbid(
            String userId,
            String auctionId,
            BigDecimal amountCents,
            String eventId
    ) {
        return createAndPublish(
                userId,
                OUTBID,
                "Outbid",
                "A higher bid of " + formatCents(amountCents) + " was placed on auction " + auctionId + ".",
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
        messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", response);
        return response;
    }

    private String sourceEventId(BidmartOrder order, String type) {
        String source = order.getSourceEventId() == null ? order.getId() : order.getSourceEventId();
        return source + ":" + type;
    }

    private String sourceEventId(String eventId, String type) {
        return eventId + ":" + type;
    }

    private String formatCents(BigDecimal amountCents) {
        return "$" + amountCents.movePointLeft(2).toPlainString();
    }
}
