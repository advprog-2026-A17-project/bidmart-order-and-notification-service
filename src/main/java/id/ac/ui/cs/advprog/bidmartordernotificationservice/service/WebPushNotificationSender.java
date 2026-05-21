package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import nl.martijndwars.webpush.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "bidmart.notification.push.vapid.private-key")
public class WebPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushNotificationSender.class);

    private final PushSubscriptionService pushSubscriptionService;
    private final ObjectMapper objectMapper;
    private final PushService pushService;

    public WebPushNotificationSender(
            PushSubscriptionService pushSubscriptionService,
            ObjectMapper objectMapper,
            @Value("${bidmart.notification.push.vapid.public-key}") String publicKey,
            @Value("${bidmart.notification.push.vapid.private-key}") String privateKey,
            @Value("${bidmart.notification.push.vapid.subject:mailto:no-reply@bidmart.local}") String subject
    ) throws GeneralSecurityException {
        this.pushSubscriptionService = pushSubscriptionService;
        this.objectMapper = objectMapper;
        this.pushService = new PushService(publicKey, privateKey);
        this.pushService.setSubject(subject);
    }

    @Async
    @Override
    public void sendPush(String userId, String title, String message) {
        List<PushSubscription> subscriptions = pushSubscriptionService.listForUser(userId);
        if (subscriptions.isEmpty()) {
            log.debug("No push subscriptions for user {}", userId);
            return;
        }

        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                    "title", title,
                    "body", message
            ));
        } catch (Exception e) {
            log.error("Failed to encode push payload for user {}: {}", userId, e.getMessage());
            return;
        }

        for (PushSubscription stored : subscriptions) {
            try {
                Subscription subscription = new Subscription(
                        stored.getEndpoint(),
                        new Subscription.Keys(stored.getP256dhKey(), stored.getAuthKey())
                );
                pushService.send(new Notification(subscription, payload));
                log.info("Web push sent to user {} endpoint {}", userId, stored.getEndpoint());
            } catch (Exception e) {
                log.warn("Web push failed for user {}: {}", userId, e.getMessage());
            }
        }
    }
}
