package id.ac.ui.cs.advprog.bidmartordernotificationservice.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String notificationId) {
        super("Notification not found: " + notificationId);
    }
}
