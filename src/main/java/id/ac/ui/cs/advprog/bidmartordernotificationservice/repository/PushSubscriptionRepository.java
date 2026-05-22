package id.ac.ui.cs.advprog.bidmartordernotificationservice.repository;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, UUID> {

    List<PushSubscription> findByUserId(String userId);

    Optional<PushSubscription> findByUserIdAndEndpoint(String userId, String endpoint);

    void deleteByUserId(String userId);
}
