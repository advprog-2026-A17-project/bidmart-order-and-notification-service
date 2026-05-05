package id.ac.ui.cs.advprog.bidmartordernotificationservice;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void createsOrderAutomaticallyFromAuctionWonEvent() throws Exception {
        mockMvc.perform(post("/api/v1/orders/events/auction-won")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Internal-Service-Token", "local-dev-internal-token")
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
                        .header("X-Internal-Service-Token", "local-dev-internal-token")
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
                        .header("X-Internal-Service-Token", "local-dev-internal-token")
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
