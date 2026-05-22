package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WalletClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private WalletClient walletClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        walletClient = new WalletClient(restTemplate, "http://wallet.test", "wallet-internal-token");
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void payoutSellerPostsAmountAndOrderReference() {
        server.expect(requestTo("http://wallet.test/api/v1/wallet/payout"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-internal-service-token", "wallet-internal-token"))
                .andExpect(content().json("""
                        {"sellerId":"seller-1","amount":125,"orderId":"order-1"}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        walletClient.payoutSeller("seller-1", 125L, "order-1");
    }

    @Test
    void creditSellerEscrowPostsAmountAndAuctionCorrelation() {
        server.expect(requestTo("http://wallet.test/api/v1/wallet/seller-escrow"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-internal-service-token", "wallet-internal-token"))
                .andExpect(content().json("""
                        {"sellerId":"seller-1","amount":125,"correlationId":"auction-1"}
                        """))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        walletClient.creditSellerEscrow("seller-1", 125L, "auction-1");
    }
}
