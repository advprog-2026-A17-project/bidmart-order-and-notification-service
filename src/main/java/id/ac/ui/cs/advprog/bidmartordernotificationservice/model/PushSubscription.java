package id.ac.ui.cs.advprog.bidmartordernotificationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "push_subscriptions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "endpoint"})
)
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 2048)
    private String endpoint;

    @Column(name = "p256dh_key", nullable = false, length = 512)
    private String p256dhKey;

    @Column(name = "auth_key", nullable = false, length = 512)
    private String authKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public static PushSubscription create(String userId, String endpoint, String p256dhKey, String authKey) {
        PushSubscription subscription = new PushSubscription();
        subscription.userId = userId;
        subscription.endpoint = endpoint;
        subscription.p256dhKey = p256dhKey;
        subscription.authKey = authKey;
        subscription.createdAt = Instant.now();
        return subscription;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public String getP256dhKey() {
        return p256dhKey;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void updateKeys(String p256dhKey, String authKey) {
        this.p256dhKey = p256dhKey;
        this.authKey = authKey;
    }
}
