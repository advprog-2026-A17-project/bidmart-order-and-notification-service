package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AuthClientTest {

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private AuthClient authClient;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        authClient = new AuthClient(restTemplate, "http://auth.test", "test-internal-token");
    }

    @AfterEach
    void verifyServer() {
        server.verify();
    }

    @Test
    void fetchShippingAddressReturnsProfileValue() {
        server.expect(requestTo("http://auth.test/api/v1/auth/internal/users/buyer-1/profile"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Internal-Service-Token", "test-internal-token"))
                .andRespond(withSuccess("""
                        {"shippingAddress":"Jl. Melati No. 10"}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Jl. Melati No. 10", authClient.fetchShippingAddress("buyer-1"));
    }

    @Test
    void fetchShippingAddressReturnsNullWhenProfileMissingAddress() {
        server.expect(requestTo("http://auth.test/api/v1/auth/internal/users/buyer-2/profile"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertNull(authClient.fetchShippingAddress("buyer-2"));
    }

    @Test
    void fetchShippingAddressReturnsNullWhenAuthServiceFails() {
        server.expect(requestTo("http://auth.test/api/v1/auth/internal/users/buyer-3/profile"))
                .andRespond(withServerError());

        assertNull(authClient.fetchShippingAddress("buyer-3"));
    }
}
