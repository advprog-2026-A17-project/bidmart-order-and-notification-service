package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

public record RegisterPushSubscriptionRequest(
        String endpoint,
        PushSubscriptionKeys keys
) {
    public record PushSubscriptionKeys(
            String p256dh,
            String auth
    ) {
    }
}
