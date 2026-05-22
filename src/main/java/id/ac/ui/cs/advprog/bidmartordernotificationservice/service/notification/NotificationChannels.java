package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.service.ExternalNotificationDispatcher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

public final class NotificationChannels {

    private NotificationChannels() {
    }

    public static final class InAppNotificationChannel implements NotificationChannel {
        private final SimpMessagingTemplate messagingTemplate;

        public InAppNotificationChannel(SimpMessagingTemplate messagingTemplate) {
            this.messagingTemplate = messagingTemplate;
        }

        @Override
        public void publish(String userId, NotificationEnvelope envelope, NotificationResponse response, NotificationPreference preferences) {
            if (!preferences.isInAppEnabled()) {
                return;
            }
            messagingTemplate.convertAndSendToUser(userId, "/queue/notifications", response);
            messagingTemplate.convertAndSend("/topic/notifications/users/" + userId, response);
        }
    }

    public static final class EmailNotificationChannel implements NotificationChannel {
        private final ExternalNotificationDispatcher dispatcher;

        public EmailNotificationChannel(ExternalNotificationDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void publish(String userId, NotificationEnvelope envelope, NotificationResponse response, NotificationPreference preferences) {
            if (preferences.isEmailEnabled()) {
                dispatcher.sendEmail(userId, envelope.title(), envelope.message());
            }
        }
    }

    public static final class PushNotificationChannel implements NotificationChannel {
        private final ExternalNotificationDispatcher dispatcher;

        public PushNotificationChannel(ExternalNotificationDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public void publish(String userId, NotificationEnvelope envelope, NotificationResponse response, NotificationPreference preferences) {
            if (preferences.isPushEnabled()) {
                dispatcher.sendPush(userId, envelope.title(), envelope.message());
            }
        }
    }
}
