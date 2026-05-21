package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.client.AuthClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExternalNotificationDispatcher {

    private static final Logger log = LoggerFactory.getLogger(ExternalNotificationDispatcher.class);

    private final AuthClient authClient;
    private final NotificationEmailSender notificationEmailSender;
    private final PushNotificationSender pushNotificationSender;

    public ExternalNotificationDispatcher(
            AuthClient authClient,
            NotificationEmailSender notificationEmailSender,
            PushNotificationSender pushNotificationSender
    ) {
        this.authClient = authClient;
        this.notificationEmailSender = notificationEmailSender;
        this.pushNotificationSender = pushNotificationSender;
    }

    public void sendEmail(String userId, String title, String message) {
        authClient.fetchUserEmail(userId).ifPresentOrElse(
                email -> notificationEmailSender.sendNotificationEmail(email, title, message),
                () -> log.warn("Skipping email notification for user {}: email not found", userId)
        );
    }

    public void sendPush(String userId, String title, String message) {
        pushNotificationSender.sendPush(userId, title, message);
    }
}
