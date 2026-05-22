package id.ac.ui.cs.advprog.bidmartordernotificationservice.client;

import java.util.Optional;

public interface AuthClient {
    Optional<String> fetchUserEmail(String userId);

    String fetchShippingAddress(String userId);
}
