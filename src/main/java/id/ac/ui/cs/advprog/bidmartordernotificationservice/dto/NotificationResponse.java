package id.ac.ui.cs.advprog.bidmartordernotificationservice.dto;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;

import java.time.Instant;

public record NotificationResponse(
        String id,
        String userId,
        String type,
        String title,
        String message,
        String status,
        boolean read,
        String sourceEventId,
        Instant readAt,
        Instant createdAt
) {
    public static NotificationResponse from(BidmartNotification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.isRead() ? "READ" : "UNREAD",
                notification.isRead(),
                notification.getSourceEventId(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
