package id.ac.ui.cs.advprog.bidmartordernotificationservice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void createsAndReadsOrder() throws Exception {
        String location = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-1")
                        .content("""
                                {
                                  "auctionId": "auction-1",
                                  "listingId": "listing-1",
                                  "sellerId": "seller-1",
                                  "buyerId": "buyer-1",
                                  "finalPrice": 125000,
                                  "shippingAddress": "Depok, Jawa Barat"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", notNullValue()))
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.auctionId").value("auction-1"))
                .andExpect(jsonPath("$.sellerId").value("seller-1"))
                .andExpect(jsonPath("$.buyerId").value("buyer-1"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get(location)
                        .header("X-User-Id", "buyer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value("auction-1"))
                .andExpect(jsonPath("$.shippingStatus").value("PENDING"))
                .andExpect(jsonPath("$.shippingAddress").value("Depok, Jawa Barat"));
    }

    @Test
    void sellerUpdatesShippingAndBuyerConfirmsReceipt() throws Exception {
        String location = createOrder("auction-2", "seller-2", "buyer-2");

        mockMvc.perform(put(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-2")
                        .content("""
                                {
                                  "status": "PACKED",
                                  "carrier": "JNE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"))
                .andExpect(jsonPath("$.shippingStatus").value("PACKED"));

        mockMvc.perform(put(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-2")
                        .content("""
                                {
                                  "status": "SHIPPED",
                                  "trackingNumber": "JNE-12345",
                                  "carrier": "JNE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"))
                .andExpect(jsonPath("$.shippingStatus").value("SHIPPED"))
                .andExpect(jsonPath("$.trackingNumber").value("JNE-12345"))
                .andExpect(jsonPath("$.carrier").value("JNE"));

        mockMvc.perform(post(location + "/confirm")
                        .header("X-User-Id", "buyer-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.shippingStatus").value("CONFIRMED"));
    }

    @Test
    void listsOrdersForCurrentBuyerOrSellerOnly() throws Exception {
        createOrder("auction-list-1", "seller-list-1", "buyer-list-1");
        createOrder("auction-list-2", "seller-list-2", "buyer-list-1");
        createOrder("auction-list-3", "seller-list-3", "buyer-list-3");

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", "buyer-list-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].buyerId").value("buyer-list-1"))
                .andExpect(jsonPath("$[1].buyerId").value("buyer-list-1"));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", "seller-list-3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sellerId").value("seller-list-3"));
    }

    @Test
    void createsOrderAutomaticallyFromAuctionWonEvent() throws Exception {
        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "bidmart-local-internal-token")
                        .content("""
                                {
                                  "eventId": "event-1",
                                  "auctionId": "auction-3",
                                  "listingId": "listing-3",
                                  "sellerId": "seller-3",
                                  "buyerId": "buyer-3",
                                  "finalPrice": 200000,
                                  "shippingAddress": "Jakarta Selatan"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.auctionId").value("auction-3"))
                .andExpect(jsonPath("$.status").value("CREATED"));

        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "bidmart-local-internal-token")
                        .content("""
                                {
                                  "eventId": "event-1",
                                  "auctionId": "auction-3",
                                  "listingId": "listing-3",
                                  "sellerId": "seller-3",
                                  "buyerId": "buyer-3",
                                  "finalPrice": 200000,
                                  "shippingAddress": "Jakarta Selatan"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.auctionId").value("auction-3"))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void auctionWonEventCreatesRestAndRealtimeNotification() throws Exception {
        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "bidmart-local-internal-token")
                        .content("""
                                {
                                  "eventId": "event-notification-1",
                                  "auctionId": "auction-notification-1",
                                  "listingId": "listing-notification-1",
                                  "sellerId": "seller-notification-1",
                                  "buyerId": "buyer-notification-1",
                                  "finalPrice": 200000,
                                  "shippingAddress": "Jakarta Selatan"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/notifications")
                        .header("X-User-Id", "buyer-notification-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("ORDER_CREATED"))
                .andExpect(jsonPath("$[0].title").value("Order created"))
                .andExpect(jsonPath("$[0].message").value("Your winning auction has been converted into an order."));

        verify(messagingTemplate, atLeastOnce())
                .convertAndSendToUser(eq("buyer-notification-1"), eq("/queue/notifications"), any());
    }

    @Test
    void notificationSupportsDetailAndReadUnreadStatus() throws Exception {
        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "bidmart-local-internal-token")
                        .content("""
                                {
                                  "eventId": "event-notification-2",
                                  "auctionId": "auction-notification-2",
                                  "listingId": "listing-notification-2",
                                  "sellerId": "seller-notification-2",
                                  "buyerId": "buyer-notification-2",
                                  "finalPrice": 300000,
                                  "shippingAddress": "Bandung"
                                }
                                """))
                .andExpect(status().isCreated());

        String listPayload = mockMvc.perform(get("/api/v1/notifications")
                        .header("X-User-Id", "buyer-notification-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", notNullValue()))
                .andExpect(jsonPath("$[0].status").value("UNREAD"))
                .andExpect(jsonPath("$[0].read").value(false))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode listNode = objectMapper.readTree(listPayload);
        String notificationId = listNode.get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/notifications/" + notificationId)
                        .header("X-User-Id", "buyer-notification-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("UNREAD"))
                .andExpect(jsonPath("$.sourceEventId", notNullValue()));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "buyer-notification-2")
                        .content("""
                                {
                                  "read": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("READ"))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt", notNullValue()));

        mockMvc.perform(patch("/api/v1/notifications/" + notificationId + "/read-status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "buyer-notification-2")
                        .content("""
                                {
                                  "read": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notificationId))
                .andExpect(jsonPath("$.status").value("UNREAD"))
                .andExpect(jsonPath("$.read").value(false));
    }

    @Test
    void rejectsInvalidInternalTokenForAuctionWonEvent() throws Exception {
        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "wrong-token")
                        .content("""
                                {
                                  "eventId": "event-invalid-token",
                                  "auctionId": "auction-invalid",
                                  "listingId": "listing-invalid",
                                  "sellerId": "seller-invalid",
                                  "buyerId": "buyer-invalid",
                                  "finalPrice": 100000,
                                  "shippingAddress": "Depok"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void buyerCanOpenDisputeOnShippedOrder() throws Exception {
        String location = createOrder("auction-dispute-api", "seller-dispute", "buyer-dispute");

        mockMvc.perform(put(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-dispute")
                        .content("""
                                {"status":"PACKED","carrier":"JNE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-dispute")
                        .content("""
                                {"status":"SHIPPED","trackingNumber":"DISPUTE-TRK","carrier":"JNE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(location + "/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "buyer-dispute")
                        .content("""
                                {"reason":"Never received","details":"No delivery after 14 days"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPUTED"))
                .andExpect(jsonPath("$.disputeReason").value("Never received"));
    }

    @Test
    void adminListsAllDisputesAcrossUsers() throws Exception {
        String disputedLocation = createOrder("auction-admin-dispute-api", "seller-admin-dispute", "buyer-admin-dispute");
        createOrder("auction-admin-normal-api", "seller-admin-normal", "buyer-admin-normal");

        mockMvc.perform(put(disputedLocation + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-admin-dispute")
                        .content("""
                                {"status":"PACKED","carrier":"JNE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(put(disputedLocation + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "seller-admin-dispute")
                        .content("""
                                {"status":"SHIPPED","trackingNumber":"ADMIN-DISPUTE-TRK","carrier":"JNE"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post(disputedLocation + "/dispute")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "buyer-admin-dispute")
                        .content("""
                                {"reason":"Item mismatch","details":"The received item does not match the listing"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/orders/admin/disputes")
                        .header("X-User-Id", "admin-1")
                        .header("X-User-Roles", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").value(hasItem("DISPUTED")))
                .andExpect(jsonPath("$[*].buyerId").value(hasItem("buyer-admin-dispute")))
                .andExpect(jsonPath("$[*].sellerId").value(hasItem("seller-admin-dispute")));

        mockMvc.perform(get("/api/v1/orders/admin/disputes")
                        .header("X-User-Id", "buyer-admin-dispute")
                        .header("X-User-Roles", "BUYER"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsWrongActorForSellerAndBuyerActions() throws Exception {
        String location = createOrder("auction-4", "seller-4", "buyer-4");

        mockMvc.perform(put(location + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", "buyer-4")
                        .content("""
                                {
                                  "status": "SHIPPED",
                                  "trackingNumber": "JNE-0001",
                                  "carrier": "JNE"
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(location + "/confirm")
                        .header("X-User-Id", "seller-4"))
                .andExpect(status().isForbidden());
    }

    private String createOrder(String auctionId, String sellerId, String buyerId) throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-User-Id", sellerId)
                        .content("""
                                {
                                  "auctionId": "%s",
                                  "listingId": "listing-%s",
                                  "sellerId": "%s",
                                  "buyerId": "%s",
                                  "finalPrice": 100000,
                                  "shippingAddress": "Depok, Jawa Barat"
                                }
                                """.formatted(auctionId, auctionId, sellerId, buyerId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getHeader("Location");
    }
}
