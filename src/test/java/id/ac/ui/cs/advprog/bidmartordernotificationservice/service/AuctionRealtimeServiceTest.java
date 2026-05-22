package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionRealtimeServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private AuctionRealtimeService auctionRealtimeService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        auctionRealtimeService = new AuctionRealtimeService(messagingTemplate);
    }

    @Test
    void publishAuctionEventBroadcastsToAllScopedTopics() throws Exception {
        var payload = objectMapper.readTree("""
                {
                  "auctionId": "auction-1",
                  "listingId": "listing-1",
                  "sellerId": "seller-1",
                  "amountCents": 12500
                }
                """);

        auctionRealtimeService.publishAuctionEvent("auction.bid-placed.v1", payload);

        ArgumentCaptor<String> destinations = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, org.mockito.Mockito.times(4))
                .convertAndSend(destinations.capture(), org.mockito.ArgumentMatchers.any(Object.class));

        assertEquals(
                java.util.List.of(
                        "/topic/auctions",
                        "/topic/auctions/auction-1",
                        "/topic/listings/listing-1",
                        "/topic/sellers/seller-1/auctions"
                ),
                destinations.getAllValues()
        );
    }

    @Test
    void publishAuctionEventWithBlankIdsOnlyUsesGlobalTopic() throws Exception {
        var payload = objectMapper.readTree("""
                {
                  "auctionId": "",
                  "listingId": "",
                  "sellerId": ""
                }
                """);

        auctionRealtimeService.publishAuctionEvent("auction.created.v1", payload);

        ArgumentCaptor<Object> messageCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(org.mockito.ArgumentMatchers.eq("/topic/auctions"), messageCaptor.capture());

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) messageCaptor.getValue();
        assertEquals("auction.created.v1", message.get("type"));
    }
}
