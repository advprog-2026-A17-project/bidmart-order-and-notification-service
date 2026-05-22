package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;

public interface NotificationChannel {

    void publish(String userId, NotificationEnvelope envelope, NotificationResponse response, NotificationPreference preferences);
}
