package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String userId,
        String type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(BidmartNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
