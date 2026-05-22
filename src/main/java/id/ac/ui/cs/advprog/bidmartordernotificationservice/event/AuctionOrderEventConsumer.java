package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.AuthClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.AuctionRealtimeService;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.metrics.BidmartOrderMetrics;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.OrderService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AuctionOrderEventConsumer {

    private static final String BID_PLACED_V1 = "auction.bid-placed.v1";
    private static final String OUTBID_V1 = "auction.outbid.v1";
    private static final String AUCTION_CREATED_V1 = "auction.created.v1";
    private static final String AUCTION_ENDED_V1 = "auction.ended.v1";
    private static final String DEFAULT_SHIPPING_ADDRESS = "Pending buyer shipping address";

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final AuctionRealtimeService auctionRealtimeService;
    private final AuthClient authClient;
    private final BidmartOrderMetrics orderMetrics;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public AuctionOrderEventConsumer(
            ObjectMapper objectMapper,
            OrderService orderService,
            NotificationService notificationService,
            AuctionRealtimeService auctionRealtimeService,
            AuthClient authClient,
            BidmartOrderMetrics orderMetrics
    ) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.notificationService = notificationService;
        this.auctionRealtimeService = auctionRealtimeService;
        this.authClient = authClient;
        this.orderMetrics = orderMetrics;
    }

    @RabbitListener(queues = "${bidmart.rabbitmq.order.auction-events-queue:order-notification.auction-events}")
    public void consume(String message) throws JsonProcessingException {
        orderMetrics.recordRabbitConsumed();
        JsonNode envelope = objectMapper.readTree(message);
        String eventId = envelope.path("eventId").asText("");
        if (!eventId.isBlank() && !processedEventIds.add(eventId)) {
            return;
        }

        String eventType = normalizeEventType(envelope);
        JsonNode payload = envelope.path("payload");
        if (AUCTION_CREATED_V1.equals(eventType)) {
            auctionRealtimeService.publishAuctionEvent(eventType, payload);
            return;
        }
        if (BID_PLACED_V1.equals(eventType)) {
            handleBidPlaced(eventId, payload);
            return;
        }
        if (OUTBID_V1.equals(eventType)) {
            handleOutbid(eventId, payload);
            return;
        }
        if (AUCTION_ENDED_V1.equals(eventType)) {
            handleAuctionEnded(eventId, payload);
        }
    }

    private void handleBidPlaced(String eventId, JsonNode payload) {
        String bidderId = payload.path("bidderId").asText("");
        String auctionId = payload.path("auctionId").asText("");
        auctionRealtimeService.publishAuctionEvent(BID_PLACED_V1, payload);
        if (bidderId.isBlank() || auctionId.isBlank()) {
            return;
        }
        notificationService.notifyBidPlaced(bidderId, auctionId, amountCents(payload), eventId);
    }

    private void handleOutbid(String eventId, JsonNode payload) {
        String previousBidderId = payload.path("previousBidderId").asText("");
        String auctionId = payload.path("auctionId").asText("");
        auctionRealtimeService.publishAuctionEvent(OUTBID_V1, payload);
        if (previousBidderId.isBlank() || auctionId.isBlank()) {
            return;
        }
        notificationService.notifyOutbid(previousBidderId, auctionId, amountCents(payload), eventId);
    }

    private void handleAuctionEnded(String eventId, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        String sellerId = payload.path("sellerId").asText("");
        String status = payload.path("status").asText("");
        String winnerId = payload.path("winnerId").asText("");
        boolean sold = "WON".equalsIgnoreCase(status)
                || (status.isBlank() && !winnerId.isBlank());
        auctionRealtimeService.publishAuctionEvent(AUCTION_ENDED_V1, payload);

        if (sold) {
            String shippingAddress = resolveShippingAddress(payload, winnerId);
            orderService.createOrderFromAuctionWon(new AuctionWonEventRequest(
                    eventId,
                    auctionId,
                    payload.path("listingId").asText(""),
                    sellerId,
                    winnerId,
                    decimalFromCents(payload.path("finalPrice")),
                    shippingAddress
            ));
            orderMetrics.recordOrderCreated();
        }

        try {
            if (!sellerId.isBlank()) {
                notificationService.notifyAuctionEnded(sellerId, auctionId, sold, eventId);
            }
            if (sold) {
                notificationService.notifyAuctionWon(winnerId, auctionId, eventId);
                orderMetrics.recordNotificationSent();
            }
        } catch (RuntimeException notificationError) {
            // Order creation must not be rolled back when optional email/push channels fail.
        }
    }

    private String resolveShippingAddress(JsonNode payload, String winnerId) {
        // Try to get shipping address from event payload first (for manual order creation)
        String fromPayload = payload.path("shippingAddress").asText("");
        if (!fromPayload.isBlank() && !DEFAULT_SHIPPING_ADDRESS.equals(fromPayload)) {
            return fromPayload;
        }
        // Fetch buyer's shipping address from auth service profile
        if (winnerId != null && !winnerId.isBlank()) {
            String fromProfile = authClient.fetchShippingAddress(winnerId);
            if (fromProfile != null && !fromProfile.isBlank()) {
                return fromProfile;
            }
        }
        return DEFAULT_SHIPPING_ADDRESS;
    }

    private String normalizeEventType(JsonNode envelope) {
        String eventType = envelope.path("eventType").asText("");
        int eventVersion = envelope.path("eventVersion").asInt(1);
        if (eventVersion == 1 && !eventType.endsWith(".v1")) {
            return eventType + ".v1";
        }
        return eventType;
    }

    private long amountCents(JsonNode payload) {
        JsonNode amount = payload.hasNonNull("amountCents")
                ? payload.path("amountCents")
                : payload.path("currentPrice");
        return centsFromNode(amount);
    }

    private long centsFromNode(JsonNode node) {
        if (node.isNumber()) {
            return node.asLong();
        }
        String raw = node.asText("0").trim();
        if (raw.isEmpty()) {
            return 0L;
        }
        return new BigDecimal(raw).longValue();
    }

    private BigDecimal decimalFromCents(JsonNode node) {
        return BigDecimal.valueOf(centsFromNode(node)).movePointLeft(2);
    }
}
