package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMissingBean(WebPushNotificationSender.class)
public class LogPushNotificationSender implements PushNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogPushNotificationSender.class);

    private final PushSubscriptionService pushSubscriptionService;

    public LogPushNotificationSender(PushSubscriptionService pushSubscriptionService) {
        this.pushSubscriptionService = pushSubscriptionService;
    }

    @Override
    public void sendPush(String userId, String title, String message) {
        int devices = pushSubscriptionService.listForUser(userId).size();
        log.info("PUSH notification user={} devices={} title={} message={}", userId, devices, title, message);
    }
}
