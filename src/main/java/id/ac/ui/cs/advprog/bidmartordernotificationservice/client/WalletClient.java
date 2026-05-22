package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class WalletClient {

    private static final String WALLET_API_PATH = "/api/v1/wallet";

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
        restTemplate.postForEntity(walletUrl("/payout"), entity, String.class);
    }

    public void creditSellerEscrow(String sellerId, long amount, String auctionId) {
        WalletSellerEscrowRequest request = new WalletSellerEscrowRequest(sellerId, amount, auctionId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("x-internal-service-token", internalToken);

        HttpEntity<WalletSellerEscrowRequest> entity = new HttpEntity<>(request, headers);
        restTemplate.postForEntity(walletUrl("/seller-escrow"), entity, String.class);
    }

    public void refundBuyer(String buyerId, long amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("x-internal-service-token", internalToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        restTemplate.postForEntity(
                walletUrl("/" + buyerId + "/top-up?amount=" + amount + "&role=BUYER"),
                entity,
                String.class
        );
    }

    private String walletUrl(String path) {
        return walletBaseUrl() + path;
    }

    private String walletBaseUrl() {
        String normalizedBaseUrl = baseUrl.replaceAll("/+$", "");
        if (normalizedBaseUrl.endsWith(WALLET_API_PATH)) {
            return normalizedBaseUrl;
        }
        return normalizedBaseUrl + WALLET_API_PATH;
    }
}
