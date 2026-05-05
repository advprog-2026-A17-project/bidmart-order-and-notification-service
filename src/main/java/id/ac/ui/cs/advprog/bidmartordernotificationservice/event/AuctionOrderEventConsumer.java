package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
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
    private static final String AUCTION_ENDED_V1 = "auction.ended.v1";
    private static final String DEFAULT_SHIPPING_ADDRESS = "Pending buyer shipping address";

    private final ObjectMapper objectMapper;
    private final OrderService orderService;
    private final NotificationService notificationService;
    private final Set<String> processedEventIds = ConcurrentHashMap.newKeySet();

    public AuctionOrderEventConsumer(
            ObjectMapper objectMapper,
            OrderService orderService,
            NotificationService notificationService
    ) {
        this.objectMapper = objectMapper;
        this.orderService = orderService;
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "${bidmart.rabbitmq.order.auction-events-queue:order-notification.auction-events}")
    public void consume(String message) throws JsonProcessingException {
        JsonNode envelope = objectMapper.readTree(message);
        String eventId = envelope.path("eventId").asText("");
        if (!eventId.isBlank() && !processedEventIds.add(eventId)) {
            return;
        }

        String eventType = normalizeEventType(envelope);
        JsonNode payload = envelope.path("payload");
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
        if (bidderId.isBlank() || auctionId.isBlank()) {
            return;
        }
        notificationService.notifyBidPlaced(bidderId, auctionId, amount(payload), eventId);
    }

    private void handleOutbid(String eventId, JsonNode payload) {
        String previousBidderId = payload.path("previousBidderId").asText("");
        String auctionId = payload.path("auctionId").asText("");
        if (previousBidderId.isBlank() || auctionId.isBlank()) {
            return;
        }
        notificationService.notifyOutbid(previousBidderId, auctionId, amount(payload), eventId);
    }

    private void handleAuctionEnded(String eventId, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        String sellerId = payload.path("sellerId").asText("");
        String winnerId = payload.path("winnerId").asText("");
        boolean sold = !winnerId.isBlank();
        if (!sellerId.isBlank()) {
            notificationService.notifyAuctionEnded(sellerId, auctionId, sold, eventId);
        }
        if (!sold) {
            return;
        }

        notificationService.notifyAuctionWon(winnerId, auctionId, eventId);
        orderService.createOrderFromAuctionWon(new AuctionWonEventRequest(
                eventId,
                auctionId,
                payload.path("listingId").asText(""),
                sellerId,
                winnerId,
                decimal(payload.path("finalPrice")),
                payload.path("shippingAddress").asText(DEFAULT_SHIPPING_ADDRESS)
        ));
    }

    private String normalizeEventType(JsonNode envelope) {
        String eventType = envelope.path("eventType").asText("");
        int eventVersion = envelope.path("eventVersion").asInt(1);
        if (eventVersion == 1 && !eventType.endsWith(".v1")) {
            return eventType + ".v1";
        }
        return eventType;
    }

    private BigDecimal amount(JsonNode payload) {
        JsonNode amount = payload.hasNonNull("amountCents")
                ? payload.path("amountCents")
                : payload.path("currentPrice");
        return decimal(amount);
    }

    private BigDecimal decimal(JsonNode node) {
        if (node.isNumber()) {
            return node.decimalValue();
        }
        return new BigDecimal(node.asText("0"));
    }
}
