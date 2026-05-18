package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class AuthClient {

    private static final Logger log = LoggerFactory.getLogger(AuthClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AuthClient(
            RestTemplate restTemplate,
            @Value("${bidmart.auth.base-url:http://localhost:8081}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Fetch the shipping address stored in a buyer's auth profile.
     * Returns null if the user is not found or the service is unavailable.
     */
    public String fetchShippingAddress(String userId) {
        try {
            String url = baseUrl + "/api/v1/auth/internal/users/" + userId + "/profile";
            @SuppressWarnings("unchecked")
            Map<String, Object> profile = restTemplate.getForObject(url, Map.class);
            if (profile == null) {
                return null;
            }
            Object address = profile.get("shippingAddress");
            return address != null ? address.toString() : null;
        } catch (RestClientException e) {
            log.warn("Could not fetch shipping address for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
