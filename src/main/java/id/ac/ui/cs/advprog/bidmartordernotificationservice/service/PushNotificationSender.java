package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

public interface PushNotificationSender {

    void sendPush(String userId, String title, String message);
}
