package id.ac.ui.cs.advprog.bidmartordernotificationservice.service;

public interface NotificationEmailSender {

    void sendNotificationEmail(String toEmail, String subject, String message);
}
