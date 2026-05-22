package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class RestAuthClient implements AuthClient {

    private static final Logger log = LoggerFactory.getLogger(RestAuthClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> PROFILE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalServiceToken;

    public RestAuthClient(
            RestTemplate restTemplate,
            @Value("${bidmart.auth.base-url:http://localhost:8080}") String baseUrl,
            @Value("${bidmart.auth.internal-service-token}") String internalServiceToken
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalServiceToken = internalServiceToken;
    }

    /**
     * Fetch the shipping address stored in a buyer's auth profile.
     * Returns null if the user is not found or the service is unavailable.
     */
    public Optional<String> fetchUserEmail(String userId) {
        try {
            String url = baseUrl + "/api/v1/auth/internal/users/" + userId + "/profile";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service-Token", internalServiceToken);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PROFILE_TYPE
            );
            Map<String, Object> profile = response.getBody();
            if (profile == null) {
                return Optional.empty();
            }
            Object email = profile.get("email");
            return email == null || email.toString().isBlank()
                    ? Optional.empty()
                    : Optional.of(email.toString());
        } catch (RestClientException e) {
            log.warn("Could not fetch email for user {}: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public String fetchShippingAddress(String userId) {
        try {
            String url = baseUrl + "/api/v1/auth/internal/users/" + userId + "/profile";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Internal-Service-Token", internalServiceToken);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    PROFILE_TYPE
            );
            Map<String, Object> profile = response.getBody();
            if (profile == null) {
                return null;
            }
            Object shippingAddress = profile.get("shippingAddress");
            return shippingAddress == null ? null : shippingAddress.toString();
        } catch (RestClientException e) {
            log.warn("Could not fetch shipping address for user {}: {}", userId, e.getMessage());
            return null;
        }
    }
}
