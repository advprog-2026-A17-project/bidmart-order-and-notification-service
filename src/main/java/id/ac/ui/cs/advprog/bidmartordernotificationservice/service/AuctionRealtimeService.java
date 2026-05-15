package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuctionRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public AuctionRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishAuctionEvent(String eventType, JsonNode payload) {
        String auctionId = payload.path("auctionId").asText("");
        String listingId = payload.path("listingId").asText("");
        String sellerId = payload.path("sellerId").asText("");

        Map<String, Object> message = Map.of(
                "type", eventType,
                "auctionId", auctionId,
                "listingId", listingId,
                "sellerId", sellerId,
                "payload", payload
        );

        messagingTemplate.convertAndSend("/topic/auctions", message);
        if (!auctionId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, message);
        }
        if (!listingId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/listings/" + listingId, message);
        }
        if (!sellerId.isBlank()) {
            messagingTemplate.convertAndSend("/topic/sellers/" + sellerId + "/auctions", message);
        }
    }
}
