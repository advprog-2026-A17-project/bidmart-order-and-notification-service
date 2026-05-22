package id.ac.ui.cs.advprog.bidmartordernotificationservice.repository;

import id.ac.ui.cs.advprog.bidmartordernotificationservice.model.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, String> {
}
