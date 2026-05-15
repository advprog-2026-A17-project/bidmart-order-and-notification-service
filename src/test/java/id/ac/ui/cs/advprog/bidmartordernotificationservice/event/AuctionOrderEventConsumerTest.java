package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.AuctionWonEventRequest;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.AuctionRealtimeService;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.NotificationService;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionOrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuctionRealtimeService auctionRealtimeService;

    private AuctionOrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuctionOrderEventConsumer(new ObjectMapper(), orderService, notificationService, auctionRealtimeService);
    }

    @Test
    void bidPlacedEventCreatesBidPlacedNotification() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-bid-1",
                  "eventType": "auction.bid-placed.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-1",
                  "payload": {
                    "auctionId": "auction-1",
                    "bidderId": "buyer-1",
                    "amountCents": 12500
                  }
                }
                """);

        verify(notificationService).notifyBidPlaced("buyer-1", "auction-1", new BigDecimal("12500"), "evt-bid-1");
        verify(auctionRealtimeService).publishAuctionEvent(eq("auction.bid-placed.v1"), argThat(payload -> payload.path("auctionId").asText().equals("auction-1")));
    }

    @Test
    void auctionCreatedEventPublishesRealtimeUpdate() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-created-1",
                  "eventType": "auction.created.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-1",
                  "payload": {
                    "auctionId": "auction-1",
                    "listingId": "listing-1",
                    "sellerId": "seller-1"
                  }
                }
                """);

        verify(auctionRealtimeService).publishAuctionEvent(eq("auction.created.v1"), argThat(payload -> payload.path("auctionId").asText().equals("auction-1")));
    }

    @Test
    void outbidEventCreatesOutbidNotification() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-outbid-1",
                  "eventType": "auction.outbid.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-1",
                  "payload": {
                    "auctionId": "auction-1",
                    "previousBidderId": "buyer-1",
                    "amountCents": 14000
                  }
                }
                """);

        verify(notificationService).notifyOutbid("buyer-1", "auction-1", new BigDecimal("14000"), "evt-outbid-1");
    }

    @Test
    void auctionEndedWithWinnerCreatesOrderAndNotifications() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-ended-1",
                  "eventType": "auction.ended.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-1",
                  "payload": {
                    "auctionId": "auction-1",
                    "listingId": "listing-1",
                    "sellerId": "seller-1",
                    "winnerId": "buyer-1",
                    "finalPrice": 15000,
                    "shippingAddress": "Pending buyer shipping address"
                  }
                }
                """);

        verify(orderService).createOrderFromAuctionWon(argThat(matchesAuctionWonRequest()));
        verify(notificationService).notifyAuctionWon("buyer-1", "auction-1", "evt-ended-1");
        verify(notificationService).notifyAuctionEnded("seller-1", "auction-1", true, "evt-ended-1");
    }

    private ArgumentMatcher<AuctionWonEventRequest> matchesAuctionWonRequest() {
        return request -> request.eventId().equals("evt-ended-1")
                && request.auctionId().equals("auction-1")
                && request.listingId().equals("listing-1")
                && request.sellerId().equals("seller-1")
                && request.buyerId().equals("buyer-1")
                && request.finalPrice().compareTo(new BigDecimal("15000")) == 0;
    }
}
