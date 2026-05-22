package id.ac.ui.cs.advprog.bidmartordernotificationservice.service.notification;

public record NotificationEnvelope(String type, String title, String message, String sourceEventId) {
}
