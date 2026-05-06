package id.ac.ui.cs.advprog.bidmartordernotificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_user_created", columnList = "userId,createdAt"),
                @Index(name = "idx_notifications_user_source", columnList = "userId,type,sourceEventId")
        }
)
public class BidmartNotification {

    @Id
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private boolean readFlag;

    @Column
    private String sourceEventId;

    @Column(nullable = false)
    private Instant createdAt;

    public static BidmartNotification create(
            String userId,
            String type,
            String title,
            String message,
            String sourceEventId
    ) {
        BidmartNotification notification = new BidmartNotification();
        notification.id = UUID.randomUUID().toString();
        notification.userId = userId;
        notification.type = type;
        notification.title = title;
        notification.message = message;
        notification.sourceEventId = sourceEventId;
        notification.readFlag = false;
        notification.createdAt = Instant.now();
        return notification;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return readFlag;
    }

    public String getSourceEventId() {
        return sourceEventId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
