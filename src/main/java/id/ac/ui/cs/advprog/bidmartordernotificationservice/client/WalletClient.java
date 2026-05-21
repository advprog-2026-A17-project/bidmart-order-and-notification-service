package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WalletClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final String internalToken;

    public WalletClient(
            RestTemplate restTemplate,
            @Value("${bidmart.wallet.base-url}") String baseUrl,
            @Value("${bidmart.wallet.internal-token}") String internalToken
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
        this.internalToken = internalToken;
    }

    public void payoutSeller(String sellerId, long amount, String orderId) {
        WalletPayoutRequest request = new WalletPayoutRequest(sellerId, amount, orderId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-internal-service-token", internalToken);

        HttpEntity<WalletPayoutRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(baseUrl + "/api/v1/wallet/payout", entity, String.class);
    }

    public void refundBuyer(String buyerId, long amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-internal-service-token", internalToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.postForEntity(
                baseUrl + "/api/v1/wallet/" + buyerId + "/top-up?amount=" + amount + "&role=BUYER",
                entity,
                String.class
        );
    }
}
