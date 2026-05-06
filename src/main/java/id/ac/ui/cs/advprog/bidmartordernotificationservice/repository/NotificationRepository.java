package id.ac.ui.cs.advprog.bidmartordernotificationservice.repository;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.BidmartNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<BidmartNotification, String> {

    List<BidmartNotification> findByUserIdOrderByCreatedAtDesc(String userId);

    Optional<BidmartNotification> findByUserIdAndTypeAndSourceEventId(
            String userId,
            String type,
            String sourceEventId
    );
}
