package id.ac.ui.cs.advprog.bidmartordernotificationservice.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.AuthClient;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.metrics.BidmartOrderMetrics;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionOrderEventConsumerTest {

    @Mock
    private OrderService orderService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AuctionRealtimeService auctionRealtimeService;

    @Mock
    private AuthClient authClient;

    @Mock
    private BidmartOrderMetrics orderMetrics;

    private AuctionOrderEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new AuctionOrderEventConsumer(
                new ObjectMapper(),
                orderService,
                notificationService,
                auctionRealtimeService,
                authClient,
                orderMetrics
        );
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

        verify(notificationService).notifyBidPlaced("buyer-1", "auction-1", new BigDecimal("125.00"), "evt-bid-1");
        verify(auctionRealtimeService).publishAuctionEvent(eq("auction.bid-placed.v1"), argThat(payload -> payload.path("auctionId").asText().equals("auction-1")));
        verify(orderMetrics).recordRabbitConsumed();
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

        verify(notificationService).notifyOutbid("buyer-1", "auction-1", new BigDecimal("140.00"), "evt-outbid-1");
    }

    @Test
    void auctionEndedWithWinnerCreatesOrderAndNotifications() throws Exception {
        when(authClient.fetchShippingAddress("buyer-1")).thenReturn("Jl. Melati No. 10, Jakarta");
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
                    "status": "WON",
                    "winnerId": "buyer-1",
                    "finalPrice": 15000
                  }
                }
                """);

        verify(orderService).createOrderFromAuctionWon(argThat(matchesAuctionWonRequest()));
        verify(notificationService).notifyAuctionWon("buyer-1", "auction-1", "evt-ended-1");
        verify(notificationService).notifyAuctionEnded("seller-1", "auction-1", true, "evt-ended-1");
        verify(orderMetrics).recordRabbitConsumed();
        verify(orderMetrics).recordNotificationSent();
        verify(orderMetrics).recordOrderCreated();
    }

    @Test
    void duplicateEventIdIsIgnored() throws Exception {
        String message = """
                {
                  "eventId": "evt-dup-1",
                  "eventType": "auction.bid-placed.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-dup",
                  "payload": {
                    "auctionId": "auction-dup",
                    "bidderId": "buyer-dup",
                    "amountCents": 10000
                  }
                }
                """;

        consumer.consume(message);
        consumer.consume(message);

        verify(notificationService).notifyBidPlaced(
                eq("buyer-dup"),
                eq("auction-dup"),
                eq(new BigDecimal("100.00")),
                eq("evt-dup-1")
        );
        verify(notificationService, org.mockito.Mockito.times(1))
                .notifyBidPlaced(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void bidPlacedWithBlankBidderSkipsNotification() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-blank-bidder",
                  "eventType": "auction.bid-placed.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-blank",
                  "payload": {
                    "auctionId": "auction-blank",
                    "bidderId": "",
                    "amountCents": 10000
                  }
                }
                """);

        verify(notificationService, never()).notifyBidPlaced(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        verify(auctionRealtimeService).publishAuctionEvent(eq("auction.bid-placed.v1"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bidPlacedUsesCurrentPriceWhenAmountCentsMissing() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-current-price",
                  "eventType": "auction.bid-placed.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-price",
                  "payload": {
                    "auctionId": "auction-price",
                    "bidderId": "buyer-price",
                    "currentPrice": 25000
                  }
                }
                """);

        verify(notificationService).notifyBidPlaced("buyer-price", "auction-price", new BigDecimal("250.00"), "evt-current-price");
    }

    @Test
    void auctionEndedLegacyWinnerWithoutStatusCreatesOrder() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-legacy-won",
                  "eventType": "auction.ended.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-legacy",
                  "payload": {
                    "auctionId": "auction-legacy",
                    "listingId": "listing-legacy",
                    "sellerId": "seller-legacy",
                    "winnerId": "buyer-legacy",
                    "finalPrice": 8800,
                    "shippingAddress": "Jl. Legacy 1"
                  }
                }
                """);

        verify(orderService).createOrderFromAuctionWon(argThat(request ->
                request.eventId().equals("evt-legacy-won")
                        && request.buyerId().equals("buyer-legacy")
                        && request.finalPrice().compareTo(new BigDecimal("88.00")) == 0
                        && request.shippingAddress().equals("Jl. Legacy 1")
        ));
        verify(notificationService).notifyAuctionWon("buyer-legacy", "auction-legacy", "evt-legacy-won");
    }

    @Test
    void auctionEndedUsesDefaultShippingWhenAuthReturnsBlank() throws Exception {
        when(authClient.fetchShippingAddress("buyer-default")).thenReturn("   ");
        consumer.consume("""
                {
                  "eventId": "evt-default-ship",
                  "eventType": "auction.ended.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-default",
                  "payload": {
                    "auctionId": "auction-default",
                    "listingId": "listing-default",
                    "sellerId": "seller-default",
                    "status": "WON",
                    "winnerId": "buyer-default",
                    "finalPrice": 5000
                  }
                }
                """);

        verify(orderService).createOrderFromAuctionWon(argThat(request ->
                request.shippingAddress().equals("Pending buyer shipping address")
        ));
    }

    @Test
    void normalizeEventTypeAddsV1SuffixWhenMissing() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-legacy-type",
                  "eventType": "auction.bid-placed",
                  "eventVersion": 1,
                  "aggregateId": "auction-type",
                  "payload": {
                    "auctionId": "auction-type",
                    "bidderId": "buyer-type",
                    "amountCents": 3000
                  }
                }
                """);

        verify(notificationService).notifyBidPlaced("buyer-type", "auction-type", new BigDecimal("30.00"), "evt-legacy-type");
    }

    @Test
    void outbidWithBlankPreviousBidderSkipsNotification() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-outbid-blank",
                  "eventType": "auction.outbid.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-blank-outbid",
                  "payload": {
                    "auctionId": "auction-blank-outbid",
                    "previousBidderId": "",
                    "amountCents": 5000
                  }
                }
                """);

        verify(notificationService, never()).notifyOutbid(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()
        );
        verify(auctionRealtimeService).publishAuctionEvent(eq("auction.outbid.v1"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void bidPlacedParsesAmountCentsFromTextNode() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-text-cents",
                  "eventType": "auction.bid-placed.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-text",
                  "payload": {
                    "auctionId": "auction-text",
                    "bidderId": "buyer-text",
                    "amountCents": "7500"
                  }
                }
                """);

        verify(notificationService).notifyBidPlaced("buyer-text", "auction-text", new BigDecimal("75.00"), "evt-text-cents");
    }

    @Test
    void auctionEndedUsesShippingAddressFromPayloadWhenProvided() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-ship-payload",
                  "eventType": "auction.ended.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-ship",
                  "payload": {
                    "auctionId": "auction-ship",
                    "listingId": "listing-ship",
                    "sellerId": "seller-ship",
                    "status": "WON",
                    "winnerId": "buyer-ship",
                    "finalPrice": 12000,
                    "shippingAddress": "Jl. Payload No. 7"
                  }
                }
                """);

        verify(orderService).createOrderFromAuctionWon(argThat(request ->
                request.shippingAddress().equals("Jl. Payload No. 7")
                        && request.finalPrice().compareTo(new BigDecimal("120.00")) == 0
        ));
        verify(authClient, never()).fetchShippingAddress(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void auctionEndedBelowReserveDoesNotCreateOrderEvenWithLegacyWinnerId() throws Exception {
        consumer.consume("""
                {
                  "eventId": "evt-ended-unsold",
                  "eventType": "auction.ended.v1",
                  "eventVersion": 1,
                  "aggregateId": "auction-2",
                  "payload": {
                    "auctionId": "auction-2",
                    "listingId": "listing-2",
                    "sellerId": "seller-1",
                    "status": "UNSOLD",
                    "winnerId": "buyer-1",
                    "finalPrice": 4000
                  }
                }
                """);

        verify(notificationService).notifyAuctionEnded("seller-1", "auction-2", false, "evt-ended-unsold");
        verify(orderService, never()).createOrderFromAuctionWon(org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).notifyAuctionWon(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private ArgumentMatcher<AuctionWonEventRequest> matchesAuctionWonRequest() {
        return request -> request.eventId().equals("evt-ended-1")
                && request.auctionId().equals("auction-1")
                && request.listingId().equals("listing-1")
                && request.sellerId().equals("seller-1")
                && request.buyerId().equals("buyer-1")
                && request.finalPrice().compareTo(new BigDecimal("150.00")) == 0
                && request.shippingAddress().equals("Jl. Melati No. 10, Jakarta");
    }
}
